package com.activehub.usecases.actualizarpermisosrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.ValidacionException;
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

@WebMvcTest(controllers = ActualizarPermisosRolController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ActualizarPermisosRolControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ActualizarPermisosRolService actualizarPermisosRolService;

    private static final UUID ROL_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void actualizar_bodyValido_devuelve200ConLasClavesFinales() {
        when(actualizarPermisosRolService.actualizar(any(), any(), any()))
                .thenReturn(new ActualizarPermisosRolResponse(ROL_ID, "INSTRUCTOR", List.of("clases.gestionar")));

        assertThat(mvc.put().uri("/api/admin/roles/{id}/permisos", ROL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"permisos\": [\"clases.gestionar\"] }")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.permisos[0]").isEqualTo("clases.gestionar");
    }

    @Test
    void actualizar_listaVacia_esValida() {
        // Dejar un rol sin permisos es legítimo (salvo el Administrador, que lo valida el Service).
        when(actualizarPermisosRolService.actualizar(any(), any(), any()))
                .thenReturn(new ActualizarPermisosRolResponse(ROL_ID, "SOPORTE", List.of()));

        assertThat(mvc.put().uri("/api/admin/roles/{id}/permisos", ROL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"permisos\": [] }")
                .exchange())
                .hasStatus(200);
    }

    @Test
    void actualizar_sinCampoPermisos_devuelve400() {
        assertThat(mvc.put().uri("/api/admin/roles/{id}/permisos", ROL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");
    }

    @Test
    void actualizar_quitandoUnCriticoDelAdmin_devuelve400ConElMensajeDeLaSpec() {
        when(actualizarPermisosRolService.actualizar(any(), any(), any())).thenThrow(new ValidacionException(
                "Quitar este permiso dejaría al rol Administrador sin capacidad de gestión. "
                        + "Esta acción no está permitida."));

        assertThat(mvc.put().uri("/api/admin/roles/{id}/permisos", ROL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .principal(admin())
                .content("{ \"permisos\": [] }")
                .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message").asString().contains("sin capacidad de gestión");
    }
}
