package com.activehub.usecases.listardenunciasadmin;

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

@WebMvcTest(controllers = ListarDenunciasAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarDenunciasAdminControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarDenunciasAdminService listarDenunciasAdminService;

    @Test
    void listar_devuelve200() {
        when(listarDenunciasAdminService.listar()).thenReturn(List.of(
                new ListarDenunciasAdminResponse(
                        UUID.randomUUID(), "CLASE", UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "Yoga",
                        new ListarDenunciasAdminResponse.Persona(UUID.randomUUID(), "Ana", "Lopez"),
                        new ListarDenunciasAdminResponse.Persona(UUID.randomUUID(), "Franco", "Gonzales"),
                        new ListarDenunciasAdminResponse.Persona(UUID.randomUUID(), "Ana", "Lopez"),
                        null, "No se presentó", "Pendiente", null, null, null, Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/denuncias")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].instructor.nombre").isEqualTo("Franco");
    }
}
