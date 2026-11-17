package pe.edu.upeu.panaderia.inventario.existencia.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.inventario.existencia.entity.Existencia;

public interface ExistenciaRepository extends JpaRepository<Existencia, Long> {

    Optional<Existencia> findByIdProducto(Long idProducto);

    /**
     * Filtro del listado por producto. La restricción es UNIQUE en Oracle, por lo que
     * devuelve como máximo un elemento, pero se declara como lista para permitir el
     * filtro opcional productoId.
     */
    List<Existencia> findAllByIdProductoOrderByIdAsc(Long idProducto);

    boolean existsByIdProducto(Long idProducto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Existencia e where e.idProducto = :idProducto")
    Optional<Existencia> buscarConBloqueoPorProducto(@Param("idProducto") Long idProducto);
}
