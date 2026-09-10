package com.activehub.usecases.registraradmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.DniEnUsoException;
import com.activehub.shared.error.EmailEnUsoException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Alta de otro administrador, hecha por un admin logueado. Es la única alta que no es
 * pública: el actor queda en la auditoría porque crear un ADMIN es una acción sensible.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrarAdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    private RegistrarAdminService service;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new RegistrarAdminService(usuarioRepository, rolRepository, passwordEncoder, auditService);
        actorId = UUID.randomUUID();

        Rol rolAdmin = new Rol();
        rolAdmin.setNombre(RolNombre.ADMIN.name());
        when(rolRepository.findByNombre(RolNombre.ADMIN)).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(u, "createdAt", Instant.now());
            return u;
        });
    }

    private RegistrarAdminRequest request() {
        return new RegistrarAdminRequest("Ana", "Pérez", "ana@activehub.test", "2611234567", null, "Password1");
    }

    @Test
    void registrar_datosValidos_creaAdminActivoYAudita() {
        RegistrarAdminResponse response = service.registrar(request(), actorId);

        assertThat(response.rol()).isEqualTo("ADMIN");
        assertThat(response.estado()).isEqualTo(EstadoUsuario.ACTIVO.name());
        assertThat(response.email()).isEqualTo("ana@activehub.test");
        verify(auditService).registrar(eq(actorId), eq(AuditAccion.ADMIN_CREADO), eq("Usuario"), any(), any());
    }

    @Test
    void registrar_guardaLaContraseniaHasheada() {
        service.registrar(request(), actorId);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash-bcrypt");
        verify(passwordEncoder).encode("Password1");
    }

    @Test
    void registrar_normalizaElEmailAMinusculas() {
        var mayus = new RegistrarAdminRequest("Ana", "Pérez", " ANA@ActiveHub.Test ", null, null, "Password1");

        // El login busca con lower(): guardarlo con mayúsculas dejaba al admin sin poder entrar.
        assertThat(service.registrar(mayus, actorId).email()).isEqualTo("ana@activehub.test");
    }

    @Test
    void registrar_dniVacio_seGuardaComoNull() {
        // El DNI es opcional y tiene índice único parcial: guardar "" dejaría una segunda
        // alta sin DNI chocando contra la primera.
        var sinDni = new RegistrarAdminRequest("Ana", "Pérez", "ana@activehub.test", null, "  ", "Password1");

        service.registrar(sinDni, actorId);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDni()).isNull();
    }

    @Test
    void registrar_emailEnUso_lanzaEmailEnUso() {
        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("ana@activehub.test")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request(), actorId))
                .isInstanceOf(EmailEnUsoException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_dniEnUso_lanzaDniEnUso() {
        var conDni = new RegistrarAdminRequest("Ana", "Pérez", "ana@activehub.test", null, "30123456", "Password1");
        when(usuarioRepository.existsByDniAndDeletedFalse("30123456")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(conDni, actorId))
                .isInstanceOf(DniEnUsoException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }
}
