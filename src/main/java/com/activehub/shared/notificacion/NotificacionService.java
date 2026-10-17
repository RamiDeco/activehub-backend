package com.activehub.shared.notificacion;

import com.activehub.domain.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository,
            ApplicationEventPublisher eventos
    ) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventos = eventos;
    }

    /**
     * @param entidadId el registro que originó el aviso (trazabilidad).
     * @param destino   a qué pantalla lleva el click; {@link Destino#ninguno()} si no hay una.
     *                  Se resuelve en el usecase, que es quien tiene cargada la cadena
     *                  inscripción → clase → actividad; el frontend no la tiene.
     *
     * <p>Si el tipo es de los importantes ({@link TipoNotificacion#porCorreo()}) además se
     * publica un {@link CorreoNotificacionEvent}. El mail <b>no</b> sale desde acá: lo manda
     * {@link CorreoNotificacionListener} después del commit, así que una operación que hace
     * rollback no avisa por correo algo que no pasó, y la latencia de Gmail no se suma a la
     * transacción de quien notifica.
     */
    public void notificar(UUID usuarioId, TipoNotificacion tipo, String mensaje, UUID entidadId, Destino destino) {
        var usuario = usuarioRepository.getReferenceById(usuarioId);
        notificacionRepository.save(new Notificacion(usuario, tipo, mensaje, entidadId, destino));

        if (tipo.porCorreo()) {
            // findById y no la referencia: el listener corre en otro hilo, fuera de esta
            // sesión, y no puede inicializar un proxy. Se le pasan los datos ya leídos.
            usuarioRepository.findById(usuarioId)
                    .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                    .ifPresent(u -> eventos.publishEvent(
                            new CorreoNotificacionEvent(u.getEmail(), u.getNombre(), tipo, mensaje)));
        }
    }
}
