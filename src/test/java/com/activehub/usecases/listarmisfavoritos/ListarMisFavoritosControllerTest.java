package com.activehub.usecases.listarmisfavoritos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
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

@WebMvcTest(controllers = ListarMisFavoritosController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarMisFavoritosControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarMisFavoritosService listarMisFavoritosService;

    @Test
    void listar_devuelve200() {
        UUID actividadId = UUID.randomUUID();
        when(listarMisFavoritosService.listar(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(actividadId));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/alumno/favoritos")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0]").isEqualTo(actividadId.toString());
    }
}
