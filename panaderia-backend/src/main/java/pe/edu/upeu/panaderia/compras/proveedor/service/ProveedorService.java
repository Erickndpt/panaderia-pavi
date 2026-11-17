package pe.edu.upeu.panaderia.compras.proveedor.service;

import java.util.List;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResponse;

public interface ProveedorService {

    List<ProveedorResponse> listar();

    ProveedorResponse obtener(Long id);

    ProveedorResponse crear(ProveedorRequest request);

    ProveedorResponse actualizar(Long id, ProveedorRequest request);

    void eliminar(Long id);
}