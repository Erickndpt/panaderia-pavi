import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Producto, ProductoFiltros, ProductoRequest } from './producto.model';

/**
 * Servicio HTTP del producto. Lo consume el CRUD de Administración y el módulo
 * Inventario, que lo usa como catálogo para poblar desplegables y mostrar el nombre.
 */
@Injectable({ providedIn: 'root' })
export class ProductoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly resource = '/api/v1/productos';

  /**
   * El filtrado (nombre, categoría, rango de precios) ocurre en el backend:
   * los filtros viajan como query params y nunca se descarga de más.
   */
  listar(filtros: ProductoFiltros = {}): Observable<Producto[]> {
    let params = new HttpParams();

    if (filtros.nombre !== undefined && filtros.nombre.trim().length > 0) {
      params = params.set('nombre', filtros.nombre.trim());
    }
    if (filtros.categoriaId !== undefined && filtros.categoriaId !== null) {
      params = params.set('categoriaId', filtros.categoriaId);
    }
    if (filtros.precioMin !== undefined && filtros.precioMin !== null) {
      params = params.set('precioMin', filtros.precioMin);
    }
    if (filtros.precioMax !== undefined && filtros.precioMax !== null) {
      params = params.set('precioMax', filtros.precioMax);
    }

    return this.http.get<Producto[]>(this.api.buildUrl(this.resource), { params });
  }

  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  crear(producto: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.api.buildUrl(this.resource), producto);
  }

  actualizar(id: number, producto: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(this.api.buildUrl(`${this.resource}/${id}`), producto);
  }
}
