package com.activehub.usecases.listarresenaspendientes;

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

@WebMvcTest(controllers = ListarResenasPendientesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarResenasPendientesControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarResenasPendientesService listarResenasPendientesService;

    @Test
    void listar_devuelve200() {
        when(listarResenasPendientesService.listar()).thenReturn(List.of(
                new ListarResenasPendientesResponse(
                        UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "Running",
                        new ListarResenasPendientesResponse.Alumno(UUID.randomUUID(), "Bruno", "Diaz"),
                        2, "No me gustó", Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/resenas")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].actividadNombre").isEqualTo("Running");
    }
}
