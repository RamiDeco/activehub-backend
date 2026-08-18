package com.activehub.usecases.actualizarestadousuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

@WebMvcTest(controllers = ActualizarEstadoUsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActualizarEstadoUsuarioControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ActualizarEstadoUsuarioService actualizarEstadoUsuarioService;

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void actualizar_bodyValido_devuelve200() {
        when(actualizarEstadoUsuarioService.actualizar(any(), any(), any()))
                .thenReturn(new ActualizarEstadoUsuarioResponse(USUARIO_ID, "SUSPENDIDO"));

        assertThat(mvc.post().uri("/api/admin/usuarios/{id}/estado", USUARIO_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"SUSPENDIDO\"}")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("SUSPENDIDO");
    }

    @Test
    void actualizar_sinEstado_devuelve400() {
        assertThat(mvc.post().uri("/api/admin/usuarios/{id}/estado", USUARIO_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"\"}")
                .exchange())
                .hasStatus(400);
    }
}
