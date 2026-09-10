package com.activehub.usecases.cambiarmicontrasenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CambiarMiContraseniaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks private CambiarMiContraseniaService service;

    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setPasswordHash("hash-viejo");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private CambiarMiContraseniaRequest req() {
        return new CambiarMiContraseniaRequest("Actual123", "Nueva456");
    }

    @Test
    void cambiar_conContraseniaActualCorrecta_guardaElHashNuevo() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Actual123", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.matches("Nueva456", "hash-viejo")).thenReturn(false);
        when(passwordEncoder.encode("Nueva456")).thenReturn("hash-nuevo");

        service.cambiar(usuarioId, req());

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void cambiar_contraseniaActualIncorrecta_lanzaValidacion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Actual123", "hash-viejo")).thenReturn(false);

        assertThatThrownBy(() -> service.cambiar(usuarioId, req()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("actual");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-viejo");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiar_nuevaIgualALaActual_lanzaValidacion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Actual123", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.matches("Nueva456", "hash-viejo")).thenReturn(true);

        assertThatThrownBy(() -> service.cambiar(usuarioId, req()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("distinta");

        verify(usuarioRepository, never()).save(any());
    }
}
