package com.activehub.usecases.obteneractividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerActividadService {

    private static final List<EstadoClase> ESTADOS_EXCLUIDOS = List.of(EstadoClase.Cancelada, EstadoClase.Finalizada);

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;

    public ObtenerActividadService(ActividadRepository actividadRepository, ClaseRepository claseRepository) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
    }

    @Transactional(readOnly = true)
    public ObtenerActividadResponse obtener(UUID id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        var tipo = actividad.getTipoActividad();
        var categoria = tipo.getCategoria();
        var instructor = actividad.getInstructor();

        var clases = claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(id, ESTADOS_EXCLUIDOS).stream()
                .map(c -> new ObtenerActividadResponse.Clase(
                        c.getId(), c.getFechaHora(), c.getEstado().name(), c.getCuposMax(), c.getCuposOcupados()))
                .toList();

        return new ObtenerActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                new ObtenerActividadResponse.TipoActividad(tipo.getId(), tipo.getNombre()),
                new ObtenerActividadResponse.Categoria(categoria.getId(), categoria.getNombre()),
                actividad.getNivelIntensidad().getEtiqueta(),
                new ObtenerActividadResponse.Instructor(instructor.getId(), instructor.getNombre(), instructor.getApellido()),
                actividad.getPrecio(),
                actividad.getUbicacion(),
                actividad.getPhotoTint(),
                actividad.getRating(),
                actividad.getCuposMax(),
                clases
        );
    }
}
