package com.activehub.usecases.listarmisnotificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.time.Instant;
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

@WebMvcTest(controllers = ListarMisNotificacionesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarMisNotificacionesControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarMisNotificacionesService listarMisNotificacionesService;

    @Test
    void listar_devuelve200() {
        when(listarMisNotificacionesService.listar(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new ListarMisNotificacionesResponse(
                        UUID.randomUUID(), "AUSENCIA_PROFESOR", "El instructor avisó que no podrá dar la clase.",
                        UUID.randomUUID(), false, Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/notificaciones")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].tipo").isEqualTo("AUSENCIA_PROFESOR");
    }
}
