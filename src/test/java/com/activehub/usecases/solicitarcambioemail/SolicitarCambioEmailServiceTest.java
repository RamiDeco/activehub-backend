package com.activehub.usecases.solicitarcambioemail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SolicitarCambioEmailServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificacionEmailService verificacionEmailService;

    private SolicitarCambioEmailService service;
    private Usuario usuario;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new SolicitarCambioEmailService(usuarioRepository, passwordEncoder, verificacionEmailService);

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setEmail("martina@email.com");
        usuario.setPasswordHash("hash");
        usuario.setEmailVerificado(true);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    @Test
    void solicitar_mandaElCodigoAlNUEVO_ySinTocarElCorreoDeLaCuenta() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash")).thenReturn(true);
        when(usuarioRepository.existsVerificadoConEmail("nuevo@email.com", usuarioId)).thenReturn(false);
        when(verificacionEmailService.reemitir(usuario, "nuevo@email.com", PropositoVerificacion.CAMBIO_EMAIL))
                .thenReturn(true);
        when(verificacionEmailService.ttlMin()).thenReturn(15);

        var respuesta = service.solicitar(usuarioId, new SolicitarCambioEmailRequest("Nuevo@Email.com ", "Password1"));

        assertThat(respuesta.email()).isEqualTo("nuevo@email.com");
        assertThat(respuesta.enviado()).isTrue();
        // La cuenta sigue con el correo viejo, que además sigue reservado: el cambio lo aplica
        // `verificaremail` cuando se ingresa el código.
        assertThat(usuario.getEmail()).isEqualTo("martina@email.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void solicitar_sinLaContraseniaActual_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

        // Cambiar el correo es cambiar la credencial de acceso: con la sesión abierta en una
        // máquina ajena, sin contraseña alcanzaría un click para quedarse con la cuenta.
        assertThatThrownBy(() -> service.solicitar(usuarioId, new SolicitarCambioEmailRequest("n@e.com", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(verificacionEmailService, never()).reemitir(any(), any(), any());
    }

    @Test
    void solicitar_correoYaVerificadoPorOtro_lanzaEmailEnUso() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash")).thenReturn(true);
        when(usuarioRepository.existsVerificadoConEmail("tomado@email.com", usuarioId)).thenReturn(true);

        assertThatThrownBy(() ->
                service.solicitar(usuarioId, new SolicitarCambioEmailRequest("tomado@email.com", "Password1")))
                .isInstanceOf(EmailEnUsoException.class);
    }

    @Test
    void solicitar_cuentaDeGoogle_noAplica() {
        usuario.setAuthProveedor(AuthProveedor.GOOGLE);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // No tiene contraseña que el usuario conozca: su hash es aleatorio. Pedirsela seria
        // pedirle algo que no existe.
        assertThatThrownBy(() ->
                service.solicitar(usuarioId, new SolicitarCambioEmailRequest("n@e.com", "loquesea")))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Google");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void solicitar_elMismoCorreoQueYaTiene_lanzaValidacion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash")).thenReturn(true);

        assertThatThrownBy(() ->
                service.solicitar(usuarioId, new SolicitarCambioEmailRequest("martina@email.com", "Password1")))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya es tu correo actual");
    }
}
