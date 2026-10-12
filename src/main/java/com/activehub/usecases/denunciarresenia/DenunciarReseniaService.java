package com.activehub.usecases.denunciarresenia;

import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2I-HU11 criterio 4: el instructor denuncia una reseña sobre su actividad.
 *
 * <p>Antes era imposible de guardar: {@code denuncia.clase_id} era NOT NULL y no existía
 * {@code resenia_id}, así que la denuncia no tenía a qué apuntar. El admin la resuelve desde
 * la misma pantalla de reclamos, con dos acciones válidas: OCULTAR_RESENIA o DESESTIMAR.
 */
@Service
public class DenunciarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstructorVerificadoGuard instructorVerificadoGuard;
    private final AuditService auditService;

    public DenunciarReseniaService(
            ReseniaRepository reseniaRepository,
            DenunciaRepository denunciaRepository,
            UsuarioRepository usuarioRepository,
            InstructorVerificadoGuard instructorVerificadoGuard,
            AuditService auditService
    ) {
        this.reseniaRepository = reseniaRepository;
        this.denunciaRepository = denunciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
        this.auditService = auditService;
    }

    @Transactional
    public DenunciarReseniaResponse denunciar(UUID reseniaId, DenunciarReseniaRequest request, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "denunciar reseñas");

        Resenia resenia = reseniaRepository.findById(reseniaId)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        if (!resenia.getClase().getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés denunciar una reseña de otro instructor.");
        }
        if (resenia.isOculta()) {
            throw new ValidacionException("Esta reseña ya fue ocultada.");
        }
        if (denunciaRepository.existsByReseniaIdAndDenuncianteId(reseniaId, instructorId)) {
            throw new ValidacionException("Ya denunciaste esta reseña.");
        }

        Denuncia denuncia = new Denuncia();
        denuncia.setResenia(resenia);
        denuncia.setDenunciante(usuarioRepository.getReferenceById(instructorId));
        denuncia.setMotivo(request.motivo().trim());
        denuncia = denunciaRepository.saveAndFlush(denuncia);

        auditService.registrar(instructorId, AuditAccion.RESENIA_DENUNCIADA, "Denuncia", denuncia.getId(), null);

        return new DenunciarReseniaResponse(
                denuncia.getId(), resenia.getId(), denuncia.getEstado().getEtiqueta(), denuncia.getCreatedAt());
    }
}
