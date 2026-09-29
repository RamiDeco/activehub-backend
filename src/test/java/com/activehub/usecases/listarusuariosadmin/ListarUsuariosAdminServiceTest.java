package com.activehub.usecases.listarusuariosadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarUsuariosAdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConfiguracionRolRepository configuracionRolRepository;

    private ListarUsuariosAdminService service;

    @BeforeEach
    void setUp() {
        service = new ListarUsuariosAdminService(usuarioRepository, configuracionRolRepository);
    }

    private Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        ReflectionTestUtils.setField(rol, "id", UUID.randomUUID());
        return rol;
    }

    @Test
    void listar_devuelveUsuariosMapeados() {
        Rol rolAlumno = rol(RolNombre.ALUMNO.name());

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Lopez");
        usuario.setEmail("ana@example.com");
        usuario.setTelefono("111");
        usuario.setRol(rolAlumno);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setCantidadPenalizaciones(2);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());

        when(usuarioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(usuario));
        when(configuracionRolRepository.rolesConPermiso("clases.gestionar")).thenReturn(List.of());

        List<ListarUsuariosAdminResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Ana");
        assertThat(resultado.get(0).rol()).isEqualTo("ALUMNO");
        assertThat(resultado.get(0).estado()).isEqualTo("ACTIVO");
        assertThat(resultado.get(0).cantidadPenalizaciones()).isEqualTo(2);
        assertThat(resultado.get(0).puedeDarClases()).isFalse();
    }

    @Test
    void listar_marcaPuedeDarClasesSegunElPermisoDelRol() {
        // Solo a un instructor se le puede aplicar una penalizacion (E4Ad-HU06 / RN-13), y
        // "es instructor" se decide por el permiso `clases.gestionar`, no por el nombre del
        // rol (RN-19): un rol nuevo que dicte clases tambien es penalizable.
        Rol rolInstructor = rol(RolNombre.INSTRUCTOR.name());

        Usuario usuario = new Usuario();
        usuario.setNombre("Beto");
        usuario.setApellido("Diaz");
        usuario.setEmail("beto@example.com");
        usuario.setRol(rolInstructor);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());

        when(usuarioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(usuario));
        when(configuracionRolRepository.rolesConPermiso("clases.gestionar"))
                .thenReturn(List.of(rolInstructor.getId()));

        assertThat(service.listar().get(0).puedeDarClases()).isTrue();
    }
}
