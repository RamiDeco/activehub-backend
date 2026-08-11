package com.activehub.usecases.preinscribirse;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.VentanaInscripcion;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.InscripcionYaExisteException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreinscribirseService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final Clock clock;

    public PreinscribirseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public PreinscribirseResponse preinscribirse(UUID claseId, UUID alumnoId) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (clase.getEstado() == EstadoClase.Cancelada || clase.getEstado() == EstadoClase.Finalizada) {
            throw new ValidacionException("Esta clase ya no admite inscripciones.");
        }

        if (!VentanaInscripcion.esVentanaPreInscripcion(clock.instant(), clase.getFechaHora())) {
            throw new ValidacionException("Faltan 4 días o menos para la clase: ya podés inscribirte directamente.");
        }

        inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA)
                .ifPresent(existente -> {
                    throw new InscripcionYaExisteException("Ya tenés una inscripción activa para esta clase.");
                });

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setAlumno(usuarioRepository.getReferenceById(alumnoId));
        inscripcion.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        inscripcion = inscripcionRepository.saveAndFlush(inscripcion);

        auditService.registrar(alumnoId, AuditAccion.PREINSCRIPCION_CREADA, "Inscripcion", inscripcion.getId(), null);

        return new PreinscribirseResponse(
                inscripcion.getId(), claseId, alumnoId, inscripcion.getEstado().getEtiqueta(), inscripcion.getCreatedAt(), null);
    }
}
