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

    /**
     * @param entidadId el registro que originó el aviso (trazabilidad).
     * @param destino   a qué pantalla lleva el click; {@link Destino#ninguno()} si no hay una.
     *                  Se resuelve en el usecase, que es quien tiene cargada la cadena
     *                  inscripción → clase → actividad; el frontend no la tiene.
     */
    public void notificar(UUID usuarioId, TipoNotificacion tipo, String mensaje, UUID entidadId, Destino destino) {
        var usuario = usuarioRepository.getReferenceById(usuarioId);
        notificacionRepository.save(new Notificacion(usuario, tipo, mensaje, entidadId, destino));
    }
}
