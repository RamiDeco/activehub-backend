package com.activehub.shared.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(UUID actorId, AuditAccion accion, String entidad, UUID entidadId, String metadata) {
        auditLogRepository.save(new AuditLog(actorId, accion, entidad, entidadId, metadata));
    }
}
