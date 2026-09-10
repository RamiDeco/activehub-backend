package com.activehub.shared.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(UUID actorId, AuditAccion accion, String entidad, UUID entidadId, String metadata) {
        auditLogRepository.save(new AuditLog(actorId, accion, entidad, entidadId, metadata, ipDelRequestActual()));
    }

    /**
     * La IP se resuelve sola desde el request en curso en vez de agregarle un parametro a las
     * ~60 llamadas a {@code registrar(...)}: asi ninguna se olvida de pasarla. Devuelve null
     * cuando no hay request (los schedulers auditan fuera de un hilo HTTP), que es exactamente
     * lo que corresponde guardar en ese caso.
     */
    private String ipDelRequestActual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
            return null;
        }
        HttpServletRequest request = atributos.getRequest();
        // Detras de un proxy, getRemoteAddr() es la IP del proxy: la del cliente es la
        // primera de la cadena X-Forwarded-For.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String primera = forwarded.split(",")[0].trim();
            if (!primera.isEmpty()) {
                return recortar(primera);
            }
        }
        return recortar(request.getRemoteAddr());
    }

    private String recortar(String ip) {
        if (ip == null) {
            return null;
        }
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
