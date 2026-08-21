package com.activehub.domain.actividad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCaseAndDeletedFalse(String nombre);

    Optional<Categoria> findByNombreIgnoreCaseAndDeletedFalse(String nombre);
}
