package pe.edu.upeu.panaderia.inventario.existencia.dto;

import java.math.BigDecimal;

/**
 * Respuesta de la existencia. A diferencia de {@link ExistenciaRequest}, no devuelve
 * productoId plano: expone un resumen del producto padre (módulo Administración) para
 * que el listado muestre el nombre sin llamadas adicionales.
 */
public record ExistenciaResponse(Long id, ProductoResumen producto, BigDecimal cantidad) {

    /**
     * Proyección local del producto padre. Evita exponer ProductoResponse (precio,
     * categoría) dentro del módulo Inventario.
     */
    public record ProductoResumen(Long id, String nombre) {
    }
}
