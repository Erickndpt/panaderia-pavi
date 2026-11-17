import { HttpInterceptorFn } from '@angular/common/http';

export const traceIdInterceptor: HttpInterceptorFn = (req, next) => {
  const traceId = crypto.randomUUID();
  return next(req.clone({ setHeaders: { 'X-Trace-ID': traceId } }));
};
