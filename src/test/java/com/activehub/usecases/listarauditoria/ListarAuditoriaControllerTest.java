package com.activehub.usecases.listarauditoria;

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

@WebMvcTest(controllers = ListarAuditoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarAuditoriaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarAuditoriaService listarAuditoriaService;

    @Test
    void listar_devuelve200() {
        when(listarAuditoriaService.listar()).thenReturn(List.of(
                new ListarAuditoriaResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Ana Lopez", "ADMIN",
                        "LOGIN_OK", "Usuario", UUID.randomUUID(), null, "Ana Lopez inició sesión.",
                        Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/auditoria")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].actorNombre").isEqualTo("Ana Lopez");
    }
}
