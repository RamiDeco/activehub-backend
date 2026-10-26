package com.activehub.usecases.actualizarmicondicionsalud;

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

@WebMvcTest(controllers = ActualizarMiCondicionSaludController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActualizarMiCondicionSaludControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ActualizarMiCondicionSaludService actualizarMiCondicionSaludService;

    private UsernamePasswordAuthenticationToken alumno() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void actualizar_bodyValido_devuelve200ConElDato() {
        when(actualizarMiCondicionSaludService.actualizar(any(), any()))
                .thenReturn(new ActualizarMiCondicionSaludResponse(UUID.randomUUID(), "Asma"));

        assertThat(mvc.put().uri("/api/usuarios/me/salud")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ \"condicionSalud\": \"Asma\" }")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.condicionSalud").isEqualTo("Asma");
    }

    @Test
    void actualizar_masDe500Caracteres_devuelve400SinLlamarAlService() {
        assertThat(mvc.put().uri("/api/usuarios/me/salud")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ \"condicionSalud\": \"" + "a".repeat(501) + "\" }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");

        verify(actualizarMiCondicionSaludService, never()).actualizar(any(), any());
    }

    @Test
    void actualizar_conHtml_devuelve400() {
        assertThat(mvc.put().uri("/api/usuarios/me/salud")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ \"condicionSalud\": \"<script>alert(1)</script>\" }")
                .exchange())
                .hasStatus(400);

        verify(actualizarMiCondicionSaludService, never()).actualizar(any(), any());
    }

    @Test
    void actualizar_sinPerfilDeAlumno_devuelve404() {
        when(actualizarMiCondicionSaludService.actualizar(any(), any()))
                .thenThrow(new NoEncontradoException("Solo un alumno puede editar este dato."));

        assertThat(mvc.put().uri("/api/usuarios/me/salud")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(alumno())
                .content("{ \"condicionSalud\": null }")
                .exchange())
                .hasStatus(404);
    }
}
