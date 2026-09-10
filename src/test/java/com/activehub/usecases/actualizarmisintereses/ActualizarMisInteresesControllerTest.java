package com.activehub.usecases.actualizarmisintereses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.NoEncontradoException;
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

@WebMvcTest(controllers = ActualizarMisInteresesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActualizarMisInteresesControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ActualizarMisInteresesService actualizarMisInteresesService;

    private UsernamePasswordAuthenticationToken alumno() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void actualizar_bodyValido_devuelve200ConLaLista() {
        var yoga = new ActualizarMisInteresesResponse.Interes(
                UUID.randomUUID(), "Yoga", UUID.randomUUID(), "Bienestar");
        when(actualizarMisInteresesService.actualizar(any(), any()))
                .thenReturn(new ActualizarMisInteresesResponse(UUID.randomUUID(), List.of(yoga)));

        assertThat(mvc.put().uri("/api/usuarios/me/intereses")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ \"tiposActividadId\": [\"11111111-1111-1111-1111-111111111111\"] }")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.intereses[0].nombre").isEqualTo("Yoga");
    }

    @Test
    void actualizar_sinCampo_devuelve400SinLlamarAlService() {
        assertThat(mvc.put().uri("/api/usuarios/me/intereses")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");

        verify(actualizarMisInteresesService, never()).actualizar(any(), any());
    }

    @Test
    void actualizar_instructor_devuelve404() {
        when(actualizarMisInteresesService.actualizar(any(), any()))
                .thenThrow(new NoEncontradoException("Solo un alumno puede editar sus intereses."));

        assertThat(mvc.put().uri("/api/usuarios/me/intereses")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ \"tiposActividadId\": [] }")
                .exchange())
                .hasStatus(404);
    }
}
