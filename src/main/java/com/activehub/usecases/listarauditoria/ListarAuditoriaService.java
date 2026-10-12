package com.activehub.usecases.listarauditoria;

import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolRepository;
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
    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    public ListarAuditoriaService(
            AuditLogRepository auditLogRepository,
            UsuarioRepository usuarioRepository,
            PermisoRepository permisoRepository,
            RolRepository rolRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.usuarioRepository = usuarioRepository;
        this.permisoRepository = permisoRepository;
        this.rolRepository = rolRepository;
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

        // Los dos catalogos que hacen falta para que la descripcion nombre las cosas en vez de
        // mostrar claves e ids. Son dos tablas chicas (17 permisos, un puñado de roles): una
        // consulta cada una para todo el listado, no una por fila.
        Map<String, String> permisosPorClave = permisoRepository.findAllByOrderByOrdenAsc().stream()
                .collect(Collectors.toMap(Permiso::getClave, Permiso::getAccion));
        Map<UUID, String> rolesPorId = rolRepository.findAll().stream()
                .collect(Collectors.toMap(Rol::getId, Rol::getNombre));

        return logs.stream()
                .map(log -> {
                    UUID actorId = log.getActorId();
                    Usuario actor = actorId != null ? actoresPorId.get(actorId) : null;
                    String actorNombre = actorId == null ? "Sistema" : (actor != null ? actor.getNombre() + " " + actor.getApellido() : "Usuario eliminado");
                    String actorRol = actor != null ? actor.getRol().getNombre() : null;

                    return new ListarAuditoriaResponse(
                            log.getId(),
                            actorId,
                            actorNombre,
                            actorRol,
                            log.getAccion().name(),
                            log.getEntidad(),
                            log.getEntidadId(),
                            log.getMetadata(),
                            DescripcionAuditoria.describir(
                                    log.getAccion(), actorNombre, log.getMetadata(), log.getEntidadId(),
                                    permisosPorClave, rolesPorId),
                            log.getCreatedAt());
                })
                .toList();
    }
}
