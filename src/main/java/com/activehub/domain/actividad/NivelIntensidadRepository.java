package com.activehub.domain.actividad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NivelIntensidadRepository extends JpaRepository<NivelIntensidad, UUID> {

    List<NivelIntensidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCaseAndDeletedFalse(String nombre);

    Optional<NivelIntensidad> findByNombreIgnoreCaseAndDeletedFalse(String nombre);
}
