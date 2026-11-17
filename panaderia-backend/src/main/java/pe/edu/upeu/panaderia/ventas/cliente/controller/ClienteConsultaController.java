package pe.edu.upeu.panaderia.ventas.cliente.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.dto.DocumentoConsultaResponse;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.service.DocumentoConsultaService;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteConsultaController {

    private final DocumentoConsultaService documentoConsultaService;

    @GetMapping("/consulta-documento")
    public ResponseEntity<DocumentoConsultaResponse> consultarDocumento(
            @RequestParam String tipo,
            @RequestParam String numero) {
        DocumentoConsultaResponse response = documentoConsultaService.consultar(tipo, numero);
        return ResponseEntity.ok(response);
    }
}
