package com.activehub.usecases.renovarsesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.UsuarioSuspendidoException;
import com.activehub.shared.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RenovarSesionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtService jwtService;

    @InjectMocks private RenovarSesionService service;

    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        Rol rol = new Rol();
        rol.setNombre(RolNombre.ALUMNO.name());

        usuario = new Usuario();
        usuario.setEmail("ana@activehub.test");
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    @Test
    void renovar_sesionActiva_emiteUnTokenNuevo() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(jwtService.emitir(usuarioId, "ana@activehub.test", RolNombre.ALUMNO.name())).thenReturn("token-nuevo");
        when(jwtService.getExpiracionMinutos()).thenReturn(30L);

        RenovarSesionResponse response = service.renovar(usuarioId);

        assertThat(response.token()).isEqualTo("token-nuevo");
        assertThat(response.expiraEnMinutos()).isEqualTo(30);
    }

    @Test
    void renovar_usuarioSuspendido_noEstiraLaSesion() {
        // Sin este chequeo, un usuario suspendido después de loguearse seguía renovando
        // indefinidamente el token que ya tenía.
        usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.renovar(usuarioId)).isInstanceOf(UsuarioSuspendidoException.class);

        verify(jwtService, never()).emitir(any(), any(), any());
    }

    @Test
    void renovar_usuarioInexistente_lanzaNoEncontrado() {
        // Una cuenta dada de baja deja de existir para el @SQLRestriction: la sesión muere ahí.
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renovar(usuarioId)).isInstanceOf(NoEncontradoException.class);
    }
}
