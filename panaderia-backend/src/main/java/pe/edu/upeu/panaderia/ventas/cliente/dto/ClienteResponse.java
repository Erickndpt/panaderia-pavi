package pe.edu.upeu.panaderia.ventas.cliente.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;

@Getter
@Setter
@Builder
public class ClienteResponse {

    private Long id;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombreRazonSocial;
    private String telefono;
    private String email;
    private String direccion;
    private String pais;
    private Boolean activo;
}
