package com.activehub.shared.notificacion;

import com.activehub.domain.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void notificar(UUID usuarioId, TipoNotificacion tipo, String mensaje, UUID entidadId) {
        var usuario = usuarioRepository.getReferenceById(usuarioId);
        notificacionRepository.save(new Notificacion(usuario, tipo, mensaje, entidadId));
    }
}
