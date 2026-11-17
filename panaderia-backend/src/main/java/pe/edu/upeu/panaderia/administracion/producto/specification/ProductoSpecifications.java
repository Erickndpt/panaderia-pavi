package pe.edu.upeu.panaderia.administracion.producto.specification;

import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upeu.panaderia.administracion.producto.entity.Producto;

public final class ProductoSpecifications {

    private ProductoSpecifications() {
    }

    public static Specification<Producto> nombreContiene(String nombre) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("nombre")),
                "%" + nombre.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<Producto> categoriaIgual(Long categoriaId) {
        return (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Producto> precioMayorIgual(BigDecimal precioMin) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("precio"), precioMin);
    }

    public static Specification<Producto> precioMenorIgual(BigDecimal precioMax) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("precio"), precioMax);
    }
}