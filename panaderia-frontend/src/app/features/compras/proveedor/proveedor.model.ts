/**
 * Contratos de Proveedor. Reflejan exactamente ProveedorRequest y ProveedorResponse.
 * El proveedor sí tiene `activo`, gestionado por el backend vía PUT.
 */
export interface Proveedor {
  id: number;
  ruc: string;
  razonSocial: string;
  telefono: string | null;
  email: string | null;
  activo: boolean;
}

export interface ProveedorRequest {
  ruc: string;
  razonSocial: string;
  telefono: string | null;
  email: string | null;
  activo: boolean;
}
