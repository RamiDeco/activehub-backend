package com.activehub.usecases.listarrolespermisos;

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

@WebMvcTest(controllers = ListarRolesPermisosController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarRolesPermisosControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarRolesPermisosService listarRolesPermisosService;

    @Test
    void listar_devuelve200ConLaMatriz() {
        var rol = new ListarRolesPermisosResponse.Rol(
                UUID.randomUUID(), "ADMIN", "Gobierna la plataforma", true, 3, List.of("roles.configurar"));
        var permiso = new ListarRolesPermisosResponse.Permiso(
                UUID.randomUUID(), "roles.configurar", "Roles", "Configurar roles y permisos", true);
        when(listarRolesPermisosService.listar())
                .thenReturn(new ListarRolesPermisosResponse(List.of(rol), List.of(permiso)));

        assertThat(mvc.get().uri("/api/admin/roles")
                .principal(new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of()))
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.roles[0].permisos[0]").isEqualTo("roles.configurar");
    }

    @Test
    void listar_incluyeLaCriticidadDeCadaPermiso() {
        var permiso = new ListarRolesPermisosResponse.Permiso(
                UUID.randomUUID(), "usuarios.gestionar", "Usuarios", "Ver, editar y suspender cuentas", true);
        when(listarRolesPermisosService.listar())
                .thenReturn(new ListarRolesPermisosResponse(List.of(), List.of(permiso)));

        assertThat(mvc.get().uri("/api/admin/roles")
                .principal(new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of()))
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.permisos[0].critico").isEqualTo(true);
    }
}
