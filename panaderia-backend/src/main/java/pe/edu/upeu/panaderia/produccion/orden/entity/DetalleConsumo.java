package pe.edu.upeu.panaderia.produccion.orden.entity;

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
@Table(name = "DETALLES_CONSUMO", schema = "PAN_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class DetalleConsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ORDEN", nullable = false)
    private OrdenProduccion orden;

    @Column(name = "ID_INSUMO", nullable = false)
    private Long insumoId;

    @Column(name = "NOMBRE_INSUMO", nullable = false, length = 80)
    private String nombreInsumo;

    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "CANTIDAD_CONSUMIDA", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadConsumida;
}