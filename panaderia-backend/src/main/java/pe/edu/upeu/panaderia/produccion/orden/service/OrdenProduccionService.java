package pe.edu.upeu.panaderia.produccion.orden.service;

import java.util.List;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionResponse;

public interface OrdenProduccionService {

    OrdenProduccionResponse registrar(OrdenProduccionRequest request);

    List<OrdenProduccionResponse> listar();

    OrdenProduccionResponse obtener(Long id);
}