package com.activehub.usecases.generarinformeactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.IaNoDisponibleException;
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

@WebMvcTest(controllers = GenerarInformeActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GenerarInformeActividadControllerTest {

    private static final UUID ACTIVIDAD_ID = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private GenerarInformeActividadService generarInformeActividadService;

    private GenerarInformeActividadResponse informe() {
        return new GenerarInformeActividadResponse(
                "Te puede gustar.",
                "Alta",
                List.of("Mejora tu flexibilidad."),
                List.of("Llevá agua."),
                Instant.parse("2026-03-10T12:00:00Z"));
    }

    @Test
    void generar_devuelveElInforme() {
        when(generarInformeActividadService.generar(any(), any(), any())).thenReturn(informe());
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/alumno/actividades/{id}/informe", ACTIVIDAD_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.afinidad").isEqualTo("Alta");
    }

    /** El alumno sale del token, nunca del cuerpo: nadie pide el informe del perfil de otro. */
    @Test
    void generar_usaElAlumnoDelTokenYLaClaseDelCuerpo() {
        UUID alumnoId = UUID.randomUUID();
        when(generarInformeActividadService.generar(any(), any(), any())).thenReturn(informe());
        var authentication = new UsernamePasswordAuthenticationToken(alumnoId, null, List.of());

        assertThat(mvc.post().uri("/api/alumno/actividades/{id}/informe", ACTIVIDAD_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"claseId\": \"abc\" }")
                .principal(authentication)
                .exchange())
                .hasStatus(200);

        verify(generarInformeActividadService).generar(
                eq(ACTIVIDAD_ID), eq(alumnoId), eq(new GenerarInformeActividadRequest("abc")));
    }

    /** Sin cuerpo también funciona: la clase es opcional. */
    @Test
    void generar_sinCuerpo_funcionaIgual() {
        when(generarInformeActividadService.generar(any(), any(), any())).thenReturn(informe());
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/alumno/actividades/{id}/informe", ACTIVIDAD_ID)
                .principal(authentication)
                .exchange())
                .hasStatus(200);
    }

    @Test
    void generar_actividadQueNoEsUnUuid_devuelve400() {
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/alumno/actividades/{id}/informe", "no-es-un-uuid")
                .principal(authentication)
                .exchange())
                .hasStatus(400);
    }

    @Test
    void generar_sinModeloDisponible_devuelve503() {
        when(generarInformeActividadService.generar(any(), any(), any()))
                .thenThrow(new IaNoDisponibleException());
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/alumno/actividades/{id}/informe", ACTIVIDAD_ID)
                .principal(authentication)
                .exchange())
                .hasStatus(503)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("IA_NO_DISPONIBLE");
    }
}
