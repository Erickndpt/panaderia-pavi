package pe.edu.upeu.panaderia.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaBadRequestException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaNotFoundException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaRateLimitedException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaServiceUnavailableException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaUnauthorizedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccesoDenegadoException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({
            FiltroInvalidoException.class,
            CompraInvalidaException.class,
            MovimientoInvalidoException.class,
            OrdenProduccionInvalidaException.class,
            PagoInvalidoException.class,
            VentaInvalidaException.class,
            VentaAnuladaNoAceptaPagosException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({
            ClienteConVentasException.class,
            ClienteDocumentoDuplicadoException.class,
            ClienteInactivoException.class,
            CompraYaAnuladaException.class,
            ExistenciaConStockException.class,
            ExistenciaDuplicadaException.class,
            InsumoDuplicadoException.class,
            InsumoStockInsuficienteException.class,
            MovimientoDuplicadoException.class,
            PagoExcedeSaldoException.class,
            PagoYaAnuladoException.class,
            ProveedorDuplicadoException.class,
            ProveedorInactivoException.class,
            StockInsuficienteException.class,
            VentaConPagosRegistradosException.class,
            VentaYaAnuladaException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DocumentoConsultaBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleDocumentoConsultaBadRequest(
            DocumentoConsultaBadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DocumentoConsultaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentoConsultaNotFound(
            DocumentoConsultaNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DocumentoConsultaUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleDocumentoConsultaUnauthorized(
            DocumentoConsultaUnauthorizedException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(DocumentoConsultaRateLimitedException.class)
    public ResponseEntity<ErrorResponse> handleDocumentoConsultaRateLimited(
            DocumentoConsultaRateLimitedException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(DocumentoConsultaServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDocumentoConsultaServiceUnavailable(
            DocumentoConsultaServiceUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT,
                "No se pudo completar la operación: el registro viola una restricción de datos.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }

    public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message) {
    }
}
