package pe.edu.upeu.panaderia.administracion.categoria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
}