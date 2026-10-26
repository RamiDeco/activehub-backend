package com.activehub.usecases.retirarmidocumento;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
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
 * El instructor quita un documento propio que ya no sirve (un certificado vencido, el que le
 * rechazaron) (2026-10-26).
 *
 * <p>No se borra: pasa a {@code RETIRADO}. Él deja de verlo y el administrador lo conserva como
 * historial (es la documentación con la que se lo evaluó). El archivo tampoco se borra.
 *
 * <p>No puede quedarse sin ningún documento vigente: RN-12 exige documentación para ser
 * instructor, y el camino para reemplazar uno es subir el nuevo y después quitar el viejo.
 */
@Service
public class RetirarMiDocumentoService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final AuditService auditService;

    public RetirarMiDocumentoService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            AuditService auditService
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void retirar(UUID instructorId, UUID documentoId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("No encontramos tu perfil de instructor."));
        DocumentoInstructor documento = documentoInstructorRepository
                .findByIdAndPerfilInstructorId(documentoId, perfil.getId())
                .filter(d -> !d.isRetirado())
                .orElseThrow(() -> new NoEncontradoException("Documento no encontrado."));

        long vigentes = documentoInstructorRepository.countByPerfilInstructorIdAndEstadoNot(
                perfil.getId(), DocumentoInstructor.RETIRADO);
        if (vigentes <= 1) {
            throw new ValidacionException(
                    "Tenés que tener al menos un documento cargado. Subí el nuevo antes de quitar este.");
        }

        documento.setEstado(DocumentoInstructor.RETIRADO);
        documentoInstructorRepository.save(documento);
        auditService.registrar(
                instructorId, AuditAccion.DOCUMENTO_INSTRUCTOR_RETIRADO, "DocumentoInstructor", documento.getId(), null);
    }
}
