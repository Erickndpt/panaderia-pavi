package pe.edu.upeu.panaderia.ventas.cliente.dto;

import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;

public record ClienteResumen(
        Long id,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombreRazonSocial) {
}
