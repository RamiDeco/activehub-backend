package com.activehub.usecases.preguntaralasistente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.IaNoDisponibleException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = PreguntarAlAsistenteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PreguntarAlAsistenteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PreguntarAlAsistenteService preguntarAlAsistenteService;

    private static final String BODY_OK = "{ \"pregunta\": \"¿cómo me inscribo?\" }";

    @Test
    void preguntar_bodyValido_devuelveLaRespuestaYSusSecciones() {
        when(preguntarAlAsistenteService.responder(any(), any())).thenReturn(
                new PreguntarAlAsistenteResponse(
                        "Presioná \"Inscribirme y pagar\".", List.of("2.6 Inscribirse y pagar una clase"), false));

        assertThat(mvc.post().uri("/api/asistente/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_OK)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.sinInformacion").isEqualTo(false);
    }

    /** El endpoint es público: sin sesión el límite se cuenta por IP. */
    @Test
    void preguntar_sinSesion_usaLaIpComoClaveDelLimite() {
        when(preguntarAlAsistenteService.responder(any(), any()))
                .thenReturn(new PreguntarAlAsistenteResponse("Una respuesta.", List.of(), false));

        assertThat(mvc.post().uri("/api/asistente/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_OK)
                .exchange())
                .hasStatus(200);

        verify(preguntarAlAsistenteService).responder(any(), eq("ip:127.0.0.1"));
    }

    @Test
    void preguntar_conXForwardedFor_tomaLaPrimeraIpDeLaCadena() {
        when(preguntarAlAsistenteService.responder(any(), any()))
                .thenReturn(new PreguntarAlAsistenteResponse("Una respuesta.", List.of(), false));

        assertThat(mvc.post().uri("/api/asistente/consultas")
                .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_OK)
                .exchange())
                .hasStatus(200);

        // Detrás del proxy del despliegue, getRemoteAddr() es la del proxy: una sola clave para
        // todo el mundo convertiría el límite por cliente en un límite global.
        verify(preguntarAlAsistenteService).responder(any(), eq("ip:203.0.113.5"));
    }

    @Test
    void preguntar_preguntaVacia_devuelve400() {
        assertThat(mvc.post().uri("/api/asistente/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"pregunta\": \"   \" }")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void preguntar_preguntaEnorme_devuelve400SinLlegarAlModelo() {
        String larga = "a".repeat(401);

        assertThat(mvc.post().uri("/api/asistente/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"pregunta\": \"" + larga + "\" }")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void preguntar_conHtmlEnLaPregunta_devuelve400() {
        assertThat(mvc.post().uri("/api/asistente/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"pregunta\": \"<script>alert(1)</script>\" }")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void preguntar_sinModeloDisponible_devuelve503() {
        when(preguntarAlAsistenteService.responder(any(), any()))
                .thenThrow(new IaNoDisponibleException());

        assertThat(mvc.post().uri("/api/asistente/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_OK)
                .exchange())
                .hasStatus(503)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("IA_NO_DISPONIBLE");
    }
}
