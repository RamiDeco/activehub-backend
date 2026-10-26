package com.activehub.usecases.reabrirsolicitudinstructor;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
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
 * En revision (PENDIENTE).
 *
 * <p>Desde el 2026-10-26 exige documentación NUEVA: al menos un documento vigente subido después
 * del rechazo. Antes se podía volver a postular sin cambiar nada y el administrador recibía de
 * nuevo exactamente lo que había rechazado. El motivo y la fecha del rechazo se CONSERVAN: el
 * administrador los ve al revisar la solicitud otra vez, y la fecha es la que marca qué es nuevo.
 *
 * <p>Es la unica transicion de EstadoVerificacion que no dispara el admin. Antes no existia:
 * el boton "Volver a postularme" llamaba a una funcion del frontend que mutaba una lista mock,
 * asi que parecia funcionar pero la pantalla seguia mostrando "Rechazada" y el backend nunca
 * se enteraba.
 */
@Service
public class ReabrirSolicitudInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final AuditService auditService;

    public ReabrirSolicitudInstructorService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            AuditService auditService) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReabrirSolicitudInstructorResponse reabrir(UUID instructorId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("No encontramos tu perfil de instructor."));

        if (perfil.getEstadoVerificacion() != EstadoVerificacion.RECHAZADO) {
            throw new ValidacionException("Solo podés volver a postularte si tu solicitud fue rechazada.");
        }

        // Sin fecha de rechazo (no debería pasar después de V35, que completó las viejas) no hay
        // contra qué comparar: no se bloquea a nadie por un dato que el sistema no tiene.
        if (perfil.getRechazadoAt() != null
                && !documentoInstructorRepository.existsByPerfilInstructorIdAndEstadoNotAndCreatedAtAfter(
                        perfil.getId(), DocumentoInstructor.RETIRADO, perfil.getRechazadoAt())) {
            throw new ValidacionException(
                    "Antes de volver a postularte, subí la documentación nueva que te pidieron en el motivo del rechazo.");
        }
        perfil.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        perfilInstructorRepository.save(perfil);

        auditService.registrar(
                instructorId, AuditAccion.SOLICITUD_INSTRUCTOR_REABIERTA, "PerfilInstructor", instructorId, null);

        return new ReabrirSolicitudInstructorResponse(instructorId, perfil.getEstadoVerificacion().name());
    }
}
