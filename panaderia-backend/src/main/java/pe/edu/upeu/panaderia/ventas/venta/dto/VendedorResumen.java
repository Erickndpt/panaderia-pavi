package pe.edu.upeu.panaderia.ventas.venta.dto;

/**
 * Identidad legible del vendedor. Se expone null cuando la venta histórica
 * no tiene datos de usuario; la interfaz nunca muestra el UUID como nombre.
 */
public record VendedorResumen(String id, String username, String nombre) {
}
