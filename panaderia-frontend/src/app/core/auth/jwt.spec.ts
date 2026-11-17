import { decodificarJwt, extraerRoles, nombreVisible } from './jwt';

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

describe('JWT', () => {
  it('decodifica el payload y sus claims principales', () => {
    const token = crearToken({
      sub: 'usuario-1',
      name: 'Ana Pérez',
      preferred_username: 'ana',
      realm_access: { roles: ['VENDEDOR', 'SUPERVISOR'] },
    });

    const payload = decodificarJwt(token);

    expect(payload?.sub).toBe('usuario-1');
    expect(payload?.name).toBe('Ana Pérez');
  });

  it('extrae los roles desde realm_access.roles', () => {
    const payload = decodificarJwt(
      crearToken({ realm_access: { roles: ['ADMIN', 'VENDEDOR', 7, null] } }),
    );

    expect(extraerRoles(payload)).toEqual(['ADMIN', 'VENDEDOR']);
  });

  it('devuelve lista vacía cuando no existen roles válidos', () => {
    expect(extraerRoles(decodificarJwt(crearToken({ sub: 'x' })))).toEqual([]);
    expect(extraerRoles(null)).toEqual([]);
  });

  it('prioriza name y luego preferred_username como nombre visible', () => {
    expect(nombreVisible({ name: 'Ana Pérez', preferred_username: 'ana' })).toBe('Ana Pérez');
    expect(nombreVisible({ preferred_username: 'ana' })).toBe('ana');
    expect(nombreVisible({ sub: 'usuario-1' })).toBeNull();
  });

  it('devuelve null para un token inválido', () => {
    expect(decodificarJwt(null)).toBeNull();
    expect(decodificarJwt('sin-puntos')).toBeNull();
    expect(decodificarJwt('a.b')).toBeNull();
  });
});
