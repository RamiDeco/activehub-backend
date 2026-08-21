package com.activehub.domain.favorito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritoRepository extends JpaRepository<ActividadFavorita, UUID> {

    List<ActividadFavorita> findByUsuarioId(UUID usuarioId);

    List<ActividadFavorita> findByActividadId(UUID actividadId);

    Optional<ActividadFavorita> findByUsuarioIdAndActividadId(UUID usuarioId, UUID actividadId);
}
