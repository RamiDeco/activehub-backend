package com.activehub.usecases.marcartodasnotificacionesleidas;

import com.activehub.shared.notificacion.NotificacionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarcarTodasNotificacionesLeidasService {

    private final NotificacionRepository notificacionRepository;

    public MarcarTodasNotificacionesLeidasService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional
    public void marcarTodasLeidas(UUID usuarioId) {
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }
}
