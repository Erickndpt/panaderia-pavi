package pe.edu.upeu.panaderia.administracion.categoria.controller;

import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.SecurityConfig;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaRequest;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResponse;
import pe.edu.upeu.panaderia.administracion.categoria.service.CategoriaService;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.security.SecurityTestSupport;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CategoriaController.class)
@Import({SecurityConfig.class, SecurityTestSupport.class})
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoriaService categoriaService;

    private CategoriaRequest categoriaRequestValida() {
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre("Panificación");
        request.setDescripcion("Productos de panadería");
        return request;
    }

    private CategoriaResponse categoriaResponseValida() {
        return CategoriaResponse.builder()
                .id(1L)
                .nombre("Panificación")
                .descripcion("Productos de panadería")
                .build();
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("GET /api/v1/categorias responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(categoriaService.listar()).thenReturn(List.of(categoriaResponseValida()));

        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/categorias con datos válidos responde 201")
    void crearDeberiaResponder201() throws Exception {
        when(categoriaService.crear(any(CategoriaRequest.class))).thenReturn(categoriaResponseValida());

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(categoriaRequestValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST con nombre vacío responde 400")
    void crearConNombreVacioDeberiaResponder400() throws Exception {
        CategoriaRequest request = categoriaRequestValida();
        request.setNombre("");

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/categorias/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(categoriaService.obtener(99L)).thenThrow(new ResourceNotFoundException("Categoría", 99L));

        mockMvc.perform(get("/api/v1/categorias/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/categorias/{id} con datos válidos responde 200")
    void actualizarDeberiaResponder200() throws Exception {
        when(categoriaService.actualizar(eq(1L), any(CategoriaRequest.class))).thenReturn(categoriaResponseValida());

        mockMvc.perform(put("/api/v1/categorias/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(categoriaRequestValida())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/categorias/{id} válido responde 204")
    void eliminarDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/v1/categorias/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
