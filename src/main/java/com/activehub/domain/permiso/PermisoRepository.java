package com.activehub.domain.permiso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    List<Permiso> findAllByOrderByOrdenAsc();

    Optional<Permiso> findByClave(String clave);
}
