package com.activehub.domain.actividad;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActividadImagenRepository extends JpaRepository<ActividadImagen, UUID> {

    List<ActividadImagen> findByActividadIdOrderByOrdenAsc(UUID actividadId);

    List<ActividadImagen> findByActividadIdInOrderByOrdenAsc(List<UUID> actividadIds);

    int countByActividadId(UUID actividadId);

    /** Si un archivo de fotos de la actividad pertenece a su galería (la portada puede ser uno). */
    boolean existsByActividadIdAndPath(UUID actividadId, String path);

    void deleteByActividadId(UUID actividadId);
}
