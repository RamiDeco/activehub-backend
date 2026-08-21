package com.activehub.usecases.crearcategoria;

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

@WebMvcTest(controllers = CrearCategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearCategoriaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearCategoriaService crearCategoriaService;

    @Test
    void crear_bodyValido_devuelve201() {
        when(crearCategoriaService.crear(any(), any()))
                .thenReturn(new CrearCategoriaResponse(UUID.randomUUID(), "Running"));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/admin/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(authentication)
                .content("{ \"nombre\": \"Running\" }")
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.nombre").isEqualTo("Running");
    }

    @Test
    void crear_nombreVacio_devuelve400() {
        assertThat(mvc.post().uri("/api/admin/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nombre\": \"\" }")
                .exchange())
                .hasStatus(400);
    }
}
