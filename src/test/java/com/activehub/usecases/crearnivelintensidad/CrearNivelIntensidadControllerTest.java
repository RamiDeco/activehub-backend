package com.activehub.usecases.crearnivelintensidad;

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

/** E4Ad-HU05 criterios 2 y 3. */
@WebMvcTest(controllers = CrearNivelIntensidadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearNivelIntensidadControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearNivelIntensidadService crearNivelIntensidadService;

    @Test
    void crear_bodyValido_devuelve201() {
        when(crearNivelIntensidadService.crear(any(), any()))
                .thenReturn(new CrearNivelIntensidadResponse(UUID.randomUUID(), "Física extrema", "Para atletas."));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/admin/niveles-intensidad")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(authentication)
                .content("{ \"nombre\": \"Física extrema\", \"descripcion\": \"Para atletas.\" }")
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.nombre").isEqualTo("Física extrema");
    }

    @Test
    void crear_nombreVacio_devuelve400() {
        assertThat(mvc.post().uri("/api/admin/niveles-intensidad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nombre\": \"\", \"descripcion\": \"Para atletas.\" }")
                .exchange())
                .hasStatus(400);
    }

    @Test
    void crear_descripcionVacia_devuelve400() {
        assertThat(mvc.post().uri("/api/admin/niveles-intensidad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nombre\": \"Física extrema\", \"descripcion\": \"  \" }")
                .exchange())
                .hasStatus(400);
    }
}
