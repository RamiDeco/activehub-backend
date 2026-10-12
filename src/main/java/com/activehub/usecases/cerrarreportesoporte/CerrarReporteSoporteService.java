package com.activehub.usecases.cerrarreportesoporte;

import com.activehub.domain.soporte.EstadoReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporteRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CerrarReporteSoporteService {

    private final ReporteSoporteRepository reporteSoporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final Clock clock;

    public CerrarReporteSoporteService(
            ReporteSoporteRepository reporteSoporteRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.reporteSoporteRepository = reporteSoporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public CerrarReporteSoporteResponse cerrar(UUID id, CerrarReporteSoporteRequest request, UUID actorId) {
        ReporteSoporte reporte = reporteSoporteRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reporte de soporte no encontrado."));

        // El estado solo avanza, igual que en las denuncias: volver a cerrar pisaria la
        // respuesta y la fecha del cierre original, que es justo lo que hay que conservar.
        if (reporte.getEstado() == EstadoReporteSoporte.CERRADO) {
            throw new ValidacionException("Este reporte ya está cerrado.");
        }

        String respuesta = request.respuesta() == null || request.respuesta().isBlank()
                ? null
                : request.respuesta().trim();

        reporte.setEstado(EstadoReporteSoporte.CERRADO);
        reporte.setRespuesta(respuesta);
        reporte.setCerradoAt(Instant.now(clock));
        usuarioRepository.findById(actorId).ifPresent(reporte::setCerradoPor);
        reporteSoporteRepository.save(reporte);

        auditService.registrar(actorId, AuditAccion.REPORTE_SOPORTE_CERRADO, "ReporteSoporte", id, null);

        return new CerrarReporteSoporteResponse(
                reporte.getId(), reporte.getEstado().getEtiqueta(), reporte.getRespuesta(), reporte.getCerradoAt());
    }
}
