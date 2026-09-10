package com.activehub.usecases.crearrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.RolDuplicadoException;
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

@WebMvcTest(controllers = CrearRolController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrearRolControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CrearRolService crearRolService;

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void crear_bodyValido_devuelve201ConElRol() {
        when(crearRolService.crear(any(), any()))
                .thenReturn(new CrearRolResponse(UUID.randomUUID(), "Soporte", "Atiende consultas", false, 0));

        assertThat(mvc.post().uri("/api/admin/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"nombre\": \"Soporte\", \"descripcion\": \"Atiende consultas\" }")
                .exchange())
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.nombre").isEqualTo("Soporte");
    }

    @Test
    void crear_nombreVacio_devuelve400SinLlamarAlService() {
        // E4Ad-HU08 criterio 4: "Este campo es obligatorio".
        assertThat(mvc.post().uri("/api/admin/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"nombre\": \"  \" }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.fieldErrors.nombre").asString().contains("obligatorio");

        verify(crearRolService, never()).crear(any(), any());
    }

    @Test
    void crear_nombreDuplicado_devuelve400ConElFieldError() {
        when(crearRolService.crear(any(), any())).thenThrow(new RolDuplicadoException());

        assertThat(mvc.post().uri("/api/admin/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"nombre\": \"Soporte\" }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.fieldErrors.nombre").asString().contains("Ya existe");
    }
}
