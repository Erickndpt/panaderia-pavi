import { computed, Injectable, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { decodificarJwt, extraerRoles, JwtPayload, nombreVisible } from './jwt';
import { calcularCodeChallenge, generarCodeVerifier, generarState } from './pkce';

const CLAVE_SESION = 'pavi.auth.sesion';
const CLAVE_CODE_VERIFIER = 'pavi.auth.code_verifier';
const CLAVE_STATE = 'pavi.auth.state';
const CLAVE_DESTINO = 'pavi.auth.destino';
const ROL_ACCESO_TOTAL = 'GERENTE_GENERAL';

interface SesionAuth {
  accessToken: string;
  refreshToken: string | null;
  idToken: string | null;
  usuario: string;
  roles: string[];
}

interface RespuestaToken {
  access_token: string;
  refresh_token?: string;
  id_token?: string;
}

function esSesion(valor: unknown): valor is SesionAuth {
  if (typeof valor !== 'object' || valor === null) {
    return false;
  }

  const sesion = valor as Partial<Record<keyof SesionAuth, unknown>>;

  return (
    typeof sesion.accessToken === 'string' &&
    sesion.accessToken.length > 0 &&
    (sesion.refreshToken === null || typeof sesion.refreshToken === 'string') &&
    (sesion.idToken === null || typeof sesion.idToken === 'string') &&
    typeof sesion.usuario === 'string' &&
    Array.isArray(sesion.roles) &&
    sesion.roles.every((rol) => typeof rol === 'string')
  );
}

function esRespuestaToken(valor: unknown): valor is RespuestaToken {
  if (typeof valor !== 'object' || valor === null) {
    return false;
  }

  const respuesta = valor as Record<string, unknown>;

  return (
    typeof respuesta['access_token'] === 'string' &&
    (respuesta['refresh_token'] === undefined || typeof respuesta['refresh_token'] === 'string') &&
    (respuesta['id_token'] === undefined || typeof respuesta['id_token'] === 'string')
  );
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sesionInterna = signal<SesionAuth | null>(this.restaurarSesion());
  private refrescoEnCurso: Promise<boolean> | null = null;

  readonly autenticado = computed(() => this.sesionInterna() !== null);
  readonly usuario = computed(() => this.sesionInterna()?.usuario ?? null);
  readonly roles = computed<readonly string[]>(() => this.sesionInterna()?.roles ?? []);

  accessToken(): string | null {
    return this.sesionInterna()?.accessToken ?? null;
  }

  tieneRol(...roles: string[]): boolean {
    if (roles.length === 0) {
      return false;
    }

    const actuales = this.roles();
    if (actuales.includes(ROL_ACCESO_TOTAL)) {
      return true;
    }
    return roles.some((rol) => actuales.includes(rol));
  }

  /** Inicia Authorization Code + PKCE y redirige al endpoint de autorización de Keycloak. */
  async iniciarLogin(destino?: string): Promise<void> {
    const codeVerifier = generarCodeVerifier();
    const state = generarState();
    const codeChallenge = await calcularCodeChallenge(codeVerifier);
    const almacenamiento = this.storage();

    almacenamiento?.setItem(CLAVE_CODE_VERIFIER, codeVerifier);
    almacenamiento?.setItem(CLAVE_STATE, state);
    almacenamiento?.setItem(
      CLAVE_DESTINO,
      this.normalizarDestino(destino ?? this.rutaActual()),
    );

    const parametros = new URLSearchParams({
      client_id: environment.keycloak.clientId,
      redirect_uri: environment.keycloak.redirectUri,
      response_type: 'code',
      scope: 'openid profile email',
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    this.redirigir(`${environment.keycloak.issuer}/protocol/openid-connect/auth?${parametros}`);
  }

  /** Valida el state y canjea el code por tokens. */
  async completarLogin(code: string, state: string): Promise<void> {
    const almacenamiento = this.storage();
    const codeVerifier = almacenamiento?.getItem(CLAVE_CODE_VERIFIER) ?? null;
    const stateEsperado = almacenamiento?.getItem(CLAVE_STATE) ?? null;

    almacenamiento?.removeItem(CLAVE_CODE_VERIFIER);
    almacenamiento?.removeItem(CLAVE_STATE);

    if (code.length === 0 || !codeVerifier || !stateEsperado || stateEsperado !== state) {
      throw new Error('La solicitud de autenticación no es válida o expiró.');
    }

    const respuesta = await this.solicitarToken(
      new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: environment.keycloak.clientId,
        redirect_uri: environment.keycloak.redirectUri,
        code,
        code_verifier: codeVerifier,
      }),
    );

    this.guardarSesion(this.crearSesion(respuesta));
  }

  /** Refresca el access token. Ante cualquier fallo limpia completamente la sesión. */
  async refrescar(): Promise<boolean> {
    if (this.refrescoEnCurso) {
      return this.refrescoEnCurso;
    }

    this.refrescoEnCurso = this.ejecutarRefresco();

    try {
      return await this.refrescoEnCurso;
    } finally {
      this.refrescoEnCurso = null;
    }
  }

  /** Implementa RP-Initiated Logout y limpia la sesión local. */
  cerrarSesion(): void {
    const idToken = this.sesionInterna()?.idToken ?? null;
    this.limpiarSesion();

    const parametros = new URLSearchParams({
      client_id: environment.keycloak.clientId,
      post_logout_redirect_uri: environment.keycloak.postLogoutRedirectUri,
    });

    if (idToken) {
      parametros.set('id_token_hint', idToken);
    }

    this.redirigir(`${environment.keycloak.issuer}/protocol/openid-connect/logout?${parametros}`);
  }

  /** Devuelve y consume la ruta interna a la que se quería acceder antes del login. */
  destinoPendiente(): string {
    const almacenamiento = this.storage();
    const destino = almacenamiento?.getItem(CLAVE_DESTINO) ?? null;

    almacenamiento?.removeItem(CLAVE_DESTINO);

    return destino === null ? '/' : this.normalizarDestino(destino);
  }

  private async ejecutarRefresco(): Promise<boolean> {
    const actual = this.sesionInterna();

    if (!actual?.refreshToken) {
      this.limpiarSesion();
      return false;
    }

    try {
      const respuesta = await this.solicitarToken(
        new URLSearchParams({
          grant_type: 'refresh_token',
          client_id: environment.keycloak.clientId,
          refresh_token: actual.refreshToken,
        }),
      );

      this.guardarSesion(this.crearSesion(respuesta, actual));
      return true;
    } catch {
      this.limpiarSesion();
      return false;
    }
  }

  private async solicitarToken(cuerpo: URLSearchParams): Promise<RespuestaToken> {
    const respuesta = await fetch(
      `${environment.keycloak.issuer}/protocol/openid-connect/token`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: cuerpo,
      },
    );

    if (!respuesta.ok) {
      throw new Error('No se pudo completar la operación de autenticación.');
    }

    const datos = (await respuesta.json()) as unknown;

    if (!esRespuestaToken(datos)) {
      throw new Error('La respuesta del servidor de autenticación no es válida.');
    }

    return datos;
  }

  private crearSesion(respuesta: RespuestaToken, anterior?: SesionAuth): SesionAuth {
    const payloadAcceso = decodificarJwt(respuesta.access_token);
    const idToken = respuesta.id_token ?? anterior?.idToken ?? null;
    const payloadId = decodificarJwt(idToken);
    const usuario =
      nombreVisible(payloadId) ??
      nombreVisible(payloadAcceso) ??
      this.subComoUsuario(payloadAcceso);

    return {
      accessToken: respuesta.access_token,
      refreshToken: respuesta.refresh_token ?? anterior?.refreshToken ?? null,
      idToken,
      usuario,
      roles: extraerRoles(payloadAcceso),
    };
  }

  private subComoUsuario(payload: JwtPayload | null): string {
    return payload?.sub && payload.sub.length > 0 ? payload.sub : 'Usuario';
  }

  private guardarSesion(sesion: SesionAuth): void {
    this.sesionInterna.set(sesion);
    this.storage()?.setItem(CLAVE_SESION, JSON.stringify(sesion));
  }

  private limpiarSesion(): void {
    this.sesionInterna.set(null);

    const almacenamiento = this.storage();
    almacenamiento?.removeItem(CLAVE_SESION);
    almacenamiento?.removeItem(CLAVE_CODE_VERIFIER);
    almacenamiento?.removeItem(CLAVE_STATE);
    almacenamiento?.removeItem(CLAVE_DESTINO);
  }

  private restaurarSesion(): SesionAuth | null {
    const almacenamiento = this.storage();
    const guardada = almacenamiento?.getItem(CLAVE_SESION);

    if (!guardada) {
      return null;
    }

    try {
      const sesion = JSON.parse(guardada) as unknown;

      if (esSesion(sesion)) {
        return sesion;
      }
    } catch {
      // La copia espejo es inválida: se descarta para iniciar sin sesión.
    }

    almacenamiento?.removeItem(CLAVE_SESION);
    return null;
  }

  private normalizarDestino(destino: string): string {
    if (
      !destino.startsWith('/') ||
      destino.startsWith('//') ||
      destino.startsWith('/auth/callback')
    ) {
      return '/';
    }

    return destino;
  }

  private rutaActual(): string {
    const { pathname, search, hash } = window.location;
    return `${pathname}${search}${hash}`;
  }

  private storage(): Storage | null {
    return typeof sessionStorage === 'undefined' ? null : sessionStorage;
  }

  private redirigir(url: string): void {
    window.location.assign(url);
  }
}
