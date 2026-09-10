package com.activehub.usecases.listarrolespermisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarRolesPermisosServiceTest {

    @Mock private RolRepository rolRepository;
    @Mock private PermisoRepository permisoRepository;
    @Mock private ConfiguracionRolRepository configuracionRolRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private ListarRolesPermisosService service;

    private Rol rolAlumno;
    private Rol rolNuevo;
    private Permiso explorar;
    private Permiso configurarRoles;

    @BeforeEach
    void setUp() {
        service = new ListarRolesPermisosService(
                rolRepository, permisoRepository, configuracionRolRepository, usuarioRepository);

        rolAlumno = rol(RolNombre.ALUMNO.name(), true);
        rolNuevo = rol("SOPORTE", false);
        explorar = permiso("catalogo.explorar", 10, false);
        configurarRoles = permiso("roles.configurar", 150, true);

        when(rolRepository.findAllByOrderBySistemaDescNombreAsc()).thenReturn(List.of(rolAlumno, rolNuevo));
        when(permisoRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of(explorar, configurarRoles));
        when(usuarioRepository.countByRolIdAndDeletedFalse(rolAlumno.getId())).thenReturn(42L);
        when(usuarioRepository.countByRolIdAndDeletedFalse(rolNuevo.getId())).thenReturn(0L);
        when(configuracionRolRepository.findAllConDetalle()).thenReturn(List.of(
                config(rolAlumno, explorar, true),
                config(rolAlumno, configurarRoles, false),
                config(rolNuevo, explorar, false),
                config(rolNuevo, configurarRoles, false)));
    }

    private Rol rol(String nombre, boolean sistema) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setSistema(sistema);
        rol.setDescripcion("Descripción de " + nombre);
        ReflectionTestUtils.setField(rol, "id", UUID.randomUUID());
        return rol;
    }

    private Permiso permiso(String clave, int orden, boolean critico) {
        Permiso permiso = new Permiso();
        permiso.setClave(clave);
        permiso.setModulo("Módulo");
        permiso.setAccion("Acción");
        permiso.setOrden(orden);
        permiso.setCritico(critico);
        ReflectionTestUtils.setField(permiso, "id", UUID.randomUUID());
        return permiso;
    }

    private ConfiguracionRol config(Rol rol, Permiso permiso, boolean habilitado) {
        ConfiguracionRol config = new ConfiguracionRol();
        config.setRol(rol);
        config.setPermiso(permiso);
        config.setHabilitado(habilitado);
        return config;
    }

    @Test
    void listar_devuelveCadaRolConSusClavesHabilitadas() {
        var response = service.listar();

        assertThat(response.roles()).hasSize(2);
        assertThat(response.roles().get(0).permisos()).containsExactly("catalogo.explorar");
        assertThat(response.roles().get(0).usuarios()).isEqualTo(42);
    }

    @Test
    void listar_rolSinPermisosHabilitados_devuelveListaVacia() {
        // Un rol recién creado tiene todas sus filas en false: la matriz lo muestra sin
        // ticks, no como "sin datos" (E4Ad-HU08 criterio 3).
        var response = service.listar();

        var soporte = response.roles().stream().filter(r -> r.nombre().equals("SOPORTE")).findFirst().orElseThrow();
        assertThat(soporte.permisos()).isEmpty();
        assertThat(soporte.sistema()).isFalse();
        assertThat(soporte.usuarios()).isZero();
    }

    @Test
    void listar_devuelveElCatalogoDePermisosConSuCriticidad() {
        var response = service.listar();

        assertThat(response.permisos()).extracting(ListarRolesPermisosResponse.Permiso::clave)
                .containsExactly("catalogo.explorar", "roles.configurar");
        assertThat(response.permisos().get(1).critico()).isTrue();
    }
}
