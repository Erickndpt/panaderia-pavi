import { HttpErrorResponse } from '@angular/common/http';

interface ApiErrorBody {
  message?: unknown;
}

function cuerpoDe(error: unknown): ApiErrorBody | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  const cuerpo = error.error as ApiErrorBody | string | null;
  return cuerpo !== null && typeof cuerpo === 'object' ? cuerpo : null;
}

export function estadoHttp(error: unknown): number | null {
  return error instanceof HttpErrorResponse ? error.status : null;
}

export function esNoEncontrado(error: unknown): boolean {
  return estadoHttp(error) === 404;
}

export function esConflicto(error: unknown): boolean {
  return estadoHttp(error) === 409;
}

export function mensajeApi(error: unknown): string | null {
  const mensaje = cuerpoDe(error)?.message;
  return typeof mensaje === 'string' && mensaje.trim().length > 0 ? mensaje : null;
}