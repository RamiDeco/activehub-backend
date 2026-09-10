package com.activehub.usecases.actualizarusuarioadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import java.time.LocalDate;
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
class ActualizarUsuarioAdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ActualizarUsuarioAdminService service;

    private UUID usuarioId;
    private UUID actorId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setEmail("ana@activehub.test");
        usuario.setTelefono("2611111111");
    }

    @Test
    void actualizar_guardaDatosNormalizadosYAudita() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        ActualizarUsuarioAdminResponse response = service.actualizar(
                usuarioId,
                new ActualizarUsuarioAdminRequest(
                        "  Ana Laura  ", "  Gomez  ", "  ANA.GOMEZ@Activehub.test ", " 2612222222 ",
                        LocalDate.of(1995, 3, 14)),
                actorId);

        assertThat(response.nombre()).isEqualTo("Ana Laura");
        assertThat(response.apellido()).isEqualTo("Gomez");
        // El email se normaliza a minúsculas: el login busca con lower(), si se guardara
        // con mayúsculas el usuario editado no podría volver a entrar por su propio mail.
        assertThat(response.email()).isEqualTo("ana.gomez@activehub.test");
        assertThat(response.telefono()).isEqualTo("2612222222");
        assertThat(response.fechaNacimiento()).isEqualTo(LocalDate.of(1995, 3, 14));

        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(actorId, AuditAccion.USUARIO_ACTUALIZADO, "Usuario", usuarioId, null);
    }

    @Test
    void actualizar_mismoEmailDelUsuario_noLoTomaComoEnUso() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Cambiar solo el nombre no puede fallar por "email en uso" contra sí mismo.
        service.actualizar(
                usuarioId,
                new ActualizarUsuarioAdminRequest("Ana", "Perez", "ANA@activehub.test", "2611111111", null),
                actorId);

        verify(usuarioRepository, never()).existsByEmailIgnoreCaseAndDeletedFalse(any());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizar_emailDeOtroUsuario_lanzaEmailEnUso() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("otro@activehub.test")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(
                usuarioId,
                new ActualizarUsuarioAdminRequest("Ana", "Perez", "otro@activehub.test", "2611111111", null),
                actorId))
                .isInstanceOf(EmailEnUsoException.class);

        verify(usuarioRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void actualizar_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                usuarioId,
                new ActualizarUsuarioAdminRequest("Ana", "Perez", "ana@activehub.test", "2611111111", null),
                actorId))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void actualizar_telefonoNulo_loDejaEnNull() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        ActualizarUsuarioAdminResponse response = service.actualizar(
                usuarioId,
                new ActualizarUsuarioAdminRequest("Ana", "Perez", "ana@activehub.test", null, null),
                actorId);

        assertThat(response.telefono()).isNull();
    }
}
