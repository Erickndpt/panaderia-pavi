package pe.edu.upeu.panaderia.ventas;

import org.springframework.stereotype.Component;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.exception.AccesoDenegadoException;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;

/**
 * Única comprobación de propiedad de una venta. SUPERVISOR y ADMIN no pasan
 * por el filtro; un VENDEDOR solo puede operar sobre ventas cuyo VENDEDOR_ID
 * coincide con su claim sub.
 */
@Component
public class AutorizacionVenta {

    public void validarPropiedad(Venta venta, IdentidadJwt identidad) {
        if (identidad == null || !identidad.vendedor()) {
            return;
        }
        if (venta.getVendedorId() == null || !venta.getVendedorId().equals(identidad.id())) {
            throw new AccesoDenegadoException(
                    "No tiene permiso para acceder a la venta con id " + venta.getId());
        }
    }

    public void validarPuedeAnularPago(IdentidadJwt identidad) {
        if (identidad != null && identidad.vendedor()) {
            throw new AccesoDenegadoException("No tiene permiso para anular pagos");
        }
    }
}
