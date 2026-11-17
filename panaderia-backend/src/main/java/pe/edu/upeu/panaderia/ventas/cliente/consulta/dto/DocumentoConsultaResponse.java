package pe.edu.upeu.panaderia.ventas.cliente.consulta.dto;

public record DocumentoConsultaResponse(
        String tipoDocumento,
        String numeroDocumento,
        String nombreRazonSocial,
        String direccion,
        String pais,
        String estado,
        String condicion
) {
}
