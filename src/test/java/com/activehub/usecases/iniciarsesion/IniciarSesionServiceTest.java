package com.activehub.usecases.iniciarsesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.IntentosLoginService;
import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.UsuarioSuspendidoException;
import com.activehub.shared.security.JwtService;
import java.time.Instant;
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
class IniciarSesionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;

    @org.mockito.Mock private IntentosLoginService intentosLoginService;


    private IniciarSesionService service;

    @BeforeEach
    void setUp() {
        service = new IniciarSesionService(usuarioRepository, passwordEncoder, jwtService, auditService, intentosLoginService);
    }

    private Usuario usuarioActivo() {
        Rol rolAlumno = new Rol();
        rolAlumno.setNombre(RolNombre.ALUMNO.name());

        Usuario usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setApellido("Gómez");
        usuario.setEmail("martina@email.com");
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setRol(rolAlumno);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(usuario, "createdAt", Instant.now());
        return usuario;
    }

    @Test
    void login_credencialesValidas_devuelveTokenYUsuario_yAuditaLoginOk() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmailConRol("martina@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash-bcrypt")).thenReturn(true);
        when(jwtService.emitir(usuario.getId(), usuario.getEmail(), RolNombre.ALUMNO.name())).thenReturn("token-jwt");

        IniciarSesionResponse response = service.login(new IniciarSesionRequest("martina@email.com", "Password1"));

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.usuario().email()).isEqualTo("martina@email.com");
        verify(auditService).registrar(eq(usuario.getId()), eq(AuditAccion.LOGIN_OK), eq("Usuario"), eq(usuario.getId()), any());
    }

    @Test
    void login_passwordIncorrecta_lanzaCredencialesInvalidasException_yAuditaLoginFallido() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmailConRol("martina@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new IniciarSesionRequest("martina@email.com", "incorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(auditService).registrar(eq(usuario.getId()), eq(AuditAccion.LOGIN_FALLIDO), eq("Usuario"), eq(usuario.getId()), any());
    }

    @Test
    void login_emailInexistente_lanzaCredencialesInvalidasException_conMismoMensajeQuePasswordIncorrecta() {
        when(usuarioRepository.findByEmailConRol("no-existe@email.com")).thenReturn(Optional.empty());

        CredencialesInvalidasException exSinPassword = new CredencialesInvalidasException();

        assertThatThrownBy(() -> service.login(new IniciarSesionRequest("no-existe@email.com", "cualquiera")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage(exSinPassword.getMessage());
    }

    @Test
    void login_usuarioSuspendido_lanzaUsuarioSuspendidoException_noCredencialesInvalidas() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        when(usuarioRepository.findByEmailConRol("martina@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash-bcrypt")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new IniciarSesionRequest("martina@email.com", "Password1")))
                .isInstanceOf(UsuarioSuspendidoException.class);
    }

    // --- ingreso por DNI (nota de credenciales de la épica E1A) ----------------------

    @Test
    void login_conDni_buscaPorDniYNoPorEmail() {
        Usuario usuario = usuarioActivo();
        usuario.setDni("30123456");
        when(usuarioRepository.findByDniConRol("30123456")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash-bcrypt")).thenReturn(true);
        when(jwtService.emitir(usuario.getId(), usuario.getEmail(), RolNombre.ALUMNO.name())).thenReturn("token-jwt");

        IniciarSesionResponse response = service.login(new IniciarSesionRequest("30123456", "Password1"));

        assertThat(response.token()).isEqualTo("token-jwt");
        verify(usuarioRepository, never()).findByEmailConRol(any());
    }

    @Test
    void login_dniInexistente_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findByDniConRol("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new IniciarSesionRequest("99999999", "cualquiera")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void login_recortaEspaciosDelIdentificador() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmailConRol("martina@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password1", "hash-bcrypt")).thenReturn(true);
        when(jwtService.emitir(any(), any(), any())).thenReturn("token-jwt");

        assertThat(service.login(new IniciarSesionRequest("  martina@email.com  ", "Password1")).token())
                .isEqualTo("token-jwt");
    }
}
