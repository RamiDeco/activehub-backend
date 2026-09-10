package com.activehub.usecases.listarmisdenuncias;

import com.activehub.domain.actividad.Clase;
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
    public List<ListarMisDenunciasResponse> listar(UUID denuncianteId) {
        // Por denunciante y no por alumno: un instructor que denuncia una reseña también
        // tiene que poder seguir la suya.
        return denunciaRepository.findByDenuncianteIdConDetalle(denuncianteId).stream()
                .map(d -> {
                    boolean sobreResenia = d.getResenia() != null;
                    Clase clase = sobreResenia ? d.getResenia().getClase() : d.getClase();
                    var actividad = clase.getActividad();
                    return new ListarMisDenunciasResponse(
                            d.getId(),
                            sobreResenia ? "RESENIA" : "CLASE",
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            d.getMotivo(),
                            d.getEstado().getEtiqueta(),
                            d.getResolucion() != null ? d.getResolucion().name() : null,
                            d.getDetalle(),
                            d.getCreatedAt());
                })
                .toList();
    }
}
