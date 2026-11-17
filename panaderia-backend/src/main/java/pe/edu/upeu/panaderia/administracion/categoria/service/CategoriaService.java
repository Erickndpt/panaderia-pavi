package pe.edu.upeu.panaderia.administracion.categoria.service;

import java.util.List;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaRequest;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResponse;

public interface CategoriaService {

    List<CategoriaResponse> listar();

    CategoriaResponse obtener(Long id);

    CategoriaResponse crear(CategoriaRequest request);

    CategoriaResponse actualizar(Long id, CategoriaRequest request);

    void eliminar(Long id);
}