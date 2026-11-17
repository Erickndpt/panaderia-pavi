package pe.edu.upeu.panaderia.ventas.venta.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;

public final class VentaSpecifications {

    private VentaSpecifications() {
    }

    public static Specification<Venta> tipoIgual(TipoVenta tipo) {
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Venta> estadoIgual(EstadoVenta estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Venta> fechaMayorIgual(LocalDateTime fechaDesde) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde);
    }

    public static Specification<Venta> fechaMenorIgual(LocalDateTime fechaHasta) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta);
    }

    public static Specification<Venta> paisDestinoContiene(String paisDestino) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("paisDestino")),
                "%" + paisDestino.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<Venta> contieneProductoId(Long productoId) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("detalles").get("idProducto"), productoId);
        };
    }

    public static Specification<Venta> clienteIgual(Long clienteId) {
        return (root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId);
    }

    public static Specification<Venta> vendedorIgual(String vendedorId) {
        return (root, query, cb) -> cb.equal(root.get("vendedorId"), vendedorId);
    }

    public static Specification<Venta> enRangoFechas(LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
            }
            return predicados.isEmpty() ? cb.conjunction() : cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}