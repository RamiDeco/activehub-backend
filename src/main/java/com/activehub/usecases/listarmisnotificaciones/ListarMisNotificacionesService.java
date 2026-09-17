package com.activehub.usecases.listarmisnotificaciones;

import com.activehub.shared.notificacion.NotificacionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarMisNotificacionesService {

    private final NotificacionRepository notificacionRepository;

    public ListarMisNotificacionesService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisNotificacionesResponse> listar(UUID usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(n -> new ListarMisNotificacionesResponse(
                        n.getId(), n.getTipo().name(), n.getMensaje(), n.getEntidadId(),
                        n.getDestinoTipo().name(), n.getDestinoId(), n.isLeida(), n.getCreatedAt()))
                .toList();
    }
}
