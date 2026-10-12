package com.activehub.usecases.registrarinteraccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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

@WebMvcTest(controllers = RegistrarInteraccionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegistrarInteraccionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegistrarInteraccionService registrarInteraccionService;

    @Test
    void registrar_vista_devuelve204YUsaElAlumnoDelToken() {
        UUID alumnoId = UUID.randomUUID();
        var authentication = new UsernamePasswordAuthenticationToken(alumnoId, null, List.of());

        assertThat(mvc.post().uri("/api/alumno/interacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"VISTA_ACTIVIDAD\",\"actividadId\":\"" + UUID.randomUUID() + "\"}")
                        .principal(authentication))
                .hasStatus(204);

        verify(registrarInteraccionService).registrar(eq(alumnoId), any());
    }

    @Test
    void registrar_sinTipo_devuelve400() {
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/alumno/interacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"termino\":\"yoga\"}")
                        .principal(authentication))
                .hasStatus(400);
    }

    /** El termino es texto libre del usuario: va con @SinHtml, como todo lo demas. */
    @Test
    void registrar_terminoConHtml_devuelve400() {
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/alumno/interacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"BUSQUEDA\",\"termino\":\"<script>alert(1)</script>\"}")
                        .principal(authentication))
                .hasStatus(400);
    }
}
