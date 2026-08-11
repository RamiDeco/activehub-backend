package com.activehub.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilInstructorRepository extends JpaRepository<PerfilInstructor, UUID> {

    Optional<PerfilInstructor> findByUsuarioId(UUID usuarioId);

    List<PerfilInstructor> findByEstadoVerificacion(EstadoVerificacion estadoVerificacion);
}
