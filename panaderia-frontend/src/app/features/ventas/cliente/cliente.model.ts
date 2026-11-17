/**
 * Contratos de Cliente. Reflejan exactamente ClienteRequest y ClienteResponse.
 * El cliente sí tiene `activo`. El tipo de documento condiciona el formato del número.
 */
export type TipoDocumento = 'DNI' | 'RUC' | 'CE' | 'PASAPORTE' | 'SIN_DOCUMENTO';

export interface Cliente {
  id: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string | null;
  nombreRazonSocial: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  pais: string | null;
  activo: boolean;
}

export interface ClienteRequest {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string | null;
  nombreRazonSocial: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  pais: string | null;
  activo: boolean;
}
export interface DocumentoConsulta {
  tipoDocumento: 'DNI' | 'RUC';
  numeroDocumento: string;
  nombreRazonSocial: string;
  direccion: string | null;
  pais: string | null;
  estado: string | null;
  condicion: string | null;
}
