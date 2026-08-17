package com.activehub.usecases.listarresenasinstructor;

import com.activehub.domain.resenia.ReseniaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarResenasInstructorService {

    private final ReseniaRepository reseniaRepository;

    public ListarResenasInstructorService(ReseniaRepository reseniaRepository) {
        this.reseniaRepository = reseniaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarResenasInstructorResponse> listar(UUID instructorId) {
        return reseniaRepository.findByInstructorIdConDetalle(instructorId).stream()
                .map(r -> {
                    var clase = r.getClase();
                    var actividad = clase.getActividad();
                    var alumno = r.getAlumno();
                    return new ListarResenasInstructorResponse(
                            r.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            new ListarResenasInstructorResponse.Alumno(alumno.getId(), alumno.getNombre(), alumno.getApellido()),
                            r.getPuntaje(),
                            r.getComentario(),
                            r.isEnModeracion(),
                            r.getCreatedAt());
                })
                .toList();
    }
}
