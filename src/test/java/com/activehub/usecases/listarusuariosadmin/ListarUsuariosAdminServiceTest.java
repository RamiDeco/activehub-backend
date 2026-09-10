package com.activehub.usecases.listarusuariosadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    private ListarUsuariosAdminService service;

    @BeforeEach
    void setUp() {
        service = new ListarUsuariosAdminService(usuarioRepository);
    }

    @Test
    void listar_devuelveUsuariosMapeados() {
        Rol rolAlumno = new Rol();
        rolAlumno.setNombre(RolNombre.ALUMNO.name());

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

        List<ListarUsuariosAdminResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Ana");
        assertThat(resultado.get(0).rol()).isEqualTo("ALUMNO");
        assertThat(resultado.get(0).estado()).isEqualTo("ACTIVO");
        assertThat(resultado.get(0).cantidadPenalizaciones()).isEqualTo(2);
    }
}
