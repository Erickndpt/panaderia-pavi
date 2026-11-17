package pe.edu.upeu.panaderia.ventas.cliente.consulta.service;

import pe.edu.upeu.panaderia.ventas.cliente.consulta.dto.DocumentoConsultaResponse;

public interface DocumentoConsultaService {
    DocumentoConsultaResponse consultar(String tipo, String numero);
}
