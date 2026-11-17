package pe.edu.upeu.panaderia.inventario.insumo.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.inventario.insumo.entity.Insumo;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Optional<Insumo> findByNombreIgnoreCase(String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id")
    Optional<Insumo> buscarConBloqueoPorId(@Param("id") Long id);
}