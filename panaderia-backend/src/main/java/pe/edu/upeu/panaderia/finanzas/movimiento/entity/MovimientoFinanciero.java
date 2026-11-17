package pe.edu.upeu.panaderia.finanzas.movimiento.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MOVIMIENTOS_FINANCIEROS", schema = "PAN_FINANZAS",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_MOVIMIENTO_ORIGEN",
                columnNames = {"ORIGEN", "ORIGEN_ID"}))
@Getter
@Setter
@NoArgsConstructor
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORIGEN", nullable = false, length = 20)
    private OrigenMovimiento origen;

    @Column(name = "ORIGEN_ID", nullable = false)
    private Long origenId;

    @Column(name = "CONCEPTO", nullable = false, length = 150)
    private String concepto;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoMovimiento estado;
}