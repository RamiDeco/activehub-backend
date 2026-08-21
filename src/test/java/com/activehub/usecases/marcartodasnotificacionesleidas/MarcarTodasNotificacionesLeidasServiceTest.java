package com.activehub.usecases.marcartodasnotificacionesleidas;

import static org.mockito.Mockito.verify;

import com.activehub.shared.notificacion.NotificacionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarcarTodasNotificacionesLeidasServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    private MarcarTodasNotificacionesLeidasService service;

    @BeforeEach
    void setUp() {
        service = new MarcarTodasNotificacionesLeidasService(notificacionRepository);
    }

    @Test
    void marcarTodasLeidas_delegaEnElRepositorio() {
        UUID usuarioId = UUID.randomUUID();

        service.marcarTodasLeidas(usuarioId);

        verify(notificacionRepository).marcarTodasLeidas(usuarioId);
    }
}
