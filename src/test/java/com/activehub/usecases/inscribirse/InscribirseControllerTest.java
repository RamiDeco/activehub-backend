package com.activehub.usecases.inscribirse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.SinCuposDisponiblesException;
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

@WebMvcTest(controllers = InscribirseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InscribirseControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private InscribirseService inscribirseService;

    private static final UUID CLASE_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void inscribirse_bodyValido_devuelve201() {
        when(inscribirseService.inscribirse(any(), any(), any())).thenReturn(
                new InscribirseResponse(UUID.randomUUID(), CLASE_ID, UUID.randomUUID(), "Inscripto", Instant.now(), UUID.randomUUID()));

        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/inscripciones", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metodoPago\": \"Mercado Pago\"}")
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("Inscripto");
    }

    @Test
    void inscribirse_metodoPagoInvalido_devuelve400() {
        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/inscripciones", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metodoPago\": \"Bitcoin\"}")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void inscribirse_sinCupos_devuelve409() {
        when(inscribirseService.inscribirse(any(), any(), any())).thenThrow(new SinCuposDisponiblesException());

        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/inscripciones", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metodoPago\": \"Efectivo\"}")
                .exchange())
                .hasStatus(409)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("SIN_CUPOS_DISPONIBLES");
    }
}
