package com.activehub.usecases.listarresenasactividad;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarResenasActividadService {

    private final ActividadRepository actividadRepository;
    private final ReseniaRepository reseniaRepository;

    public ListarResenasActividadService(ActividadRepository actividadRepository, ReseniaRepository reseniaRepository) {
        this.actividadRepository = actividadRepository;
        this.reseniaRepository = reseniaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarResenasActividadResponse> listar(UUID actividadId) {
        if (!actividadRepository.existsById(actividadId)) {
            throw new NoEncontradoException("Actividad no encontrada.");
        }

        return reseniaRepository.findVisiblesPorActividad(actividadId).stream()
                .map(r -> new ListarResenasActividadResponse(
                        r.getId(),
                        r.getClase().getId(),
                        new ListarResenasActividadResponse.Alumno(
                                r.getAlumno().getId(), r.getAlumno().getNombre(), r.getAlumno().getApellido()),
                        r.getPuntaje(),
                        r.getComentario(),
                        r.getCreatedAt(),
                        r.getRespuestaInstructor(),
                        r.getRespuestaInstructorAt()))
                .toList();
    }
}
