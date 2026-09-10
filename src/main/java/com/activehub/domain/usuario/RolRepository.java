package com.activehub.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombreIgnoreCaseAndDeletedFalse(String nombre);

    List<Rol> findAllByOrderBySistemaDescNombreAsc();

    /** Atajo para los tres roles del sistema, que es como los busca todo el codigo de altas. */
    default Optional<Rol> findByNombre(RolNombre nombre) {
        return findByNombre(nombre.name());
    }
}
