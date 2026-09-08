package com.activehub.usecases.habilitarclasesproximas;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.VentanaInscripcion;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transicion Programada -> Habilitada de la seccion 3 de la especificacion: una clase pasa a
 * Habilitada cuando faltan 4 dias o menos para su dictado.
 *
 * <p>Responde la ambiguedad 3 del documento ("no esta definido quien dispara la transicion"):
 * la dispara este job, con la misma cadencia que el de finalizacion. Se persiste el estado en
 * vez de calcularlo al vuelo porque los badges de la UI leen {@code EstadoClase}; la ventana de
 * negocio para inscribirse sigue calculandose en el momento con {@link VentanaInscripcion}, que
 * es la fuente de verdad y no depende de que este job haya corrido.
 *
 * <p>Antes de esto {@code EstadoClase.Habilitada} estaba declarado en el enum y en el CHECK de
 * la migracion pero no lo asignaba nadie: toda clase se quedaba en Programada hasta que se
 * dictaba, y el badge "Habilitada" no aparecia nunca.
 */
@Service
public class HabilitarClasesProximasService {

    private final ClaseRepository claseRepository;
    private final AuditService auditService;
    private final Clock clock;

    public HabilitarClasesProximasService(
            ClaseRepository claseRepository, AuditService auditService, Clock clock) {
        this.claseRepository = claseRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public int habilitar() {
        Instant ahora = clock.instant();
        Instant limite = ahora.plus(VentanaInscripcion.UMBRAL_PREINSCRIPCION);

        List<Clase> candidatas =
                claseRepository.findByEstadoInAndFechaHoraBefore(List.of(EstadoClase.Programada), limite);

        int habilitadas = 0;
        for (Clase clase : candidatas) {
            // Las que ya pasaron son problema del job de finalizacion, no de este.
            if (!clase.getFechaHora().isAfter(ahora)) {
                continue;
            }
            clase.setEstado(EstadoClase.Habilitada);
            claseRepository.save(clase);
            auditService.registrar(null, AuditAccion.CLASE_HABILITADA, "Clase", clase.getId(), null);
            habilitadas++;
        }
        return habilitadas;
    }
}
