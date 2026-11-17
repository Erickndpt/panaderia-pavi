export interface Insumo {
  id: number;
  nombre: string;
  unidadMedida: string;
  stockActual: number;
  stockMinimo: number;
}

export type InsumoRequest = Omit<Insumo, 'id'>;
