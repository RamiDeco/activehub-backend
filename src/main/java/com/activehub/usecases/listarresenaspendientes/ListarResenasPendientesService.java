package com.activehub.usecases.listarresenaspendientes;

import com.activehub.domain.resenia.ReseniaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarResenasPendientesService {

    private final ReseniaRepository reseniaRepository;

    public ListarResenasPendientesService(ReseniaRepository reseniaRepository) {
        this.reseniaRepository = reseniaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarResenasPendientesResponse> listar() {
        return reseniaRepository.findPendientesConDetalle().stream()
                .map(r -> {
                    var clase = r.getClase();
                    var actividad = clase.getActividad();
                    var alumno = r.getAlumno();
                    return new ListarResenasPendientesResponse(
                            r.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            new ListarResenasPendientesResponse.Alumno(alumno.getId(), alumno.getNombre(), alumno.getApellido()),
                            r.getPuntaje(),
                            r.getComentario(),
                            r.getCreatedAt());
                })
                .toList();
    }
}
