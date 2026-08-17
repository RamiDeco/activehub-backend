package com.activehub.usecases.marcarasistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
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

@WebMvcTest(controllers = MarcarAsistenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MarcarAsistenciaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MarcarAsistenciaService marcarAsistenciaService;

    private static final UUID INSCRIPCION_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void marcar_bodyValido_devuelve200() {
        when(marcarAsistenciaService.marcar(any(), any(), any()))
                .thenReturn(new MarcarAsistenciaResponse(INSCRIPCION_ID, true));

        assertThat(mvc.post().uri("/api/instructor/inscripciones/{id}/asistencia", INSCRIPCION_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"presente\": true}")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.presente").isEqualTo(true);
    }

    @Test
    void marcar_sinPresente_devuelve400() {
        assertThat(mvc.post().uri("/api/instructor/inscripciones/{id}/asistencia", INSCRIPCION_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange())
                .hasStatus(400);
    }
}
