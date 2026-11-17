package pe.edu.upeu.panaderia.compras.compra.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.compras.compra.entity.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Override
    @EntityGraph(attributePaths = {"detalles", "proveedor"})
    List<Compra> findAll();

    @Override
    @EntityGraph(attributePaths = {"detalles", "proveedor"})
    Optional<Compra> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"detalles", "proveedor"})
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> buscarConBloqueoPorId(@Param("id") Long id);
}