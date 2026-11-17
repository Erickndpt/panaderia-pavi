import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Proveedor, ProveedorRequest } from './proveedor.model';

@Injectable({ providedIn: 'root' })
export class ProveedorService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/proveedores';

  listar(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(this.api.buildUrl(this.resource));
  }

  obtener(id: number): Observable<Proveedor> {
    return this.http.get<Proveedor>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  crear(proveedor: ProveedorRequest): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.api.buildUrl(this.resource), proveedor);
  }

  actualizar(id: number, proveedor: ProveedorRequest): Observable<Proveedor> {
    return this.http.put<Proveedor>(this.api.buildUrl(`${this.resource}/${id}`), proveedor);
  }
}
