package com.activehub.usecases.creartipoactividad;

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

@WebMvcTest(controllers = CrearTipoActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearTipoActividadControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearTipoActividadService crearTipoActividadService;

    @Test
    void crear_bodyValido_devuelve201() {
        UUID categoriaId = UUID.randomUUID();
        when(crearTipoActividadService.crear(any(), any()))
                .thenReturn(new CrearTipoActividadResponse(UUID.randomUUID(), "Yoga", categoriaId));

        String body = """
                { "nombre": "Yoga", "categoriaId": "%s" }
                """.formatted(categoriaId);

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/admin/tipos-actividad")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(authentication)
                .content(body)
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.nombre").isEqualTo("Yoga");
    }

    @Test
    void crear_nombreVacio_devuelve400() {
        String body = """
                { "nombre": "", "categoriaId": "%s" }
                """.formatted(UUID.randomUUID());

        assertThat(mvc.post().uri("/api/admin/tipos-actividad")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange())
                .hasStatus(400);
    }
}
