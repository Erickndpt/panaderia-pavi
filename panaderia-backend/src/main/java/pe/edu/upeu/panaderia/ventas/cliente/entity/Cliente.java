package pe.edu.upeu.panaderia.ventas.cliente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CLIENTES", schema = "PAN_VENTAS")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_DOCUMENTO", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    // NULL para TipoDocumento.SIN_DOCUMENTO: Oracle permite varios NULL en
    // la restricción de unicidad, de modo que los clientes sin documento
    // pueden coexistir.
    @Column(name = "NUMERO_DOCUMENTO", length = 20)
    private String numeroDocumento;

    @Column(name = "NOMBRE_RAZON_SOCIAL", nullable = false, length = 150)
    private String nombreRazonSocial;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "EMAIL", length = 120)
    private String email;

    @Column(name = "DIRECCION", length = 200)
    private String direccion;

    @Column(name = "PAIS", length = 80)
    private String pais;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;
}
