package pe.edu.upeu.panaderia.compras.proveedor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PROVEEDORES", schema = "PAN_COMPRAS")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUC", nullable = false, length = 11, unique = true)
    private String ruc;

    @Column(name = "RAZON_SOCIAL", nullable = false, length = 120)
    private String razonSocial;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "EMAIL", length = 120)
    private String email;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;
}