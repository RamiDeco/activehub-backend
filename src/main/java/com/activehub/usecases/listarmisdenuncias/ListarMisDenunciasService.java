package com.activehub.usecases.listarmisdenuncias;

import com.activehub.domain.denuncia.DenunciaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarMisDenunciasService {

    private final DenunciaRepository denunciaRepository;

    public ListarMisDenunciasService(DenunciaRepository denunciaRepository) {
        this.denunciaRepository = denunciaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisDenunciasResponse> listar(UUID alumnoId) {
        return denunciaRepository.findByAlumnoIdConDetalle(alumnoId).stream()
                .map(d -> {
                    var clase = d.getClase();
                    var actividad = clase.getActividad();
                    return new ListarMisDenunciasResponse(
                            d.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            d.getMotivo(),
                            d.getEstado().getEtiqueta(),
                            d.getCreatedAt());
                })
                .toList();
    }
}
