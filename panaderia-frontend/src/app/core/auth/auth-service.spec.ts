import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth-service';
import { calcularCodeChallenge } from './pkce';

const CLAVE_SESION = 'pavi.auth.sesion';
const CLAVE_CODE_VERIFIER = 'pavi.auth.code_verifier';
const CLAVE_STATE = 'pavi.auth.state';
const CLAVE_DESTINO = 'pavi.auth.destino';

const TOKEN_URL = 'http://localhost:38080/realms/panaderia/protocol/openid-connect/token';

function base64Url(valor: unknown): string {
  const bytes = new TextEncoder().encode(JSON.stringify(valor));
  let binario = '';

  for (const byte of bytes) {
    binario += String.fromCharCode(byte);
  }

  return btoa(binario).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function crearToken(payload: Record<string, unknown>): string {
  return `${base64Url({ alg: 'none', typ: 'JWT' })}.${base64Url(payload)}.firma`;
}

function respuestaJson(datos: unknown, ok = true): Response {
  return {
    ok,
    status: ok ? 200 : 401,
    json: async () => datos,
  } as Response;
}

type ConRedireccion = { redirigir: (url: string) => void };

describe('AuthService', () => {
  const accessToken = crearToken({
    sub: 'usuario-1',
    name: 'Ana Pérez',
    preferred_username: 'ana',
    realm_access: { roles: ['VENDEDOR', 'SUPERVISOR'] },
  });
  const idToken = crearToken({
    name: 'Ana Pérez',
    preferred_username: 'ana',
    realm_access: { roles: ['VENDEDOR'] },
  });
  const refreshToken = 'refresh-1';

  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    sessionStorage.clear();
    window.localStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    sessionStorage.clear();
    window.localStorage.clear();
  });

  function redirigirEspia(service: AuthService) {
    const espia = vi.spyOn(service as unknown as ConRedireccion, 'redirigir');
    espia.mockImplementation(() => undefined);
    return espia;
  }

  it('inicia login con PKCE, state y destino sin usar localStorage', async () => {
    const service = TestBed.inject(AuthService);
    const redirigir = redirigirEspia(service);

    await service.iniciarLogin('/ventas/reporte');

    const codeVerifier = sessionStorage.getItem(CLAVE_CODE_VERIFIER);
    const state = sessionStorage.getItem(CLAVE_STATE);

    expect(codeVerifier).toBeTruthy();
    expect(state).toBeTruthy();
    expect(sessionStorage.getItem(CLAVE_DESTINO)).toBe('/ventas/reporte');
    expect(window.localStorage.length).toBe(0);
    expect(redirigir).toHaveBeenCalledTimes(1);

    const url = new URL(redirigir.mock.calls[0][0] as string);

    expect(`${url.origin}${url.pathname}`).toBe(
      'http://localhost:38080/realms/panaderia/protocol/openid-connect/auth',
    );
    expect(url.searchParams.get('client_id')).toBe('panaderia-frontend');
    expect(url.searchParams.get('redirect_uri')).toBe(
      'http://localhost:4200/auth/callback',
    );
    expect(url.searchParams.get('response_type')).toBe('code');
    expect(url.searchParams.get('scope')).toBe('openid profile email');
    expect(url.searchParams.get('state')).toBe(state);
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('code_challenge')).toBe(
      await calcularCodeChallenge(codeVerifier ?? ''),
    );
  });

  it('valida el state y canjea el code por tokens', async () => {
    sessionStorage.setItem(CLAVE_CODE_VERIFIER, 'verifier-1');
    sessionStorage.setItem(CLAVE_STATE, 'state-1');
    sessionStorage.setItem(CLAVE_DESTINO, '/ventas/reporte');
    fetchMock.mockResolvedValue(
      respuestaJson({
        access_token: accessToken,
        refresh_token: refreshToken,
        id_token: idToken,
      }),
    );

    const service = TestBed.inject(AuthService);
    await service.completarLogin('code-1', 'state-1');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const cuerpo = init.body as URLSearchParams;

    expect(url).toBe(TOKEN_URL);
    expect(init.method).toBe('POST');
    expect(cuerpo.get('grant_type')).toBe('authorization_code');
    expect(cuerpo.get('client_id')).toBe('panaderia-frontend');
    expect(cuerpo.get('redirect_uri')).toBe('http://localhost:4200/auth/callback');
    expect(cuerpo.get('code')).toBe('code-1');
    expect(cuerpo.get('code_verifier')).toBe('verifier-1');
    expect(cuerpo.get('client_secret')).toBeNull();

    expect(service.autenticado()).toBe(true);
    expect(service.usuario()).toBe('Ana Pérez');
    expect(service.roles()).toEqual(['VENDEDOR', 'SUPERVISOR']);
    expect(service.accessToken()).toBe(accessToken);
    expect(sessionStorage.getItem(CLAVE_CODE_VERIFIER)).toBeNull();
    expect(sessionStorage.getItem(CLAVE_STATE)).toBeNull();
    expect(JSON.parse(sessionStorage.getItem(CLAVE_SESION) ?? '{}')).toMatchObject({
      accessToken,
      refreshToken,
      idToken,
      usuario: 'Ana Pérez',
    });
    expect(service.destinoPendiente()).toBe('/ventas/reporte');
    expect(sessionStorage.getItem(CLAVE_DESTINO)).toBeNull();
  });

  it('rechaza el login si el state no coincide y no llama al token endpoint', async () => {
    sessionStorage.setItem(CLAVE_CODE_VERIFIER, 'verifier-1');
    sessionStorage.setItem(CLAVE_STATE, 'state-esperado');

    const service = TestBed.inject(AuthService);

    await expect(service.completarLogin('code-1', 'state-distinto')).rejects.toThrow(
      'no es válida',
    );

    expect(fetchMock).not.toHaveBeenCalled();
    expect(service.autenticado()).toBe(false);
    expect(sessionStorage.getItem(CLAVE_CODE_VERIFIER)).toBeNull();
    expect(sessionStorage.getItem(CLAVE_STATE)).toBeNull();
  });

  it('restaura la sesión desde sessionStorage después de un F5', () => {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        accessToken,
        refreshToken,
        idToken,
        usuario: 'Ana Pérez',
        roles: ['VENDEDOR'],
      }),
    );

    const service = TestBed.inject(AuthService);

    expect(service.autenticado()).toBe(true);
    expect(service.usuario()).toBe('Ana Pérez');
    expect(service.roles()).toEqual(['VENDEDOR']);
    expect(service.accessToken()).toBe(accessToken);
    expect(service.tieneRol('VENDEDOR')).toBe(true);
    expect(service.tieneRol('ADMIN')).toBe(false);
    expect(service.tieneRol('VENDEDOR', 'ADMIN')).toBe(true);
  });

  it('GERENTE_GENERAL satisface cualquier permiso funcional solicitado', () => {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        accessToken,
        refreshToken,
        idToken,
        usuario: 'Gerente General',
        roles: ['GERENTE_GENERAL'],
      }),
    );

    const service = TestBed.inject(AuthService);

    expect(service.tieneRol('ADMIN')).toBe(true);
    expect(service.tieneRol('SUPERVISOR')).toBe(true);
    expect(service.tieneRol('VENDEDOR')).toBe(true);
    expect(service.tieneRol()).toBe(false);
  });

  it('descarta una copia espejo inválida y continúa sin sesión', () => {
    sessionStorage.setItem(CLAVE_SESION, '{no-es-json');

    const service = TestBed.inject(AuthService);

    expect(service.autenticado()).toBe(false);
    expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('refresca el token y conserva la sesión actualizando roles', async () => {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        accessToken,
        refreshToken,
        idToken,
        usuario: 'Ana Pérez',
        roles: ['VENDEDOR'],
      }),
    );
    const nuevoAccessToken = crearToken({
      sub: 'usuario-1',
      preferred_username: 'ana',
      realm_access: { roles: ['ADMIN'] },
    });
    fetchMock.mockResolvedValue(respuestaJson({ access_token: nuevoAccessToken }));

    const service = TestBed.inject(AuthService);
    await expect(service.refrescar()).resolves.toBe(true);

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const cuerpo = init.body as URLSearchParams;

    expect(cuerpo.get('grant_type')).toBe('refresh_token');
    expect(cuerpo.get('refresh_token')).toBe(refreshToken);
    expect(service.accessToken()).toBe(nuevoAccessToken);
    expect(service.roles()).toEqual(['ADMIN']);
    expect(service.usuario()).toBe('Ana Pérez');
    expect(service.autenticado()).toBe(true);
    expect(JSON.parse(sessionStorage.getItem(CLAVE_SESION) ?? '{}')).toMatchObject({
      accessToken: nuevoAccessToken,
      refreshToken,
    });
  });

  it('limpia completamente la sesión cuando el refresh falla', async () => {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        accessToken,
        refreshToken,
        idToken,
        usuario: 'Ana Pérez',
        roles: ['VENDEDOR'],
      }),
    );
    sessionStorage.setItem(CLAVE_CODE_VERIFIER, 'verifier');
    sessionStorage.setItem(CLAVE_STATE, 'state');
    sessionStorage.setItem(CLAVE_DESTINO, '/ventas/reporte');
    fetchMock.mockResolvedValue(respuestaJson({ error: 'invalid_grant' }, false));

    const service = TestBed.inject(AuthService);
    await expect(service.refrescar()).resolves.toBe(false);

    expect(service.autenticado()).toBe(false);
    expect(service.accessToken()).toBeNull();
    expect(service.roles()).toEqual([]);
    expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    expect(sessionStorage.getItem(CLAVE_CODE_VERIFIER)).toBeNull();
    expect(sessionStorage.getItem(CLAVE_STATE)).toBeNull();
    expect(sessionStorage.getItem(CLAVE_DESTINO)).toBeNull();
  });

  it('limpia la sesión si no existe refresh token', async () => {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        accessToken,
        refreshToken: null,
        idToken: null,
        usuario: 'Ana Pérez',
        roles: ['VENDEDOR'],
      }),
    );

    const service = TestBed.inject(AuthService);

    await expect(service.refrescar()).resolves.toBe(false);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(service.autenticado()).toBe(false);
    expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('cierra sesión con RP-Initiated Logout e id_token_hint', () => {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        accessToken,
        refreshToken,
        idToken,
        usuario: 'Ana Pérez',
        roles: ['VENDEDOR'],
      }),
    );

    const service = TestBed.inject(AuthService);
    const redirigir = redirigirEspia(service);

    service.cerrarSesion();

    expect(service.autenticado()).toBe(false);
    expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    expect(redirigir).toHaveBeenCalledTimes(1);

    const url = new URL(redirigir.mock.calls[0][0] as string);

    expect(`${url.origin}${url.pathname}`).toBe(
      'http://localhost:38080/realms/panaderia/protocol/openid-connect/logout',
    );
    expect(url.searchParams.get('client_id')).toBe('panaderia-frontend');
    expect(url.searchParams.get('post_logout_redirect_uri')).toBe(
      'http://localhost:4200/',
    );
    expect(url.searchParams.get('id_token_hint')).toBe(idToken);
  });

  it('no escribe nada en localStorage durante login y callback', async () => {
    sessionStorage.setItem(CLAVE_CODE_VERIFIER, 'verifier-1');
    sessionStorage.setItem(CLAVE_STATE, 'state-1');
    fetchMock.mockResolvedValue(
      respuestaJson({
        access_token: accessToken,
        refresh_token: refreshToken,
        id_token: idToken,
      }),
    );
    const service = TestBed.inject(AuthService);
    await service.completarLogin('code-1', 'state-1');

    expect(window.localStorage.length).toBe(0);
  });

  it('descarta destinos externos o de callback', () => {
    sessionStorage.setItem(CLAVE_DESTINO, '//sitio-ajeno.example');
    const service = TestBed.inject(AuthService);

    expect(service.destinoPendiente()).toBe('/');
  });
});
