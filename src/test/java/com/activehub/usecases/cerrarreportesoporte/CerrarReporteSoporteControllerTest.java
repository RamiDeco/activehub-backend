package com.activehub.usecases.cerrarreportesoporte;

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

@WebMvcTest(controllers = CerrarReporteSoporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CerrarReporteSoporteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CerrarReporteSoporteService cerrarReporteSoporteService;

    @Test
    void cerrar_devuelve200() {
        UUID id = UUID.randomUUID();
        when(cerrarReporteSoporteService.cerrar(any(), any(), any()))
                .thenReturn(new CerrarReporteSoporteResponse(id, "Cerrado", "Resuelto.", Instant.now()));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/admin/soporte/reportes/{id}/cerrar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(authentication)
                .content("{ \"respuesta\": \"Resuelto.\" }")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("Cerrado");
    }

    /** La respuesta es opcional: cerrar sin texto tiene que ser un 200, no un 400. */
    @Test
    void cerrar_sinRespuesta_devuelve200() {
        UUID id = UUID.randomUUID();
        when(cerrarReporteSoporteService.cerrar(any(), any(), any()))
                .thenReturn(new CerrarReporteSoporteResponse(id, "Cerrado", null, Instant.now()));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/admin/soporte/reportes/{id}/cerrar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(authentication)
                .content("{}")
                .exchange())
                .hasStatus(200);
    }
}
