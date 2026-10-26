package com.activehub.domain.usuario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoInstructorRepository extends JpaRepository<DocumentoInstructor, UUID> {

    List<DocumentoInstructor> findByPerfilInstructorIdOrderByCreatedAtDesc(UUID perfilInstructorId);

    Optional<DocumentoInstructor> findByIdAndPerfilInstructorId(UUID id, UUID perfilInstructorId);

    /** Los documentos que el instructor ve como suyos: todos menos los que retiró. */
    List<DocumentoInstructor> findByPerfilInstructorIdAndEstadoNotOrderByCreatedAtDesc(UUID perfilInstructorId, String estado);

    long countByPerfilInstructorIdAndEstadoNot(UUID perfilInstructorId, String estado);

    /** Si subió documentación vigente después de una fecha (la del último rechazo). */
    boolean existsByPerfilInstructorIdAndEstadoNotAndCreatedAtAfter(UUID perfilInstructorId, String estado, Instant despues);
}
