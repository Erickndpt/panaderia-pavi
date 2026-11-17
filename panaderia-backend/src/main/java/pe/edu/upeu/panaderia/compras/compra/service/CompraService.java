package pe.edu.upeu.panaderia.compras.compra.service;

import java.util.List;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraRequest;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraResponse;

public interface CompraService {

    CompraResponse registrar(CompraRequest request);

    List<CompraResponse> listar();

    CompraResponse obtener(Long id);

    CompraResponse anular(Long id);
}