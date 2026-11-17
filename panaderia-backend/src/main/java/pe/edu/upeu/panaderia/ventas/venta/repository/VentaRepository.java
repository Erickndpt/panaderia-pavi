package pe.edu.upeu.panaderia.ventas.venta.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Override
    @EntityGraph(attributePaths = {"detalles", "cliente"})
    List<Venta> findAll();

    @Override
    @EntityGraph(attributePaths = {"detalles", "cliente"})
    List<Venta> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"detalles", "cliente"})
    Optional<Venta> findById(Long id);

    @EntityGraph(attributePaths = {"detalles", "cliente"})
    List<Venta> findAll(Specification<Venta> spec);

    @EntityGraph(attributePaths = {"detalles", "cliente"})
    List<Venta> findAll(Specification<Venta> spec, Sort sort);

    long countByClienteId(Long clienteId);

    // La consulta bloqueante conserva su EntityGraph original: el bloqueo
    // pesimista se aplica sobre VENTAS y el cliente se carga por su estado
    // LAZY dentro de la misma transacción de la anulación.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "detalles")
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> buscarConBloqueoPorId(@Param("id") Long id);
}
