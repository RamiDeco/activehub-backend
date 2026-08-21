package com.activehub.domain.actividad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoActividadRepository extends JpaRepository<TipoActividad, UUID> {

    List<TipoActividad> findAllByDeletedFalseOrderByNombreAsc();

    List<TipoActividad> findByCategoriaIdAndDeletedFalseOrderByNombreAsc(UUID categoriaId);

    boolean existsByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse(String nombre, UUID categoriaId);

    Optional<TipoActividad> findByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse(String nombre, UUID categoriaId);

    boolean existsByCategoriaIdAndDeletedFalse(UUID categoriaId);
}
