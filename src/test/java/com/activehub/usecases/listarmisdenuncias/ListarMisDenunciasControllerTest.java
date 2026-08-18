package com.activehub.usecases.listarmisdenuncias;

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

@WebMvcTest(controllers = ListarMisDenunciasController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarMisDenunciasControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarMisDenunciasService listarMisDenunciasService;

    @Test
    void listar_devuelve200() {
        UUID alumnoId = UUID.randomUUID();
        when(listarMisDenunciasService.listar(alumnoId)).thenReturn(List.of(
                new ListarMisDenunciasResponse(
                        UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(), "Yoga",
                        "No se presentó", "Pendiente", Instant.now())));

        var authentication = new UsernamePasswordAuthenticationToken(alumnoId, null, List.of());

        assertThat(mvc.get().uri("/api/alumno/denuncias")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].actividadNombre").isEqualTo("Yoga");
    }
}
