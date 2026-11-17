package pe.edu.upeu.panaderia.administracion.categoria.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaRequest;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResponse;
import pe.edu.upeu.panaderia.administracion.categoria.entity.Categoria;
import pe.edu.upeu.panaderia.administracion.categoria.mapper.CategoriaMapper;
import pe.edu.upeu.panaderia.administracion.categoria.repository.CategoriaRepository;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return categoriaMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        Categoria categoria = categoriaRepository.save(categoriaMapper.toEntity(request));
        log.info("Categoría creada con id {}", categoria.getId());
        return categoriaMapper.toResponse(categoria);
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarOFallar(id);
        categoriaMapper.actualizar(request, categoria);
        log.info("Categoría actualizada con id {}", id);
        return categoriaMapper.toResponse(categoria);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarOFallar(id);
        categoriaRepository.delete(categoria);
        log.info("Categoría eliminada con id {}", id);
    }

    private Categoria buscarOFallar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }
}