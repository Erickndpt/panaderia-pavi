import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { CompraRequest, CompraResponse } from './compra.model';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/compras';

  registrar(compra: CompraRequest): Observable<CompraResponse> {
    return this.http.post<CompraResponse>(this.api.buildUrl(this.resource), compra);
  }

  listar(): Observable<CompraResponse[]> {
    return this.http.get<CompraResponse[]>(this.api.buildUrl(this.resource));
  }

  obtener(id: number): Observable<CompraResponse> {
    return this.http.get<CompraResponse>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  anular(id: number): Observable<CompraResponse> {
    return this.http.patch<CompraResponse>(this.api.buildUrl(`${this.resource}/${id}/anular`), null);
  }
}
