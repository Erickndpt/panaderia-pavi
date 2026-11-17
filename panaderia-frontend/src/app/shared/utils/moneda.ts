/** Formatea un monto como soles con dos decimales, p. ej. 172.5 -> "S/ 172.50". */
export function formatearMoneda(valor: number): string {
  const seguro = Number.isFinite(valor) ? valor : 0;
  return `S/ ${seguro.toFixed(2)}`;
}
