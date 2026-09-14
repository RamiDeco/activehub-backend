package com.activehub.usecases.reabrirsolicitudinstructor;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2I-HU12 criterio 6: desde Rechazada, el instructor vuelve a postularse y su perfil pasa a
 * En revision (PENDIENTE), limpiando el motivo del rechazo anterior.
 *
 * <p>Es la unica transicion de EstadoVerificacion que no dispara el admin. Antes no existia:
 * el boton "Volver a postularme" llamaba a una funcion del frontend que mutaba una lista mock,
 * asi que parecia funcionar pero la pantalla seguia mostrando "Rechazada" y el backend nunca
 * se enteraba.
 */
@Service
public class ReabrirSolicitudInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;

    public ReabrirSolicitudInstructorService(
            PerfilInstructorRepository perfilInstructorRepository, AuditService auditService) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReabrirSolicitudInstructorResponse reabrir(UUID instructorId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("No encontramos tu perfil de instructor."));

        if (perfil.getEstadoVerificacion() != EstadoVerificacion.RECHAZADO) {
            throw new ValidacionException("Solo podés volver a postularte si tu solicitud fue rechazada.");
        }

        perfil.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        perfil.setMotivoRechazo(null);
        perfilInstructorRepository.save(perfil);

        auditService.registrar(
                instructorId, AuditAccion.SOLICITUD_INSTRUCTOR_REABIERTA, "PerfilInstructor", instructorId, null);

        return new ReabrirSolicitudInstructorResponse(instructorId, perfil.getEstadoVerificacion().name());
    }
}
