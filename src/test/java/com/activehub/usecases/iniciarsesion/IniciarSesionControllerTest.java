package com.activehub.usecases.iniciarsesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.UsuarioSuspendidoException;
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

@WebMvcTest(controllers = IniciarSesionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class IniciarSesionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IniciarSesionService iniciarSesionService;

    private static final String BODY = """
            { "email": "martina@email.com", "password": "Password1" }
            """;

    @Test
    void login_credencialesValidas_devuelve200ConTokenYUsuario() {
        var usuarioDto = new IniciarSesionResponse.Usuario(
                UUID.randomUUID(), "Martina", "Gómez", "martina@email.com", "2611234567",
                LocalDate.of(2000, 5, 10), "ALUMNO", "ACTIVO", 0, Instant.now());
        when(iniciarSesionService.login(any())).thenReturn(new IniciarSesionResponse("token-jwt", usuarioDto));

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.token").isEqualTo("token-jwt");
    }

    @Test
    void login_credencialesInvalidas_devuelve401ConCodigoCredencialesInvalidas() {
        when(iniciarSesionService.login(any())).thenThrow(new CredencialesInvalidasException());

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange())
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("CREDENCIALES_INVALIDAS");
    }

    @Test
    void login_usuarioSuspendido_devuelve403ConCodigoUsuarioSuspendido() {
        when(iniciarSesionService.login(any())).thenThrow(new UsuarioSuspendidoException());

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange())
                .hasStatus(403)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("USUARIO_SUSPENDIDO");
    }
}
