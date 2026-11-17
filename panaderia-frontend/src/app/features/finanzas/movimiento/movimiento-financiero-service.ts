import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import {
  MovimientoFinancieroResponse,
  ResumenFinancieroResponse,
} from './movimiento-financiero.model';

/**
 * Módulo financiero de solo consulta: el backend crea los movimientos
 * automáticamente desde ventas y compras y no ofrece operaciones de escritura.
 */
@Injectable({ providedIn: 'root' })
export class MovimientoFinancieroService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/movimientos-financieros';

  listarMovimientos(): Observable<MovimientoFinancieroResponse[]> {
    return this.http.get<MovimientoFinancieroResponse[]>(this.api.buildUrl(this.resource));
  }

  obtenerMovimiento(id: number): Observable<MovimientoFinancieroResponse> {
    return this.http.get<MovimientoFinancieroResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  /** Resumen del backend: considera únicamente movimientos ACTIVO. */
  obtenerResumen(): Observable<ResumenFinancieroResponse> {
    return this.http.get<ResumenFinancieroResponse>(
      this.api.buildUrl('/api/v1/reportes/finanzas'),
    );
  }
}
