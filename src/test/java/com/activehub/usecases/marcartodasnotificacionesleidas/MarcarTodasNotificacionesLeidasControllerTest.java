package com.activehub.usecases.marcartodasnotificacionesleidas;

import static org.assertj.core.api.Assertions.assertThat;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = MarcarTodasNotificacionesLeidasController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MarcarTodasNotificacionesLeidasControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MarcarTodasNotificacionesLeidasService marcarTodasNotificacionesLeidasService;

    @Test
    void marcarLeidas_devuelve200() {
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/notificaciones/marcar-leidas")
                .principal(authentication)
                .exchange())
                .hasStatus(200);
    }
}
