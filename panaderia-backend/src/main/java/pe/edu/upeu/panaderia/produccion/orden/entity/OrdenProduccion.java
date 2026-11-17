package pe.edu.upeu.panaderia.produccion.orden.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ORDENES_PRODUCCION", schema = "PAN_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class OrdenProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long productoId;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoOrdenProduccion estado;

    @Column(name = "CANTIDAD_PRODUCIDA", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadProducida;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleConsumo> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleConsumo detalle) {
        detalle.setOrden(this);
        this.detalles.add(detalle);
    }
}