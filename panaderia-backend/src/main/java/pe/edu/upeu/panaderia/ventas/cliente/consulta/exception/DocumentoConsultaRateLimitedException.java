package pe.edu.upeu.panaderia.ventas.cliente.consulta.exception;

public class DocumentoConsultaRateLimitedException extends DocumentoConsultaException {
    public DocumentoConsultaRateLimitedException(String message) {
        super(message);
    }
}
