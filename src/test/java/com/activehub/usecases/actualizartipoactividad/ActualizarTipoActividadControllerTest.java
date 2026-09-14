package com.activehub.usecases.actualizartipoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.TipoActividadDuplicadoException;
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

@WebMvcTest(controllers = ActualizarTipoActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActualizarTipoActividadControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ActualizarTipoActividadService actualizarTipoActividadService;

    private static final UUID TIPO_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();

    private static final String BODY_VALIDO = """
            { "nombre": "Trail running", "categoriaId": "%s" }
            """.formatted(CATEGORIA_ID);

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void actualizar_bodyValido_devuelve200ConElTipo() {
        when(actualizarTipoActividadService.actualizar(any(), any(), any()))
                .thenReturn(new ActualizarTipoActividadResponse(TIPO_ID, "Trail running", CATEGORIA_ID));

        assertThat(mvc.put().uri("/api/admin/tipos-actividad/{id}", TIPO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content(BODY_VALIDO)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.nombre").isEqualTo("Trail running");
    }

    @Test
    void actualizar_sinCategoria_devuelve400() {
        assertThat(mvc.put().uri("/api/admin/tipos-actividad/{id}", TIPO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"nombre\": \"Trail running\" }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");
    }

    @Test
    void actualizar_nombreVacio_devuelve400() {
        assertThat(mvc.put().uri("/api/admin/tipos-actividad/{id}", TIPO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"nombre\": \"\", \"categoriaId\": \"%s\" }".formatted(CATEGORIA_ID))
                .exchange())
                .hasStatus(400);
    }

    @Test
    void actualizar_nombreDuplicado_devuelve400ConElFieldError() {
        when(actualizarTipoActividadService.actualizar(any(), any(), any()))
                .thenThrow(new TipoActividadDuplicadoException());

        assertThat(mvc.put().uri("/api/admin/tipos-actividad/{id}", TIPO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content(BODY_VALIDO)
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.fieldErrors.nombre").asString().contains("Ya existe");
    }
}
