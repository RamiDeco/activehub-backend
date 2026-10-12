package com.activehub.usecases.registraralumno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = RegistrarAlumnoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegistrarAlumnoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegistrarAlumnoService registrarAlumnoService;

    private static final String BODY_VALIDO = """
            {
              "nombre": "Martina",
              "apellido": "Gómez",
              "email": "martina@email.com",
              "telefono": "2611234567",
              "password": "Password1",
              "fechaNacimiento": "2000-05-10",
              "intereses": ["11111111-1111-1111-1111-111111111111"],
              "aceptaTerminos": true
            }
            """;

    @Test
    void registrarAlumno_bodyValido_devuelve201ConTokenYUsuario() {
        var usuarioDto = new RegistrarAlumnoResponse.Usuario(
                UUID.randomUUID(), "Martina", "Gómez", "martina@email.com", "2611234567",
                LocalDate.of(2000, 5, 10), "ALUMNO", "ACTIVO", 0, Instant.now(), false, "LOCAL");
        when(registrarAlumnoService.registrar(any())).thenReturn(new RegistrarAlumnoResponse("token-jwt", true, usuarioDto));

        assertThat(mvc.post().uri("/api/auth/registro/alumno")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO)
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.token").isEqualTo("token-jwt");
    }

    @Test
    void registrarAlumno_emailInvalido_devuelve400ConFieldErrors() {
        String bodyEmailInvalido = BODY_VALIDO.replace("martina@email.com", "no-es-un-email");

        assertThat(mvc.post().uri("/api/auth/registro/alumno")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyEmailInvalido)
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");
    }

    @Test
    void registrarAlumno_aceptaTerminosFalse_devuelve400() {
        String bodySinAceptar = BODY_VALIDO.replace("\"aceptaTerminos\": true", "\"aceptaTerminos\": false");

        assertThat(mvc.post().uri("/api/auth/registro/alumno")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodySinAceptar)
                .exchange())
                .hasStatus(400);
    }
}
