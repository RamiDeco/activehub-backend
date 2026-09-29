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
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
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
        return new ActualizarMiPerfilRequest(
                "Martina", "Gimenez", email, "2617654321", LocalDate.of(1995, 5, 1), null);
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

    /**
     * Antes este endpoint cambiaba el correo directamente, y desde V26 eso es un agujero: el
     * correo es la credencial verificada y lo que <b>reserva</b> la dirección, así que
     * cambiarlo sin confirmar dejaría tomar la casilla de cualquiera con un PUT. El cambio
     * real vive en {@code solicitarcambioemail} + {@code verificaremail}.
     */
    @Test
    void actualizar_conOtroEmail_loRECHAZA_porqueElCorreoSeCambiaConCodigo() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.actualizar(usuarioId, req("Nuevo@Email.com")))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("código");

        assertThat(usuario.getEmail()).isEqualTo("martina@email.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_mismoEmailPropio_noChequeaDuplicado() {
        // Guardar sin tocar el correo no debe chocar contra uno mismo.
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.actualizar(usuarioId, req("MARTINA@email.com"));

        verify(usuarioRepository).save(usuario);
    }

    /**
     * El DNI es la unica forma de cargarlo despues del alta: quien se registro con Google
     * nunca paso por un formulario que lo pidiera.
     */
    @Test
    void actualizar_conDniLibre_loGuarda() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByDniAndIdNotAndDeletedFalse("30123456", usuarioId)).thenReturn(false);

        service.actualizar(usuarioId, new ActualizarMiPerfilRequest(
                "Martina", "Gimenez", "martina@email.com", "2617654321", LocalDate.of(1995, 5, 1), "30123456"));

        assertThat(usuario.getDni()).isEqualTo("30123456");
    }

    @Test
    void actualizar_conDniDeOtraCuenta_lanzaDniEnUso() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByDniAndIdNotAndDeletedFalse("30123456", usuarioId)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(usuarioId, new ActualizarMiPerfilRequest(
                "Martina", "Gimenez", "martina@email.com", "2617654321", LocalDate.of(1995, 5, 1), "30123456")))
                .isInstanceOf(com.activehub.shared.error.DniEnUsoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(usuarioId, req("x@email.com")))
                .isInstanceOf(NoEncontradoException.class);
    }
}
