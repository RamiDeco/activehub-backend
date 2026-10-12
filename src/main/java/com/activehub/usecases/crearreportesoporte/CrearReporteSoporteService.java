package com.activehub.usecases.crearreportesoporte;

import com.activehub.domain.soporte.ReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporteRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearReporteSoporteService {

    private final ReporteSoporteRepository reporteSoporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public CrearReporteSoporteService(
            ReporteSoporteRepository reporteSoporteRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService
    ) {
        this.reporteSoporteRepository = reporteSoporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    /**
     * @param autorId id del usuario logueado, o {@code null} si lo manda un visitante sin
     *                cuenta. El endpoint es publico, asi que este caso es normal, no un error.
     */
    @Transactional
    public CrearReporteSoporteResponse crear(CrearReporteSoporteRequest request, UUID autorId) {
        ReporteSoporte reporte = new ReporteSoporte();
        reporte.setEmail(request.email().trim().toLowerCase());
        reporte.setAsunto(request.asunto().trim());
        reporte.setDetalle(request.detalle().trim());

        // Un id que ya no existe (cuenta dada de baja con el token todavia vivo) no invalida el
        // reporte: se guarda como anonimo, que es exactamente lo que es a partir de ese momento.
        if (autorId != null) {
            usuarioRepository.findById(autorId).ifPresent(reporte::setUsuario);
        }

        reporte = reporteSoporteRepository.saveAndFlush(reporte);

        auditService.registrar(
                autorId, AuditAccion.REPORTE_SOPORTE_CREADO, "ReporteSoporte", reporte.getId(), null);

        return new CrearReporteSoporteResponse(
                reporte.getId(), reporte.getEstado().getEtiqueta(), reporte.getCreatedAt());
    }
}
