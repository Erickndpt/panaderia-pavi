package pe.edu.upeu.panaderia.inventario.existencia.service;

import java.math.BigDecimal;
import java.util.List;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaRequest;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaResponse;

public interface ExistenciaService {

    /**
     * @param productoId filtro opcional; null devuelve todas las existencias.
     */
    List<ExistenciaResponse> listar(Long productoId);

    ExistenciaResponse obtener(Long id);

    ExistenciaResponse obtenerPorProducto(Long productoId);

    ExistenciaResponse crear(ExistenciaRequest request);

    ExistenciaResponse actualizar(Long id, ExistenciaRequest request);

    void eliminar(Long id);

    void descontar(Long productoId, BigDecimal cantidad);

    void reponer(Long productoId, BigDecimal cantidad);
}
