package pe.edu.upeu.panaderia.administracion.producto.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.edu.upeu.panaderia.administracion.producto.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    @Override
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll();

    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll(Specification<Producto> spec);
}