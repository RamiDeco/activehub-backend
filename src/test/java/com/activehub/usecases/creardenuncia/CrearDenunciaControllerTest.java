package com.activehub.usecases.creardenuncia;

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

@WebMvcTest(controllers = CrearDenunciaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearDenunciaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearDenunciaService crearDenunciaService;

    private static final UUID CLASE_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void crear_bodyValido_devuelve201() {
        when(crearDenunciaService.crear(any(), any(), any())).thenReturn(
                new CrearDenunciaResponse(UUID.randomUUID(), CLASE_ID, UUID.randomUUID(), "No se presentó", "Pendiente", Instant.now()));

        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/denuncias", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"No se presentó\"}")
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("Pendiente");
    }

    @Test
    void crear_sinMotivo_devuelve400() {
        assertThat(mvc.post().uri("/api/alumno/clases/{claseId}/denuncias", CLASE_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"\"}")
                .exchange())
                .hasStatus(400);
    }
}
