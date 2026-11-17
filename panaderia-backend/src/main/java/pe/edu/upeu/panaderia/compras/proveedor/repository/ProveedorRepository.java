package pe.edu.upeu.panaderia.compras.proveedor.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByRuc(String ruc);
}