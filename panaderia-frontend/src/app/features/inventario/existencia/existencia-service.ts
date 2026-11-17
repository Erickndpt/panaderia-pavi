import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { ExistenciaRequest, ExistenciaResponse } from './existencia.model';

@Injectable({ providedIn: 'root' })
export class ExistenciaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/existencias';

  /**
   * El filtro productoId viaja como query param: el filtrado ocurre en el backend,
   * nunca descargando la lista completa para descartar filas en Angular.
   */
  listar(productoId?: number): Observable<ExistenciaResponse[]> {
    let params = new HttpParams();

    if (productoId !== undefined && productoId !== null) {
      params = params.set('productoId', productoId);
    }

    return this.http.get<ExistenciaResponse[]>(this.api.buildUrl(this.resource), { params });
  }

  obtener(id: number): Observable<ExistenciaResponse> {
    return this.http.get<ExistenciaResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  /** Consulta la existencia actual de un producto por su identificador. */
  obtenerPorProducto(productoId: number): Observable<ExistenciaResponse> {
    return this.http.get<ExistenciaResponse>(
      this.api.buildUrl(`${this.resource}/producto/${productoId}`),
    );
  }

  crear(existencia: ExistenciaRequest): Observable<ExistenciaResponse> {
    return this.http.post<ExistenciaResponse>(this.api.buildUrl(this.resource), existencia);
  }

  actualizar(id: number, existencia: ExistenciaRequest): Observable<ExistenciaResponse> {
    return this.http.put<ExistenciaResponse>(this.api.buildUrl(`${this.resource}/${id}`), existencia);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`${this.resource}/${id}`));
  }
}
