import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Cliente, ClienteRequest, DocumentoConsulta } from './cliente.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/clientes';

  listar(buscar?: string): Observable<Cliente[]> {
    let params = new HttpParams();

    if (buscar !== undefined && buscar.trim().length > 0) {
      params = params.set('buscar', buscar.trim());
    }

    return this.http.get<Cliente[]>(this.api.buildUrl(this.resource), { params });
  }

  obtener(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(this.api.buildUrl(this.resource + '/' + id));
  }

  crear(cliente: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(this.api.buildUrl(this.resource), cliente);
  }

  actualizar(id: number, cliente: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(this.api.buildUrl(this.resource + '/' + id), cliente);
  }

  consultarDocumento(tipo: string, numero: string): Observable<DocumentoConsulta> {
    const params = new HttpParams().set('tipo', tipo).set('numero', numero.trim());
    return this.http.get<DocumentoConsulta>(this.api.buildUrl(this.resource + '/consulta-documento'), { params });
  }
}