package com.activehub.usecases.verificaremail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.domain.usuario.VerificacionEmail;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.EmailEnUsoException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerificarEmailServiceTest {

    private static final Instant AHORA = Instant.parse("2026-03-10T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VerificacionEmailService verificacionEmailService;
    @Mock
    private AuditService auditService;
    @Mock
    private com.activehub.shared.security.JwtService jwtService;

    private VerificarEmailService service;
    private Usuario usuario;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new VerificarEmailService(
                usuarioRepository, verificacionEmailService, auditService, jwtService,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setEmail("martina@email.com");
        // El service emite un token nuevo al confirmar, y para eso necesita el rol.
        var rol = new com.activehub.domain.usuario.Rol();
        rol.setNombre("ALUMNO");
        usuario.setRol(rol);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private VerificacionEmail verificacion(String email, PropositoVerificacion proposito) {
        return new VerificacionEmail(usuario, email, "hash", proposito, AHORA.plusSeconds(600));
    }

    @Test
    void verificar_alta_marcaElCorreoComoVerificado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(verificacionEmailService.validar(usuarioId, "123456"))
                .thenReturn(verificacion("martina@email.com", PropositoVerificacion.REGISTRO));
        when(usuarioRepository.existsVerificadoConEmail("martina@email.com", usuarioId)).thenReturn(false);

        VerificarEmailResponse respuesta = service.verificar(usuarioId, new VerificarEmailRequest("123456"));

        assertThat(respuesta.emailVerificado()).isTrue();
        assertThat(respuesta.cambioDeEmail()).isFalse();
        assertThat(usuario.isEmailVerificado()).isTrue();
        assertThat(usuario.getEmailVerificadoAt()).isEqualTo(AHORA);
        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.EMAIL_VERIFICADO), eq("Usuario"), eq(usuarioId), eq("martina@email.com"));
    }

    @Test
    void verificar_cambioDeEmail_reemplazaElCorreoDeLaCuenta() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(verificacionEmailService.validar(usuarioId, "123456"))
                .thenReturn(verificacion("nuevo@email.com", PropositoVerificacion.CAMBIO_EMAIL));
        when(usuarioRepository.existsVerificadoConEmail("nuevo@email.com", usuarioId)).thenReturn(false);

        VerificarEmailResponse respuesta = service.verificar(usuarioId, new VerificarEmailRequest("123456"));

        // Recién acá cambia el correo de la cuenta: `solicitarcambioemail` no lo toca, para que
        // un error de tipeo en el nuevo no deje al usuario sin el viejo.
        assertThat(usuario.getEmail()).isEqualTo("nuevo@email.com");
        assertThat(respuesta.cambioDeEmail()).isTrue();
        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.EMAIL_CAMBIADO), eq("Usuario"), eq(usuarioId), eq("nuevo@email.com"));
    }

    /**
     * La carrera que habilita la regla pedida: dos cuentas sin verificar pueden sostener
     * códigos para la misma dirección, y gana la primera que lo ingresa. La segunda tiene que
     * recibir un 409 claro, no un error de base de datos por el índice único.
     */
    @Test
    void verificar_siOtroYaVerificoEseCorreo_lanzaEmailEnUso() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(verificacionEmailService.validar(usuarioId, "123456"))
                .thenReturn(verificacion("martina@email.com", PropositoVerificacion.REGISTRO));
        when(usuarioRepository.existsVerificadoConEmail("martina@email.com", usuarioId)).thenReturn(true);

        assertThatThrownBy(() -> service.verificar(usuarioId, new VerificarEmailRequest("123456")))
                .isInstanceOf(EmailEnUsoException.class);

        assertThat(usuario.isEmailVerificado()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }
}
