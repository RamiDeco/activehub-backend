package com.activehub.usecases.registraradmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.EmailEnUsoException;
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

@WebMvcTest(controllers = RegistrarAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegistrarAdminControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegistrarAdminService registrarAdminService;

    private static final String BODY_VALIDO = """
            {
              "nombre": "Ana",
              "apellido": "Pérez",
              "email": "ana@activehub.test",
              "telefono": "2611234567",
              "password": "Password1"
            }
            """;

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    private RegistrarAdminResponse respuestaOk() {
        return new RegistrarAdminResponse(
                UUID.randomUUID(), "Ana", "Pérez", "ana@activehub.test", "2611234567",
                null, "ADMIN", "ACTIVO", 0, Instant.now());
    }

    @Test
    void registrar_bodyValido_devuelve201ConElAdmin() {
        when(registrarAdminService.registrar(any(), any())).thenReturn(respuestaOk());

        assertThat(mvc.post().uri("/api/admin/usuarios/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content(BODY_VALIDO)
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.rol").isEqualTo("ADMIN");
    }

    @Test
    void registrar_passwordSinNumero_devuelve400SinLlamarAlService() {
        String flojo = BODY_VALIDO.replace("Password1", "Passwordd");

        assertThat(mvc.post().uri("/api/admin/usuarios/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content(flojo)
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");

        verify(registrarAdminService, never()).registrar(any(), any());
    }

    @Test
    void registrar_dniDeCincoDigitos_devuelve400() {
        String conDniCorto = BODY_VALIDO.replace("\"password\"", "\"dni\": \"12345\", \"password\"");

        assertThat(mvc.post().uri("/api/admin/usuarios/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content(conDniCorto)
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.fieldErrors.dni").asString().contains("7 u 8");
    }

    @Test
    void registrar_emailEnUso_devuelve409() {
        when(registrarAdminService.registrar(any(), any())).thenThrow(new EmailEnUsoException());

        assertThat(mvc.post().uri("/api/admin/usuarios/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content(BODY_VALIDO)
                .exchange())
                .hasStatus(409)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("EMAIL_EN_USO");
    }
}
