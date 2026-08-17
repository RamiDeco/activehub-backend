package com.activehub.usecases.finalizarclasesvencidas;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizarClasesVencidasService {

    private static final List<EstadoClase> ESTADOS_A_FINALIZAR = List.of(EstadoClase.Programada, EstadoClase.Habilitada);

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AuditService auditService;
    private final Clock clock;

    public FinalizarClasesVencidasService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void finalizar() {
        List<Clase> vencidas = claseRepository.findByEstadoInAndFechaHoraBefore(ESTADOS_A_FINALIZAR, clock.instant());

        for (Clase clase : vencidas) {
            clase.setEstado(EstadoClase.Finalizada);
            claseRepository.save(clase);

            for (Inscripcion inscripcion : inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA)) {
                if (inscripcion.getEstado() == EstadoInscripcion.PRE_INSCRIPCION
                        || inscripcion.getEstado() == EstadoInscripcion.PAGO_PENDIENTE) {
                    inscripcion.setEstado(EstadoInscripcion.CANCELADA);
                    inscripcionRepository.save(inscripcion);
                }
            }

            auditService.registrar(null, AuditAccion.CLASE_FINALIZADA, "Clase", clase.getId(), null);
        }
    }
}
