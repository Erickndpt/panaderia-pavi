const BYTES_CODE_VERIFIER = 64;
const BYTES_STATE = 32;

function base64UrlDesdeBytes(bytes: Uint8Array): string {
  let binario = '';
  for (const byte of bytes) {
    binario += String.fromCharCode(byte);
  }

  return btoa(binario).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function bytesAleatorios(longitud: number): Uint8Array {
  const bytes = new Uint8Array(longitud);
  crypto.getRandomValues(bytes);
  return bytes;
}

/** Genera un code_verifier aleatorio apto para RFC 7636 (43-128 caracteres Base64URL). */
export function generarCodeVerifier(): string {
  return base64UrlDesdeBytes(bytesAleatorios(BYTES_CODE_VERIFIER));
}

/** Genera un valor state criptográficamente aleatorio. */
export function generarState(): string {
  return base64UrlDesdeBytes(bytesAleatorios(BYTES_STATE));
}

/** Calcula el code_challenge S256 (SHA-256 en Base64URL) del code_verifier. */
export async function calcularCodeChallenge(codeVerifier: string): Promise<string> {
  const datos = new TextEncoder().encode(codeVerifier);
  const digest = await crypto.subtle.digest('SHA-256', datos);
  return base64UrlDesdeBytes(new Uint8Array(digest));
}
