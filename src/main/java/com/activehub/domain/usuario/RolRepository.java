package com.activehub.domain.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByNombre(RolNombre nombre);
}
