import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { OrdenProduccionRequest, OrdenProduccionResponse } from './orden-produccion.model';

@Injectable({ providedIn: 'root' })
export class OrdenProduccionService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/ordenes-produccion';

  registrar(orden: OrdenProduccionRequest): Observable<OrdenProduccionResponse> {
    return this.http.post<OrdenProduccionResponse>(this.api.buildUrl(this.resource), orden);
  }

  listar(): Observable<OrdenProduccionResponse[]> {
    return this.http.get<OrdenProduccionResponse[]>(this.api.buildUrl(this.resource));
  }

  obtener(id: number): Observable<OrdenProduccionResponse> {
    return this.http.get<OrdenProduccionResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }
}
