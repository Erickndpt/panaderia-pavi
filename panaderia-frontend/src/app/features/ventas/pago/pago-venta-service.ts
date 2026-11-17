import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { PagoVentaRequest, PagoVentaResponse, PagoVentaResumenResponse } from './pago-venta.model';

/**
 * Endpoints reales de pagos de venta. Los pagos se gestionan fuera de
 * VentaResponse: registrar y listar viven bajo /ventas/{id}/pagos y obtener
 * y anular bajo /pagos-venta/{id}.
 */
@Injectable({ providedIn: 'root' })
export class PagoVentaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly ventasResource = '/api/v1/ventas';
  private readonly pagosResource = '/api/v1/pagos-venta';

  registrar(ventaId: number, request: PagoVentaRequest): Observable<PagoVentaResponse> {
    return this.http.post<PagoVentaResponse>(
      this.api.buildUrl(`${this.ventasResource}/${ventaId}/pagos`),
      request,
    );
  }

  /** Devuelve todos los pagos de la venta, incluidos los anulados. */
  listarPorVenta(ventaId: number): Observable<PagoVentaResponse[]> {
    return this.http.get<PagoVentaResponse[]>(
      this.api.buildUrl(`${this.ventasResource}/${ventaId}/pagos`),
    );
  }

  resumen(ventaId: number): Observable<PagoVentaResumenResponse> {
    return this.http.get<PagoVentaResumenResponse>(
      this.api.buildUrl(`${this.ventasResource}/${ventaId}/pagos/resumen`),
    );
  }

  obtener(pagoId: number): Observable<PagoVentaResponse> {
    return this.http.get<PagoVentaResponse>(this.api.buildUrl(`${this.pagosResource}/${pagoId}`));
  }

  /** El PATCH de anulación no envía cuerpo de negocio. */
  anular(pagoId: number): Observable<PagoVentaResponse> {
    return this.http.patch<PagoVentaResponse>(
      this.api.buildUrl(`${this.pagosResource}/${pagoId}/anular`),
      null,
    );
  }
}
