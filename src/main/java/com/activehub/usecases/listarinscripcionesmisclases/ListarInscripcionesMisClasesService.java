package com.activehub.usecases.listarinscripcionesmisclases;

import com.activehub.domain.inscripcion.InscripcionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscripciones a las clases del instructor logueado.
 *
 * <p>Alimenta el panel (grafico de inscripciones de los ultimos 7 dias y alerta de pagos en
 * efectivo por confirmar) y las metricas (alumnos e ingresos). Antes esas pantallas leian
 * {@code DataContext.inscripciones}, que es el dataset mock: sus claseId tienen forma
 * {@code act-running-c0} mientras que los reales son UUID, asi que el cruce nunca podia
 * matchear y los indicadores daban cero de forma deterministica.
 */
@Service
public class ListarInscripcionesMisClasesService {

    private final InscripcionRepository inscripcionRepository;

    public ListarInscripcionesMisClasesService(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarInscripcionesMisClasesResponse> listar(UUID instructorId) {
        return inscripcionRepository.findByInstructorIdConDetalle(instructorId).stream()
                .map(i -> {
                    var clase = i.getClase();
                    var alumno = i.getAlumno();
                    var pago = i.getPago();
                    return new ListarInscripcionesMisClasesResponse(
                            i.getId(),
                            clase.getId(),
                            clase.getActividad().getId(),
                            clase.getActividad().getNombre(),
                            clase.getFechaHora(),
                            clase.getEstado().name(),
                            alumno.getId(),
                            alumno.getNombre() + " " + alumno.getApellido(),
                            i.getEstado().getEtiqueta(),
                            i.getCreatedAt(),
                            pago != null ? pago.getEstado().name() : null,
                            pago != null ? pago.getMonto() : null);
                })
                .toList();
    }
}
