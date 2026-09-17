package com.activehub.usecases.listarmisresenas;

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

@WebMvcTest(controllers = ListarMisResenasController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarMisResenasControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarMisResenasService listarMisResenasService;

    @Test
    void listar_devuelve200() {
        when(listarMisResenasService.listar(any())).thenReturn(List.of(
                new ListarMisResenasResponse(
                        UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "Yoga",
                        "Carla Nuñez", 4, "Buena clase", true, false, null, null, Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/alumno/resenas")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].actividadNombre").isEqualTo("Yoga");
    }
}
