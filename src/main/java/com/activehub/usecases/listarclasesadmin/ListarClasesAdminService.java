package com.activehub.usecases.listarclasesadmin;

import com.activehub.domain.actividad.ClaseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarClasesAdminService {

    private final ClaseRepository claseRepository;

    public ListarClasesAdminService(ClaseRepository claseRepository) {
        this.claseRepository = claseRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarClasesAdminResponse> listar() {
        return claseRepository.findAllConDetalle().stream()
                .map(c -> {
                    var actividad = c.getActividad();
                    return new ListarClasesAdminResponse(
                            c.getId(),
                            actividad.getId(),
                            actividad.getNombre(),
                            c.getFechaHora(),
                            c.getHoraFin(),
                            c.getEstado().name(),
                            c.getCuposMax(),
                            c.getCuposOcupados());
                })
                .toList();
    }
}
