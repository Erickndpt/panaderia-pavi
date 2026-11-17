package pe.edu.upeu.panaderia.ventas.pago.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;

/**
 * Pago de una venta.
 *
 * <p>Sin cascada hacia {@link Venta}: la venta nunca se crea ni se borra
 * desde el pago. La relación es de lectura (para validar el saldo) y se
 * resuelve en la misma transacción con bloqueo pesimista.
 */
@Entity
@Table(name = "PAGOS_VENTAS", schema = "PAN_VENTAS")
@Getter
@Setter
@NoArgsConstructor
public class PagoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_VENTA", nullable = false)
    private Venta venta;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "METODO_PAGO", nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "REFERENCIA", length = 100)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoPago estado;
}