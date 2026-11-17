package pe.edu.upeu.panaderia.ventas.cliente.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;

@Getter
@Setter
public class ClienteRequest {

    @NotNull
    private TipoDocumento tipoDocumento;

    @Size(max = 20)
    private String numeroDocumento;

    @NotBlank
    @Size(max = 150)
    private String nombreRazonSocial;

    @Size(max = 20)
    private String telefono;

    @Email
    @Size(max = 120)
    private String email;

    @Size(max = 200)
    private String direccion;

    @Size(max = 80)
    private String pais;

    @NotNull
    private Boolean activo;

    @AssertTrue(message = "El DNI debe contener exactamente 8 dígitos")
    public boolean isNumeroDocumentoDniValido() {
        return tipoDocumento != TipoDocumento.DNI || cumple("\\d{8}");
    }

    @AssertTrue(message = "El RUC debe contener exactamente 11 dígitos")
    public boolean isNumeroDocumentoRucValido() {
        return tipoDocumento != TipoDocumento.RUC || cumple("\\d{11}");
    }

    @AssertTrue(message = "El tipo CE y el tipo PASAPORTE exigen un número de documento de 3 a 20 caracteres")
    public boolean isNumeroDocumentoObligatorioValido() {
        boolean exigeDocumento = tipoDocumento == TipoDocumento.CE || tipoDocumento == TipoDocumento.PASAPORTE;
        if (!exigeDocumento) {
            return true;
        }
        return numeroDocumento != null && numeroDocumento.trim().length() >= 3
                && numeroDocumento.trim().length() <= 20;
    }

    @AssertTrue(message = "El tipo SIN_DOCUMENTO no admite número de documento")
    public boolean isNumeroDocumentoAusenteValido() {
        return tipoDocumento != TipoDocumento.SIN_DOCUMENTO
                || numeroDocumento == null || numeroDocumento.isBlank();
    }

    private boolean cumple(String regex) {
        return numeroDocumento != null && numeroDocumento.trim().matches(regex);
    }
}
