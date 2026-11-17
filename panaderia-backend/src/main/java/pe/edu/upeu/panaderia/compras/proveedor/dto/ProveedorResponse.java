package pe.edu.upeu.panaderia.compras.proveedor.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProveedorResponse {

    private Long id;
    private String ruc;
    private String razonSocial;
    private String telefono;
    private String email;
    private Boolean activo;
}