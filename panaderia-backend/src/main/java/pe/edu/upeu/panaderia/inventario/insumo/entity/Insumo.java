package pe.edu.upeu.panaderia.inventario.insumo.entity;

import java.math.BigDecimal;
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
@Table(name = "INSUMOS", schema = "PAN_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "STOCK_ACTUAL", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockActual;

    @Column(name = "STOCK_MINIMO", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo;
}