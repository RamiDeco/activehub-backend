package com.activehub.domain.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilAlumnoRepository extends JpaRepository<PerfilAlumno, UUID> {

    Optional<PerfilAlumno> findByUsuarioId(UUID usuarioId);
}
