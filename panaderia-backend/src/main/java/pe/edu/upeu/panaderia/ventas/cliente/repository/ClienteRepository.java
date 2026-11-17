package pe.edu.upeu.panaderia.ventas.cliente.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    // La búsqueda parcial ignora mayúsculas y minúsculas y cubre tanto el
    // nombre o razón social como el número de documento.
    @Query("""
            select c from Cliente c
            where lower(c.nombreRazonSocial) like lower(concat('%', :buscar, '%'))
               or lower(c.numeroDocumento) like lower(concat('%', :buscar, '%'))
            order by c.nombreRazonSocial
            """)
    List<Cliente> buscar(@Param("buscar") String buscar);
}
