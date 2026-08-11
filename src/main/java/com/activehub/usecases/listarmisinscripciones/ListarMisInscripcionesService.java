package com.activehub.usecases.listarmisinscripciones;

import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.error.ValidacionException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarMisInscripcionesService {

    private final InscripcionRepository inscripcionRepository;

    public ListarMisInscripcionesService(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisInscripcionesResponse> listar(UUID alumnoId, String estadoEtiqueta) {
        EstadoInscripcion estado = null;
        if (estadoEtiqueta != null) {
            try {
                estado = EstadoInscripcion.fromEtiqueta(estadoEtiqueta);
            } catch (IllegalArgumentException ex) {
                throw new ValidacionException("Estado de inscripción inválido: " + estadoEtiqueta);
            }
        }

        return inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, estado).stream()
                .map(i -> {
                    var clase = i.getClase();
                    var actividad = clase.getActividad();
                    var pago = i.getPago();
                    var pagoDto = pago != null
                            ? new ListarMisInscripcionesResponse.Pago(
                                    pago.getId(), pago.getEstado().name(), pago.getMonto(), pago.getMetodo().getEtiqueta())
                            : null;

                    return new ListarMisInscripcionesResponse(
                            i.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            clase.getEstado().name(),
                            actividad.getId(),
                            actividad.getNombre(),
                            alumnoId,
                            i.getEstado().getEtiqueta(),
                            i.getCreatedAt(),
                            pago != null ? pago.getId() : null,
                            pagoDto
                    );
                })
                .toList();
    }
}
