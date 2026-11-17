import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  buildUrl(path: string): string {
    const ruta = path.startsWith('/') ? path : `/${path}`;
    return `${this.baseUrl}${ruta}`;
  }
}
