package com.activehub.usecases.actualizarcategoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = ActualizarCategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActualizarCategoriaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ActualizarCategoriaService actualizarCategoriaService;

    private static final UUID CATEGORIA_ID = UUID.randomUUID();

    @Test
    void actualizar_bodyValido_devuelve200() {
        when(actualizarCategoriaService.actualizar(any(), any(), any()))
                .thenReturn(new ActualizarCategoriaResponse(CATEGORIA_ID, "Running"));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.put().uri("/api/admin/categorias/{id}", CATEGORIA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(authentication)
                .content("{ \"nombre\": \"Running\" }")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.nombre").isEqualTo("Running");
    }

    @Test
    void actualizar_nombreVacio_devuelve400() {
        assertThat(mvc.put().uri("/api/admin/categorias/{id}", CATEGORIA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nombre\": \"\" }")
                .exchange())
                .hasStatus(400);
    }
}
