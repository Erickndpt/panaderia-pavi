/** Normaliza un texto para búsquedas: sin espacios extremos, minúsculas y sin tildes. */
export function normalizarTexto(valor: string): string {
  return valor
    .trim()
    .toLocaleLowerCase('es')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}
