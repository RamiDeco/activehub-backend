package com.activehub.usecases.listarresenasactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = ListarResenasActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarResenasActividadControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarResenasActividadService listarResenasActividadService;

    @Test
    void listar_devuelve200() {
        UUID actividadId = UUID.randomUUID();
        when(listarResenasActividadService.listar(any())).thenReturn(List.of(
                new ListarResenasActividadResponse(
                        UUID.randomUUID(), UUID.randomUUID(),
                        new ListarResenasActividadResponse.Alumno(UUID.randomUUID(), "Ana", "Lopez"),
                        5, "Genial", Instant.now())));

        assertThat(mvc.get().uri("/api/actividades/{id}/resenas", actividadId).exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].puntaje").isEqualTo(5);
    }
}
