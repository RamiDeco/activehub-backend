package com.activehub.usecases.listarauditoria;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditLog;
import com.activehub.shared.audit.AuditLogRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarAuditoriaService {

    private final AuditLogRepository auditLogRepository;
    private final UsuarioRepository usuarioRepository;

    public ListarAuditoriaService(AuditLogRepository auditLogRepository, UsuarioRepository usuarioRepository) {
        this.auditLogRepository = auditLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarAuditoriaResponse> listar() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();

        List<UUID> actorIds = logs.stream()
                .map(AuditLog::getActorId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<UUID, Usuario> actoresPorId = usuarioRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        return logs.stream()
                .map(log -> {
                    UUID actorId = log.getActorId();
                    Usuario actor = actorId != null ? actoresPorId.get(actorId) : null;
                    String actorNombre = actorId == null ? "Sistema" : (actor != null ? actor.getNombre() + " " + actor.getApellido() : "Usuario eliminado");
                    String actorRol = actor != null ? actor.getRol().getNombre().name() : null;

                    return new ListarAuditoriaResponse(
                            log.getId(),
                            actorId,
                            actorNombre,
                            actorRol,
                            log.getAccion().name(),
                            log.getEntidad(),
                            log.getEntidadId(),
                            log.getMetadata(),
                            log.getCreatedAt());
                })
                .toList();
    }
}
