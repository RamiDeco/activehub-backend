package com.activehub.usecases.actualizarmiperfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
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
class ActualizarMiPerfilServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ActualizarMiPerfilService service;

    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setApellido("Lopez");
        usuario.setEmail("martina@email.com");
        usuario.setTelefono("2611234567");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private ActualizarMiPerfilRequest req(String email) {
        return new ActualizarMiPerfilRequest("Martina", "Gimenez", email, "2617654321", LocalDate.of(1995, 5, 1));
    }

    @Test
    void actualizar_datosValidos_persisteYAudita() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        ActualizarMiPerfilResponse response = service.actualizar(usuarioId, req("martina@email.com"));

        assertThat(response.apellido()).isEqualTo("Gimenez");
        assertThat(usuario.getTelefono()).isEqualTo("2617654321");
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void actualizar_emailNuevoLibre_loCambia() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("nuevo@email.com")).thenReturn(false);

        service.actualizar(usuarioId, req("Nuevo@Email.com"));

        assertThat(usuario.getEmail()).isEqualTo("nuevo@email.com");
    }

    @Test
    void actualizar_emailDeOtraCuenta_lanzaEmailEnUso() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("ocupado@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(usuarioId, req("ocupado@email.com")))
                .isInstanceOf(EmailEnUsoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_mismoEmailPropio_noChequeaDuplicado() {
        // Guardar sin tocar el correo no debe chocar contra uno mismo.
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.actualizar(usuarioId, req("MARTINA@email.com"));

        verify(usuarioRepository, never()).existsByEmailIgnoreCaseAndDeletedFalse(any());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizar_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(usuarioId, req("x@email.com")))
                .isInstanceOf(NoEncontradoException.class);
    }
}
