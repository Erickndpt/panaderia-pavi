package pe.edu.upeu.panaderia.administracion.categoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.panaderia.administracion.categoria.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}