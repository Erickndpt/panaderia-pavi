package pe.edu.upeu.panaderia.finanzas.movimiento.service;

import java.util.List;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.MovimientoFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.ResumenFinancieroResponse;

public interface FinanzasService {

    List<MovimientoFinancieroResponse> listar();

    MovimientoFinancieroResponse obtener(Long id);

    ResumenFinancieroResponse resumen();
}
