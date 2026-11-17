import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Insumo, InsumoRequest } from './insumo.model';

@Injectable({ providedIn: 'root' })
export class InsumoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/insumos';

  listar(): Observable<Insumo[]> {
    return this.http.get<Insumo[]>(this.api.buildUrl(this.resource));
  }

  obtener(id: number): Observable<Insumo> {
    return this.http.get<Insumo>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  crear(insumo: InsumoRequest): Observable<Insumo> {
    return this.http.post<Insumo>(this.api.buildUrl(this.resource), insumo);
  }

  actualizar(id: number, insumo: InsumoRequest): Observable<Insumo> {
    return this.http.put<Insumo>(this.api.buildUrl(`${this.resource}/${id}`), insumo);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`${this.resource}/${id}`));
  }
}
