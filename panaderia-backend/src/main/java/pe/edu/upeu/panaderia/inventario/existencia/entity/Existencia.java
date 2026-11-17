package pe.edu.upeu.panaderia.inventario.existencia.entity;

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
@Table(name = "EXISTENCIAS", schema = "PAN_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class Existencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_PRODUCTO", nullable = false, unique = true)
    private Long idProducto;

    @Column(name = "CANTIDAD", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;
}