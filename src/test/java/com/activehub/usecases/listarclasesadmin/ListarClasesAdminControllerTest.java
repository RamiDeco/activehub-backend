package com.activehub.usecases.listarclasesadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = ListarClasesAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarClasesAdminControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarClasesAdminService listarClasesAdminService;

    @Test
    void listar_devuelve200() {
        when(listarClasesAdminService.listar()).thenReturn(List.of(
                new ListarClasesAdminResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Yoga", Instant.now(), Instant.now().plusSeconds(3600),
                        "Programada", 10, 3, new BigDecimal("4500"))));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/clases")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].actividadNombre").isEqualTo("Yoga");
    }
}
