package com.activehub.usecases.listarmisfavoritos;

import com.activehub.domain.favorito.FavoritoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarMisFavoritosService {

    private final FavoritoRepository favoritoRepository;

    public ListarMisFavoritosService(FavoritoRepository favoritoRepository) {
        this.favoritoRepository = favoritoRepository;
    }

    @Transactional(readOnly = true)
    public List<UUID> listar(UUID alumnoId) {
        return favoritoRepository.findByUsuarioId(alumnoId).stream()
                .map(f -> f.getActividad().getId())
                .toList();
    }
}
