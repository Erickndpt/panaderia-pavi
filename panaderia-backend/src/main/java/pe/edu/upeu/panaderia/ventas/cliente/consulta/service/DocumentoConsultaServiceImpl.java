package pe.edu.upeu.panaderia.ventas.cliente.consulta.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.client.PeruApiClient;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.dto.DocumentoConsultaResponse;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaBadRequestException;

@Service
@RequiredArgsConstructor
public class DocumentoConsultaServiceImpl implements DocumentoConsultaService {

    private final PeruApiClient peruApiClient;

    @Override
    public DocumentoConsultaResponse consultar(String tipo, String numero) {
        if (tipo == null || numero == null) {
            throw new DocumentoConsultaBadRequestException("Formato de documento inválido.");
        }
        String tipoNormalizado = tipo.trim().toUpperCase();
        String numeroLimpio = numero.trim().replaceAll("\\s+", "");

        if ("SIN_DOCUMENTO".equals(tipoNormalizado)) {
            throw new DocumentoConsultaBadRequestException("No es posible consultar documentos sin número.");
        }

        if ("DNI".equals(tipoNormalizado)) {
            if (!numeroLimpio.matches("^\\d{8}$")) {
                throw new DocumentoConsultaBadRequestException("El DNI debe contener exactamente 8 dígitos.");
            }
            return peruApiClient.consultarDni(numeroLimpio);
        }

        if ("RUC".equals(tipoNormalizado)) {
            if (!numeroLimpio.matches("^\\d{11}$")) {
                throw new DocumentoConsultaBadRequestException("El RUC debe contener exactamente 11 dígitos.");
            }
            return peruApiClient.consultarRuc(numeroLimpio);
        }

        throw new DocumentoConsultaBadRequestException("Tipo de documento no soportado para consulta.");
    }
}
