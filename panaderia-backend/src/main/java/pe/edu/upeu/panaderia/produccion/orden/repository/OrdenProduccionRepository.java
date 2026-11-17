package pe.edu.upeu.panaderia.produccion.orden.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.panaderia.produccion.orden.entity.OrdenProduccion;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {

    @Override
    @EntityGraph(attributePaths = "detalles")
    List<OrdenProduccion> findAll();

    @Override
    @EntityGraph(attributePaths = "detalles")
    Optional<OrdenProduccion> findById(Long id);
}