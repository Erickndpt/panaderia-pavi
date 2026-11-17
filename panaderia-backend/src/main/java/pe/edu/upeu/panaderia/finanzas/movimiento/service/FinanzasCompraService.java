package pe.edu.upeu.panaderia.finanzas.movimiento.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Operaciones financieras que necesita exclusivamente el módulo de compras. */
public interface FinanzasCompraService {

    void registrarEgresoCompra(
            Long compraId, LocalDateTime fecha, BigDecimal monto);

    void anularEgresoCompra(Long compraId);
}
