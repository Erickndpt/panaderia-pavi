package pe.edu.upeu.panaderia.ventas.pago.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.PagoVenta;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {

    List<PagoVenta> findByVentaIdOrderById(Long ventaId);

    // Solo los REGISTRADO entran en la suma del saldo; por eso el estado
    // viaja como parámetro en lugar de estar fijo en la consulta.
    @Query("select coalesce(sum(p.monto), 0) from PagoVenta p "
            + "where p.venta.id = :ventaId and p.estado = :estado")
    BigDecimal sumarMontosPorVentaYEstado(@Param("ventaId") Long ventaId, @Param("estado") EstadoPago estado);

    @Query("select count(p) > 0 from PagoVenta p where p.venta.id = :ventaId and p.estado = :estado")
    boolean existePagadoPorVentaYEstado(@Param("ventaId") Long ventaId, @Param("estado") EstadoPago estado);
}