import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Categoria, CategoriaRequest } from './categoria.model';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/categorias';

  listar(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.api.buildUrl(this.resource));
  }

  obtener(id: number): Observable<Categoria> {
    return this.http.get<Categoria>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  crear(categoria: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.api.buildUrl(this.resource), categoria);
  }

  actualizar(id: number, categoria: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(this.api.buildUrl(`${this.resource}/${id}`), categoria);
  }
}
