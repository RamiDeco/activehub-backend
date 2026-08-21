package com.activehub.usecases.listarmisnotificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.notificacion.Notificacion;
import com.activehub.shared.notificacion.NotificacionRepository;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarMisNotificacionesServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    private ListarMisNotificacionesService service;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new ListarMisNotificacionesService(notificacionRepository);
        usuarioId = UUID.randomUUID();
    }

    @Test
    void listar_mapeaDetalle() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        Notificacion notificacion = new Notificacion(usuario, TipoNotificacion.AUSENCIA_PROFESOR, "El instructor avisó que no podrá dar la clase.", UUID.randomUUID());
        ReflectionTestUtils.setField(notificacion, "id", UUID.randomUUID());

        when(notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(List.of(notificacion));

        List<ListarMisNotificacionesResponse> resultado = service.listar(usuarioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).tipo()).isEqualTo("AUSENCIA_PROFESOR");
        assertThat(resultado.get(0).mensaje()).isEqualTo("El instructor avisó que no podrá dar la clase.");
        assertThat(resultado.get(0).leida()).isFalse();
    }

    @Test
    void listar_sinNotificaciones_devuelveListaVacia() {
        when(notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(List.of());

        assertThat(service.listar(usuarioId)).isEmpty();
    }
}
