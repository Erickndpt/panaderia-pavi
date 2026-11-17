import { calcularCodeChallenge, generarCodeVerifier, generarState } from './pkce';

const BASE64URL = /^[A-Za-z0-9_-]+$/;

describe('PKCE', () => {
  it('genera code_verifier aleatorio con formato Base64URL y longitud RFC 7636', () => {
    const primero = generarCodeVerifier();
    const segundo = generarCodeVerifier();

    expect(primero).not.toBe(segundo);
    expect(primero.length).toBeGreaterThanOrEqual(43);
    expect(primero.length).toBeLessThanOrEqual(128);
    expect(primero).toMatch(BASE64URL);
    expect(primero).not.toContain('=');
  });

  it('genera state aleatorio con formato Base64URL', () => {
    const primero = generarState();
    const segundo = generarState();

    expect(primero).not.toBe(segundo);
    expect(primero.length).toBeGreaterThanOrEqual(43);
    expect(primero).toMatch(BASE64URL);
  });

  it('calcula el code_challenge S256 con el vector de RFC 7636', async () => {
    await expect(
      calcularCodeChallenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'),
    ).resolves.toBe('E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM');
  });

  it('produce un challenge Base64URL de 43 caracteres sin relleno', async () => {
    const challenge = await calcularCodeChallenge(generarCodeVerifier());

    expect(challenge.length).toBe(43);
    expect(challenge).toMatch(BASE64URL);
    expect(challenge).not.toContain('=');
  });
});
