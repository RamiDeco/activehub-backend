package com.activehub.usecases.listarmisresenas;

import com.activehub.domain.resenia.ReseniaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarMisResenasService {

    private final ReseniaRepository reseniaRepository;

    public ListarMisResenasService(ReseniaRepository reseniaRepository) {
        this.reseniaRepository = reseniaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisResenasResponse> listar(UUID alumnoId) {
        return reseniaRepository.findByAlumnoIdConDetalle(alumnoId).stream()
                .map(r -> {
                    var clase = r.getClase();
                    var actividad = clase.getActividad();
                    var instructor = actividad.getInstructor();
                    return new ListarMisResenasResponse(
                            r.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            instructor.getNombre() + " " + instructor.getApellido(),
                            r.getPuntaje(),
                            r.getComentario(),
                            r.isEnModeracion(),
                            r.getCreatedAt());
                })
                .toList();
    }
}
