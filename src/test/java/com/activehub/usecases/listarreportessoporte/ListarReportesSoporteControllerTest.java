package com.activehub.usecases.listarreportessoporte;

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

@WebMvcTest(controllers = ListarReportesSoporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarReportesSoporteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarReportesSoporteService listarReportesSoporteService;

    @Test
    void listar_devuelve200() {
        when(listarReportesSoporteService.listar()).thenReturn(List.of(
                new ListarReportesSoporteResponse(
                        UUID.randomUUID(), "ana@mail.com", "No puedo pagar", "Me tira error.", "Abierto",
                        null, null, null, null, null, Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/soporte/reportes")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].asunto").isEqualTo("No puedo pagar");
    }
}
