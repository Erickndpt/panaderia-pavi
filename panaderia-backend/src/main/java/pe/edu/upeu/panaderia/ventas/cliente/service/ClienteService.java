package pe.edu.upeu.panaderia.ventas.cliente.service;

import java.util.List;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteRequest;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResponse;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;

public interface ClienteService {

    List<ClienteResponse> listar(String buscar);

    ClienteResponse obtener(Long id);

    ClienteResponse crear(ClienteRequest request);

    ClienteResponse actualizar(Long id, ClienteRequest request);

    void eliminar(Long id);

    // Usada por el registro de ventas: exige un cliente existente y activo.
    Cliente obtenerActivo(Long id);
}
