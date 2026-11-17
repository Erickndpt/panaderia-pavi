import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import {
  ReporteVentaResponse,
  VentaFiltros,
  VentaRequest,
  VentaResponse,
} from './venta.model';

/** Convierte una fecha YYYY-MM-DD al inicio del día ISO-8601; respeta un valor ya completo. */
function normalizarDesde(fecha: string): string {
  return fecha.includes('T') ? fecha : `${fecha}T00:00:00`;
}

/** Convierte una fecha YYYY-MM-DD al fin del día ISO-8601; respeta un valor ya completo. */
function normalizarHasta(fecha: string): string {
  return fecha.includes('T') ? fecha : `${fecha}T23:59:59.999`;
}

function noVacio(valor: string | undefined): valor is string {
  return valor !== undefined && valor.trim().length > 0;
}

function construirParams(filtros: VentaFiltros): HttpParams {
  let params = new HttpParams();

  if (filtros.tipo !== undefined) {
    params = params.set('tipo', filtros.tipo);
  }
  if (filtros.estado !== undefined) {
    params = params.set('estado', filtros.estado);
  }
  if (noVacio(filtros.fechaDesde)) {
    params = params.set('fechaDesde', normalizarDesde(filtros.fechaDesde));
  }
  if (noVacio(filtros.fechaHasta)) {
    params = params.set('fechaHasta', normalizarHasta(filtros.fechaHasta));
  }
  if (noVacio(filtros.paisDestino)) {
    params = params.set('paisDestino', filtros.paisDestino.trim());
  }
  if (filtros.productoId !== undefined && filtros.productoId !== null) {
    params = params.set('productoId', filtros.productoId);
  }
  if (filtros.clienteId !== undefined && filtros.clienteId !== null) {
    params = params.set('clienteId', filtros.clienteId);
  }
  if (noVacio(filtros.ordenFecha)) {
    params = params.set('ordenFecha', filtros.ordenFecha);
  }

  return params;
}

@Injectable({ providedIn: 'root' })
export class VentaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/ventas';

  registrar(venta: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(this.api.buildUrl(this.resource), venta);
  }

  listar(filtros: VentaFiltros = {}): Observable<VentaResponse[]> {
    return this.http.get<VentaResponse[]>(this.api.buildUrl(this.resource), {
      params: construirParams(filtros),
    });
  }

  obtener(id: number): Observable<VentaResponse> {
    return this.http.get<VentaResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  resumen(fechaDesde?: string, fechaHasta?: string): Observable<ReporteVentaResponse> {
    return this.http.get<ReporteVentaResponse>(this.api.buildUrl(`${this.resource}/resumen`), {
      params: construirParams({ fechaDesde, fechaHasta }),
    });
  }

  /** El PATCH de anulación no envía cuerpo de negocio. */
  anular(id: number): Observable<VentaResponse> {
    return this.http.patch<VentaResponse>(this.api.buildUrl(`${this.resource}/${id}/anular`), null);
  }
}
