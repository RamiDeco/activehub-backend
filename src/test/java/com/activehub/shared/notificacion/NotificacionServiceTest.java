package com.activehub.shared.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pedido del usuario: las notificaciones importantes (una clase cancelada, un reintegro, una
 * sanción) tienen que llegar también por correo. Lo que se fija acá es la frontera: cuáles
 * publican el evento del mail y cuáles se quedan sólo en la campana.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ApplicationEventPublisher eventos;

    @InjectMocks private NotificacionService service;

    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Sofía");
        usuario.setEmail("sofia@example.com");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        org.mockito.Mockito.lenient().when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(usuario);
    }

    @Test
    void notificar_claseCancelada_guardaYPublicaElCorreo() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.notificar(usuarioId, TipoNotificacion.CLASE_CANCELADA, "Se canceló la clase de Yoga.",
                UUID.randomUUID(), Destino.ninguno());

        verify(notificacionRepository).save(any(Notificacion.class));
        ArgumentCaptor<CorreoNotificacionEvent> evento = ArgumentCaptor.forClass(CorreoNotificacionEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().email()).isEqualTo("sofia@example.com");
        assertThat(evento.getValue().nombre()).isEqualTo("Sofía");
        assertThat(evento.getValue().tipo()).isEqualTo(TipoNotificacion.CLASE_CANCELADA);
        assertThat(evento.getValue().mensaje()).isEqualTo("Se canceló la clase de Yoga.");
    }

    @Test
    void notificar_rutinaria_noMandaCorreo() {
        service.notificar(usuarioId, TipoNotificacion.NUEVA_INSCRIPCION, "Nueva inscripción.",
                UUID.randomUUID(), Destino.ninguno());

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(eventos, never()).publishEvent(any());
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void notificar_importanteSinCorreoCargado_soloQuedaEnLaCampana() {
        usuario.setEmail(null);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.notificar(usuarioId, TipoNotificacion.INSCRIPCION_CANCELADA, "Se canceló tu inscripción.",
                UUID.randomUUID(), Destino.ninguno());

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void lasImportantesSonLasQueTocanAgendaPlataOCuenta() {
        assertThat(TipoNotificacion.CLASE_CANCELADA.porCorreo()).isTrue();
        assertThat(TipoNotificacion.INSCRIPCION_CANCELADA.porCorreo()).isTrue();
        assertThat(TipoNotificacion.AUSENCIA_PROFESOR.porCorreo()).isTrue();
        assertThat(TipoNotificacion.ACTIVIDAD_ELIMINADA.porCorreo()).isTrue();
        assertThat(TipoNotificacion.PENALIZACION_APLICADA.porCorreo()).isTrue();
        assertThat(TipoNotificacion.NUEVA_RESENIA.porCorreo()).isFalse();
        assertThat(TipoNotificacion.NUEVO_HORARIO_FAVORITO.porCorreo()).isFalse();
    }
}
