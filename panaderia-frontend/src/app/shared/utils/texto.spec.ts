import { normalizarTexto } from './texto';

describe('normalizarTexto', () => {
  it('debe quitar espacios extremos, tildes y mayúsculas', () => {
    expect(normalizarTexto('  Azúcar Rubia ')).toBe('azucar rubia');
  });

  it('debe devolver cadena vacía al normalizar espacios', () => {
    expect(normalizarTexto('   ')).toBe('');
  });
});
