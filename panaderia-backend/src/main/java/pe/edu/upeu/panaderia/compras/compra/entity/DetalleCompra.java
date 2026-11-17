package pe.edu.upeu.panaderia.compras.compra.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "DETALLE_COMPRAS", schema = "PAN_COMPRAS")
@Getter
@Setter
@NoArgsConstructor
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPRA", nullable = false)
    private Compra compra;

    @Column(name = "ID_INSUMO", nullable = false)
    private Long insumoId;

    @Column(name = "NOMBRE_INSUMO", nullable = false, length = 80)
    private String nombreInsumo;

    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "CANTIDAD", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}