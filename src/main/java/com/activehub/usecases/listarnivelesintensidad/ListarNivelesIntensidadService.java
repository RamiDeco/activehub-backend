package com.activehub.usecases.listarnivelesintensidad;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarNivelesIntensidadService {

    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final ActividadRepository actividadRepository;

    public ListarNivelesIntensidadService(
            NivelIntensidadRepository nivelIntensidadRepository, ActividadRepository actividadRepository) {
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.actividadRepository = actividadRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarNivelesIntensidadResponse> listar() {
        return nivelIntensidadRepository.findAllByOrderByNombreAsc().stream()
                .map(n -> new ListarNivelesIntensidadResponse(
                        n.getId(),
                        n.getNombre(),
                        n.getDescripcion(),
                        actividadRepository.countByNivelIntensidadIdAndDeletedFalse(n.getId())))
                .toList();
    }
}
