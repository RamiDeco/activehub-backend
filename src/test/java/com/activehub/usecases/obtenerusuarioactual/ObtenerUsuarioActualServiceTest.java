package com.activehub.usecases.obtenerusuarioactual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * `/api/auth/me` es de donde el frontend saca los permisos con los que arma el menú y las
 * rutas (RN-19): si esta respuesta se queda corta, la sesión entera queda mal.
 */
@ExtendWith(MockitoExtension.class)
class ObtenerUsuarioActualServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilAlumnoRepository perfilAlumnoRepository;
    @Mock
    private ConfiguracionRolRepository configuracionRolRepository;

    private ObtenerUsuarioActualService service;
    private UUID usuarioId;
    private UUID rolId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new ObtenerUsuarioActualService(
                usuarioRepository, perfilAlumnoRepository, configuracionRolRepository);

        rolId = UUID.randomUUID();
        Rol rol = new Rol();
        rol.setNombre("ALUMNO");
        ReflectionTestUtils.setField(rol, "id", rolId);

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Lucía");
        usuario.setApellido("Paz");
        usuario.setEmail("lucia@test.com");
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private static ConfiguracionRol configuracion(String clave, boolean habilitado) {
        Permiso permiso = new Permiso();
        permiso.setClave(clave);
        ConfiguracionRol c = new ConfiguracionRol();
        c.setPermiso(permiso);
        c.setHabilitado(habilitado);
        return c;
    }

    @Test
    void obtener_devuelveSoloLosPermisosHabilitados() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(configuracionRolRepository.findByRolId(rolId)).thenReturn(List.of(
                configuracion("catalogo.explorar", true),
                configuracion("inscripciones.gestionar", true),
                // Destildado en la pantalla de HU08: no puede viajar en la respuesta, porque
                // el frontend le mostraría el módulo y el backend después se lo rechaza.
                configuracion("usuarios.gestionar", false)));

        ObtenerUsuarioActualResponse response = service.obtener(usuarioId);

        assertThat(response.permisos()).containsExactlyInAnyOrder("catalogo.explorar", "inscripciones.gestionar");
        assertThat(response.rol()).isEqualTo("ALUMNO");
        assertThat(response.email()).isEqualTo("lucia@test.com");
    }

    @Test
    void obtener_sinPerfilDeAlumno_devuelveInteresesVaciosYNoNull() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(configuracionRolRepository.findByRolId(rolId)).thenReturn(List.of());

        ObtenerUsuarioActualResponse response = service.obtener(usuarioId);

        assertThat(response.intereses()).isNotNull().isEmpty();
    }

    @Test
    void obtener_conPerfilDeAlumno_devuelveLosInteresesConSuCategoria() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Aventura");
        ReflectionTestUtils.setField(categoria, "id", UUID.randomUUID());

        TipoActividad trekking = new TipoActividad();
        trekking.setNombre("Trekking");
        trekking.setCategoria(categoria);
        ReflectionTestUtils.setField(trekking, "id", UUID.randomUUID());

        PerfilAlumno perfil = new PerfilAlumno(usuario, List.of(trekking));

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(configuracionRolRepository.findByRolId(rolId)).thenReturn(List.of());

        ObtenerUsuarioActualResponse response = service.obtener(usuarioId);

        assertThat(response.intereses()).hasSize(1);
        assertThat(response.intereses().get(0).nombre()).isEqualTo("Trekking");
        assertThat(response.intereses().get(0).categoria()).isEqualTo("Aventura");
    }

    @Test
    void obtener_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(usuarioId)).isInstanceOf(NoEncontradoException.class);
    }
}
