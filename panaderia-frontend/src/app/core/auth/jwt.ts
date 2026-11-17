export interface JwtPayload {
  sub?: string;
  name?: string;
  preferred_username?: string;
  exp?: number;
  realm_access?: {
    roles?: unknown;
  };
  [claim: string]: unknown;
}

function base64UrlADatos(valor: string): Uint8Array {
  const normalizado = valor.replace(/-/g, '+').replace(/_/g, '/');
  const relleno = normalizado.length % 4 === 0 ? '' : '='.repeat(4 - (normalizado.length % 4));
  const binario = atob(normalizado + relleno);
  const bytes = new Uint8Array(binario.length);

  for (let indice = 0; indice < binario.length; indice += 1) {
    bytes[indice] = binario.charCodeAt(indice);
  }

  return bytes;
}

/** Decodifica el payload de un JWT sin validar la firma. */
export function decodificarJwt(token: string | null | undefined): JwtPayload | null {
  if (!token) {
    return null;
  }

  const partes = token.split('.');
  if (partes.length < 2 || partes[1].length === 0) {
    return null;
  }

  try {
    const payload = JSON.parse(
      new TextDecoder().decode(base64UrlADatos(partes[1])),
    ) as unknown;

    if (typeof payload !== 'object' || payload === null || Array.isArray(payload)) {
      return null;
    }

    return payload as JwtPayload;
  } catch {
    return null;
  }
}

/** Extrae los roles de realm_access.roles, ignorando valores que no sean texto. */
export function extraerRoles(payload: JwtPayload | null): string[] {
  const roles = payload?.realm_access?.roles;
  if (!Array.isArray(roles)) {
    return [];
  }

  return roles.filter((rol): rol is string => typeof rol === 'string');
}

/** Devuelve el nombre visible priorizando name y luego preferred_username. */
export function nombreVisible(payload: JwtPayload | null): string | null {
  if (!payload) {
    return null;
  }

  const nombre = payload.name;
  if (typeof nombre === 'string' && nombre.trim().length > 0) {
    return nombre;
  }

  const usuario = payload.preferred_username;
  if (typeof usuario === 'string' && usuario.trim().length > 0) {
    return usuario;
  }

  return null;
}
