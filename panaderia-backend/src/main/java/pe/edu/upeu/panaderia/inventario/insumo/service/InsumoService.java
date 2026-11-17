package pe.edu.upeu.panaderia.inventario.insumo.service;

import java.math.BigDecimal;
import java.util.List;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoRequest;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;

public interface InsumoService {

    List<InsumoResponse> listar();

    InsumoResponse obtener(Long id);

    InsumoResponse crear(InsumoRequest request);

    InsumoResponse actualizar(Long id, InsumoRequest request);

    void eliminar(Long id);

    void aumentarStock(Long insumoId, BigDecimal cantidad);

    void descontarStock(Long insumoId, BigDecimal cantidad);
}