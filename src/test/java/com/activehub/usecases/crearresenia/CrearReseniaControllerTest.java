package com.activehub.usecases.crearresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = CrearReseniaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearReseniaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearReseniaService crearReseniaService;

    private static final UUID CLASE_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void crear_bodyValido_devuelve201() {
        when(crearReseniaService.crear(any(), any(), any())).thenReturn(
                new CrearReseniaResponse(UUID.randomUUID(), CLASE_ID, UUID.randomUUID(), 5, "Muy buena", true, Instant.now()));

        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/resenas", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"puntaje\": 5, \"comentario\": \"Muy buena\"}")
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.enModeracion").isEqualTo(true);
    }

    @Test
    void crear_sinComentario_devuelve400() {
        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/resenas", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"puntaje\": 5, \"comentario\": \"\"}")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void crear_puntajeFueraDeRango_devuelve400() {
        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/resenas", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"puntaje\": 6, \"comentario\": \"Muy buena\"}")
                .exchange())
                .hasStatus(400);
    }
}
