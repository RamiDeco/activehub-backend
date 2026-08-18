package com.activehub.usecases.actualizarestadousuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActualizarEstadoUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    private ActualizarEstadoUsuarioService service;
    private UUID usuarioId;
    private UUID actorId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new ActualizarEstadoUsuarioService(usuarioRepository, auditService);
        usuarioId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setEstado(EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    @Test
    void actualizar_suspende() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        ActualizarEstadoUsuarioResponse response =
                service.actualizar(usuarioId, new ActualizarEstadoUsuarioRequest("SUSPENDIDO"), actorId);

        assertThat(response.estado()).isEqualTo("SUSPENDIDO");
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
    }

    @Test
    void actualizar_reactiva() {
        usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        ActualizarEstadoUsuarioResponse response =
                service.actualizar(usuarioId, new ActualizarEstadoUsuarioRequest("ACTIVO"), actorId);

        assertThat(response.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void actualizar_propioUsuario_lanzaValidacion() {
        assertThatThrownBy(() -> service.actualizar(actorId, new ActualizarEstadoUsuarioRequest("SUSPENDIDO"), actorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void actualizar_estadoInvalido_lanzaValidacion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.actualizar(usuarioId, new ActualizarEstadoUsuarioRequest("NO_EXISTE"), actorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void actualizar_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(usuarioId, new ActualizarEstadoUsuarioRequest("SUSPENDIDO"), actorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
