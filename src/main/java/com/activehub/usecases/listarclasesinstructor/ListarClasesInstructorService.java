package com.activehub.usecases.listarclasesinstructor;

import com.activehub.domain.actividad.ClaseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarClasesInstructorService {

    private final ClaseRepository claseRepository;

    public ListarClasesInstructorService(ClaseRepository claseRepository) {
        this.claseRepository = claseRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarClasesInstructorResponse> listar(UUID instructorId) {
        return claseRepository.findByInstructorIdConDetalle(instructorId).stream()
                .map(c -> {
                    var actividad = c.getActividad();
                    return new ListarClasesInstructorResponse(
                            c.getId(),
                            actividad.getId(),
                            actividad.getNombre(),
                            c.getFechaHora(),
                            c.getEstado().name(),
                            c.getCuposMax(),
                            c.getCuposOcupados());
                })
                .toList();
    }
}
