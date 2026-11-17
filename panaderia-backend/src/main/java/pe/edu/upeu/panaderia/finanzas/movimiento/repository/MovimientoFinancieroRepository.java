package pe.edu.upeu.panaderia.finanzas.movimiento.repository;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.EstadoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.MovimientoFinanciero;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.OrigenMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.TipoMovimiento;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {

    Optional<MovimientoFinanciero> findByOrigenAndOrigenId(OrigenMovimiento origen, Long origenId);

    @Query("select count(m) from MovimientoFinanciero m where m.tipo = :tipo and m.estado = :estado")
    long contarPorTipoYEstado(@Param("tipo") TipoMovimiento tipo, @Param("estado") EstadoMovimiento estado);

    @Query("select coalesce(sum(m.monto), 0) from MovimientoFinanciero m "
            + "where m.tipo = :tipo and m.estado = :estado")
    BigDecimal sumarMontoPorTipoYEstado(@Param("tipo") TipoMovimiento tipo, @Param("estado") EstadoMovimiento estado);
}