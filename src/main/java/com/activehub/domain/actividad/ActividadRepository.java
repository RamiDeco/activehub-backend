package com.activehub.domain.actividad;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActividadRepository extends JpaRepository<Actividad, UUID>, JpaSpecificationExecutor<Actividad> {

    boolean existsByTipoActividadIdAndDeletedFalse(UUID tipoActividadId);
}
