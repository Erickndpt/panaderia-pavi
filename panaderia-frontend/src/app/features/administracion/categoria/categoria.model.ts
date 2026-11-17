/**
 * Contratos de Categoría. Reflejan exactamente CategoriaRequest y CategoriaResponse
 * del backend. No existe el campo `activo` en el recurso real.
 */
export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
}

export interface CategoriaRequest {
  nombre: string;
  descripcion: string | null;
}
