package com.activehub.usecases.listartiposactividad;

import com.activehub.domain.actividad.TipoActividadRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarTiposActividadService {

    private final TipoActividadRepository tipoActividadRepository;

    public ListarTiposActividadService(TipoActividadRepository tipoActividadRepository) {
        this.tipoActividadRepository = tipoActividadRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarTiposActividadResponse> listar(UUID categoriaId) {
        var tipos = categoriaId != null
                ? tipoActividadRepository.findByCategoriaIdAndDeletedFalseOrderByNombreAsc(categoriaId)
                : tipoActividadRepository.findAllByDeletedFalseOrderByNombreAsc();

        return tipos.stream()
                .map(t -> new ListarTiposActividadResponse(t.getId(), t.getNombre(), t.getCategoria().getId()))
                .toList();
    }
}
