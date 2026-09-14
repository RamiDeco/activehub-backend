package com.activehub.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoInstructorRepository extends JpaRepository<DocumentoInstructor, UUID> {

    List<DocumentoInstructor> findByPerfilInstructorIdOrderByCreatedAtDesc(UUID perfilInstructorId);

    Optional<DocumentoInstructor> findByIdAndPerfilInstructorId(UUID id, UUID perfilInstructorId);
}
