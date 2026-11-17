package pe.edu.upeu.panaderia.compras.proveedor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProveedorRequest {

    @NotBlank
    @Size(min = 11, max = 11)
    @Pattern(regexp = "\\d{11}", message = "El RUC debe contener exactamente 11 dígitos")
    private String ruc;

    @NotBlank
    @Size(max = 120)
    private String razonSocial;

    @Size(max = 20)
    private String telefono;

    @Email
    @Size(max = 120)
    private String email;

    @NotNull
    private Boolean activo;
}