package com.activehub.usecases.registraralumno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.security.JwtService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrarAlumnoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PerfilAlumnoRepository perfilAlumnoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;

    private RegistrarAlumnoService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarAlumnoService(
                usuarioRepository, rolRepository, perfilAlumnoRepository, passwordEncoder, jwtService, auditService);
    }

    private RegistrarAlumnoRequest requestValido() {
        return new RegistrarAlumnoRequest(
                "Martina", "Gómez", "martina@email.com", "2611234567", "Password1",
                LocalDate.of(2000, 5, 10), List.of("Running", "Yoga"), null, true);
    }

    @Test
    void registrarAlumno_datosValidos_creaUsuarioYPerfilAlumno_devuelveTokenYUsuario() {
        Rol rolAlumno = new Rol();
        rolAlumno.setNombre(RolNombre.ALUMNO);

        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("martina@email.com")).thenReturn(false);
        when(rolRepository.findByNombre(RolNombre.ALUMNO)).thenReturn(Optional.of(rolAlumno));
        when(passwordEncoder.encode("Password1")).thenReturn("hash-bcrypt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(u, "createdAt", Instant.now());
            return u;
        });
        when(jwtService.emitir(any(UUID.class), anyString(), eq(RolNombre.ALUMNO))).thenReturn("token-jwt");

        RegistrarAlumnoResponse response = service.registrar(requestValido());

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.usuario().email()).isEqualTo("martina@email.com");
        assertThat(response.usuario().rol()).isEqualTo("ALUMNO");
        assertThat(response.usuario().estado()).isEqualTo("ACTIVO");

        verify(usuarioRepository).saveAndFlush(any(Usuario.class));
        verify(perfilAlumnoRepository).save(any());
        verify(auditService).registrar(any(UUID.class), eq(AuditAccion.REGISTRO_ALUMNO), eq("Usuario"), any(UUID.class), isNull());
    }

    @Test
    void registrarAlumno_conCondicionSalud_laGuardaEnElPerfil() {
        Rol rolAlumno = new Rol();
        rolAlumno.setNombre(RolNombre.ALUMNO);

        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("martina@email.com")).thenReturn(false);
        when(rolRepository.findByNombre(RolNombre.ALUMNO)).thenReturn(Optional.of(rolAlumno));
        when(passwordEncoder.encode("Password1")).thenReturn("hash-bcrypt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(u, "createdAt", Instant.now());
            return u;
        });
        when(jwtService.emitir(any(UUID.class), anyString(), eq(RolNombre.ALUMNO))).thenReturn("token-jwt");

        RegistrarAlumnoRequest request = new RegistrarAlumnoRequest(
                "Martina", "Gómez", "martina@email.com", "2611234567", "Password1",
                LocalDate.of(2000, 5, 10), List.of("Running"), "Asma leve, evitar esfuerzo prolongado.", true);

        service.registrar(request);

        ArgumentCaptor<PerfilAlumno> captor = ArgumentCaptor.forClass(PerfilAlumno.class);
        verify(perfilAlumnoRepository).save(captor.capture());
        assertThat(captor.getValue().getCondicionSalud()).isEqualTo("Asma leve, evitar esfuerzo prolongado.");
    }

    @Test
    void registrarAlumno_emailYaRegistrado_lanzaEmailEnUsoException() {
        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("martina@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(requestValido()))
                .isInstanceOf(EmailEnUsoException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(perfilAlumnoRepository, never()).save(any());
    }
}
