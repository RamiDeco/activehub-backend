package com.activehub.usecases.notificarausenciaprofesor;

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

@WebMvcTest(controllers = NotificarAusenciaProfesorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificarAusenciaProfesorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private NotificarAusenciaProfesorService notificarAusenciaProfesorService;

    private static final UUID CLASE_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void notificar_bodyValido_devuelve200() {
        when(notificarAusenciaProfesorService.notificar(any(), any(), any()))
                .thenReturn(new NotificarAusenciaProfesorResponse(CLASE_ID, "Cancelada", 3));

        assertThat(mvc.post().uri("/api/instructor/clases/{id}/notificar-ausencia", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\": \"No voy a poder dar la clase.\"}")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.alumnosNotificados").isEqualTo(3);
    }

    @Test
    void notificar_sinMensaje_devuelve400() {
        assertThat(mvc.post().uri("/api/instructor/clases/{id}/notificar-ausencia", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\": \"\"}")
                .exchange())
                .hasStatus(400);
    }
}
