package com.activehub.usecases.eliminarcategoria;

import static org.assertj.core.api.Assertions.assertThat;

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

@WebMvcTest(controllers = EliminarCategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EliminarCategoriaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private EliminarCategoriaService eliminarCategoriaService;

    @Test
    void eliminar_devuelve204() {
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.delete().uri("/api/admin/categorias/{id}", UUID.randomUUID())
                .principal(authentication)
                .exchange())
                .hasStatus(204);
    }
}
