package com.activehub.usecases.crearreportesoporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = CrearReporteSoporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearReporteSoporteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearReporteSoporteService crearReporteSoporteService;

    private static final String BODY_OK =
            "{ \"email\": \"ana@mail.com\", \"asunto\": \"No puedo pagar\", \"detalle\": \"Me tira error.\" }";

    @Test
    void crear_bodyValido_devuelve201() {
        when(crearReporteSoporteService.crear(any(), any()))
                .thenReturn(new CrearReporteSoporteResponse(UUID.randomUUID(), "Abierto", Instant.now()));

        assertThat(mvc.post().uri("/api/soporte/reportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_OK)
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("Abierto");
    }

    /** Sin principal el autor viaja en null y el reporte queda anonimo: el endpoint es publico. */
    @Test
    void crear_sinSesion_pasaAutorNull() {
        when(crearReporteSoporteService.crear(any(), any()))
                .thenReturn(new CrearReporteSoporteResponse(UUID.randomUUID(), "Abierto", Instant.now()));

        assertThat(mvc.post().uri("/api/soporte/reportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_OK)
                .exchange())
                .hasStatus(201);

        verify(crearReporteSoporteService).crear(any(), isNull());
    }

    @Test
    void crear_asuntoVacio_devuelve400() {
        assertThat(mvc.post().uri("/api/soporte/reportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"ana@mail.com\", \"asunto\": \"\", \"detalle\": \"Algo\" }")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void crear_emailInvalido_devuelve400() {
        assertThat(mvc.post().uri("/api/soporte/reportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"no-es-un-mail\", \"asunto\": \"Hola\", \"detalle\": \"Algo\" }")
                .exchange())
                .hasStatus(400);
    }
}
