package com.activehub.usecases.restablecerpassword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.domain.usuario.VerificacionEmail;
import com.activehub.domain.usuario.VerificacionEmailRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.ValidacionException;
import java.time.Instant;
import java.util.List;
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
class RestablecerPasswordServiceTest {

    private static final String CODIGO = "123456";
    private static final String NUEVA = "Segura123";
    private static final String ERROR_GENERICO = "El código no es correcto o ya venció. Pedí uno nuevo.";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VerificacionEmailRepository verificacionRepository;
    @Mock
    private VerificacionEmailService verificacionEmailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private RestablecerPasswordService service;
    private Usuario usuario;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new RestablecerPasswordService(
                usuarioRepository, verificacionRepository, verificacionEmailService, passwordEncoder, auditService);

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setEmail("martina@email.com");
        usuario.setEmailVerificado(true);
        usuario.setAuthProveedor(AuthProveedor.LOCAL);
        usuario.setPasswordHash("hash-viejo");
        Rol rol = new Rol();
        rol.setNombre("ALUMNO");
        usuario.setRol(rol);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private RestablecerPasswordRequest pedido() {
        return new RestablecerPasswordRequest("martina@email.com", CODIGO, NUEVA);
    }

    private VerificacionEmail verificacion(PropositoVerificacion proposito) {
        return new VerificacionEmail(
                usuario, "martina@email.com", "hash-codigo", proposito, Instant.parse("2026-03-10T12:00:00Z"));
    }

    private void hayCuenta() {
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of(usuario));
    }

    @Test
    void restablecer_codigoCorrecto_guardaLaContraseniaNuevaYAudita() {
        hayCuenta();
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId))
                .thenReturn(Optional.of(verificacion(PropositoVerificacion.RECUPERACION_PASSWORD)));
        when(verificacionEmailService.validar(usuarioId, CODIGO))
                .thenReturn(verificacion(PropositoVerificacion.RECUPERACION_PASSWORD));
        when(passwordEncoder.matches(NUEVA, "hash-viejo")).thenReturn(false);
        when(passwordEncoder.encode(NUEVA)).thenReturn("hash-nuevo");

        RestablecerPasswordResponse respuesta = service.restablecer(pedido());

        assertThat(respuesta.email()).isEqualTo("martina@email.com");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.PASSWORD_RESTABLECIDA), eq("Usuario"), eq(usuarioId), isNull());
    }

    /**
     * El propósito se mira ANTES de validar: si no, un código de alta o de cambio de correo se
     * consumiría acá y el usuario lo perdería sin haberlo usado para nada.
     */
    @Test
    void restablecer_ultimoCodigoDeOtroProposito_noLoConsumeYRechaza() {
        hayCuenta();
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId))
                .thenReturn(Optional.of(verificacion(PropositoVerificacion.CAMBIO_EMAIL)));

        assertThatThrownBy(() -> service.restablecer(pedido()))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(ERROR_GENERICO);

        verify(verificacionEmailService, never()).validar(any(), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void restablecer_sinNingunCodigoPendiente_rechaza() {
        hayCuenta();
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecer(pedido()))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(ERROR_GENERICO);
    }

    /**
     * Mismo mensaje que un código equivocado: si acá el error delatara que la cuenta no existe,
     * no habría servido de nada ocultarlo en el paso anterior.
     */
    @Test
    void restablecer_correoInexistente_daElMismoErrorQueUnCodigoInvalido() {
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of());

        assertThatThrownBy(() -> service.restablecer(pedido()))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(ERROR_GENERICO);
    }

    @Test
    void restablecer_cuentaDeGoogle_daElMismoErrorQueUnCodigoInvalido() {
        usuario.setAuthProveedor(AuthProveedor.GOOGLE);
        hayCuenta();

        assertThatThrownBy(() -> service.restablecer(pedido()))
                .isInstanceOf(ValidacionException.class)
                .hasMessage(ERROR_GENERICO);
    }

    @Test
    void restablecer_mismaContraseniaQueLaActual_laRechaza() {
        hayCuenta();
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId))
                .thenReturn(Optional.of(verificacion(PropositoVerificacion.RECUPERACION_PASSWORD)));
        when(verificacionEmailService.validar(usuarioId, CODIGO))
                .thenReturn(verificacion(PropositoVerificacion.RECUPERACION_PASSWORD));
        when(passwordEncoder.matches(NUEVA, "hash-viejo")).thenReturn(true);

        assertThatThrownBy(() -> service.restablecer(pedido()))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("La contraseña nueva tiene que ser distinta de la actual.");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-viejo");
        verify(usuarioRepository, never()).save(any());
    }

    /**
     * El código equivocado lo rechaza VerificacionEmailService con su propio mensaje (que
     * cuenta los intentos restantes); acá se verifica que este usecase no lo tape.
     */
    @Test
    void restablecer_codigoInvalido_propagaElErrorDeLaValidacion() {
        hayCuenta();
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId))
                .thenReturn(Optional.of(verificacion(PropositoVerificacion.RECUPERACION_PASSWORD)));
        when(verificacionEmailService.validar(usuarioId, CODIGO))
                .thenThrow(new ValidacionException("El código no es correcto. Te quedan 4 intentos."));

        assertThatThrownBy(() -> service.restablecer(pedido()))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("El código no es correcto. Te quedan 4 intentos.");

        verify(usuarioRepository, never()).save(any());
    }
}
