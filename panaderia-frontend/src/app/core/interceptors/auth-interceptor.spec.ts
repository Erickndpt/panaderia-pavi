import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { AuthService } from '../auth/auth-service';
import { authInterceptor } from './auth-interceptor';

const API = 'http://localhost:8081';
const URL_VENTAS = `${API}/api/v1/ventas`;

function esperarMicrotarea(): Promise<void> {
  return new Promise((resolver) => setTimeout(resolver, 0));
}

function capturar<T>(
  origen: Observable<T>,
): Promise<{ ok: true; valor: T } | { ok: false; error: unknown }> {
  return firstValueFrom(origen).then(
    (valor) => ({ ok: true as const, valor }),
    (error: unknown) => ({ ok: false as const, error }),
  );
}

describe('authInterceptor', () => {
  let control: HttpTestingController;

  function configurar(tokenInicial: string | null = 'token-inicial') {
    let token: string | null = tokenInicial;

    const auth = {
      accessToken: () => token,
      refrescar: vi.fn<() => Promise<boolean>>(),
      iniciarLogin: vi.fn<() => Promise<void>>(),
    };

    auth.refrescar.mockResolvedValue(false);
    auth.iniciarLogin.mockResolvedValue(undefined);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    control = TestBed.inject(HttpTestingController);

    return {
      http: TestBed.inject(HttpClient),
      auth,
      cambiarToken: (nuevo: string | null) => {
        token = nuevo;
      },
    };
  }

  afterEach(() => {
    control.verify();
  });

  it('adjunta Bearer a las solicitudes dirigidas al backend', async () => {
    const { http } = configurar('token-1');

    const respuesta = firstValueFrom(http.get(URL_VENTAS));
    const peticion = control.expectOne(URL_VENTAS);

    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-1');

    peticion.flush([]);
    await respuesta;
  });

  it('no adjunta el token a URLs ajenas al backend', async () => {
    const { http } = configurar('token-1');

    const respuesta = firstValueFrom(http.get('https://otro-servicio.example/datos'));
    const peticion = control.expectOne('https://otro-servicio.example/datos');

    expect(peticion.request.headers.has('Authorization')).toBe(false);

    peticion.flush({});
    await respuesta;
  });

  it('no adjunta el token a hosts que solo comparten el prefijo del backend', async () => {
    const { http } = configurar('token-1');

    const url = 'http://localhost:8081.evil.example/datos';
    const respuesta = firstValueFrom(http.get(url));
    const peticion = control.expectOne(url);

    expect(peticion.request.headers.has('Authorization')).toBe(false);

    peticion.flush({});
    await respuesta;
  });

  it('ante un 401 refresca y reintenta la petición una sola vez', async () => {
    const { http, auth, cambiarToken } = configurar('token-viejo');
    auth.refrescar.mockImplementation(async () => {
      cambiarToken('token-nuevo');
      return true;
    });

    const respuesta = firstValueFrom(http.get(URL_VENTAS));
    const original = control.expectOne(URL_VENTAS);

    expect(original.request.headers.get('Authorization')).toBe('Bearer token-viejo');
    original.flush({}, { status: 401, statusText: 'Unauthorized' });
    await esperarMicrotarea();

    const reintento = control.expectOne(URL_VENTAS);

    expect(reintento.request.headers.get('Authorization')).toBe('Bearer token-nuevo');
    reintento.flush({ ok: true });

    await expect(respuesta).resolves.toEqual({ ok: true });
    expect(auth.refrescar).toHaveBeenCalledTimes(1);
  });

  it('no entra en bucle si el reintento vuelve a responder 401', async () => {
    const { http, auth, cambiarToken } = configurar('token-viejo');
    auth.refrescar.mockImplementation(async () => {
      cambiarToken('token-nuevo');
      return true;
    });

    const respuesta = capturar(http.get(URL_VENTAS));

    control
      .expectOne(URL_VENTAS)
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await esperarMicrotarea();

    control
      .expectOne(URL_VENTAS)
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await esperarMicrotarea();

    const resultado = await respuesta;
    expect(resultado.ok).toBe(false);
    if (!resultado.ok) {
      expect(resultado.error).toMatchObject({ status: 401 });
    }
    expect(auth.refrescar).toHaveBeenCalledTimes(1);
    control.expectNone(URL_VENTAS);
  });

  it('propaga el 403 sin refrescar ni iniciar login', async () => {
    const { http, auth } = configurar();

    const respuesta = capturar(http.get(URL_VENTAS));

    control
      .expectOne(URL_VENTAS)
      .flush({}, { status: 403, statusText: 'Forbidden' });
    await esperarMicrotarea();

    const resultado = await respuesta;
    expect(resultado.ok).toBe(false);
    if (!resultado.ok) {
      expect(resultado.error).toMatchObject({ status: 403 });
    }
    expect(auth.refrescar).not.toHaveBeenCalled();
    expect(auth.iniciarLogin).not.toHaveBeenCalled();
    control.expectNone(URL_VENTAS);
  });

  it('si no puede refrescar inicia login y no reintenta', async () => {
    const { http, auth } = configurar();
    auth.refrescar.mockResolvedValue(false);

    const respuesta = capturar(http.get(URL_VENTAS));

    control
      .expectOne(URL_VENTAS)
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await esperarMicrotarea();

    const resultado = await respuesta;
    expect(resultado.ok).toBe(false);
    if (!resultado.ok) {
      expect(resultado.error).toMatchObject({ status: 401 });
    }
    expect(auth.refrescar).toHaveBeenCalledTimes(1);
    expect(auth.iniciarLogin).toHaveBeenCalledTimes(1);
    control.expectNone(URL_VENTAS);
  });
});
