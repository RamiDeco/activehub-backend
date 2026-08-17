package com.activehub.usecases.listarresenasinstructor;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = ListarResenasInstructorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarResenasInstructorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarResenasInstructorService listarResenasInstructorService;

    @Test
    void listar_devuelve200() {
        when(listarResenasInstructorService.listar(any())).thenReturn(List.of(
                new ListarResenasInstructorResponse(
                        UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "Yoga",
                        new ListarResenasInstructorResponse.Alumno(UUID.randomUUID(), "Ana", "Lopez"),
                        3, "Estuvo bien", false, Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/instructor/resenas")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].actividadNombre").isEqualTo("Yoga");
    }
}
