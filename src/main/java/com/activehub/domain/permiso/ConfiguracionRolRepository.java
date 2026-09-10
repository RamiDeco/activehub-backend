package com.activehub.domain.permiso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConfiguracionRolRepository extends JpaRepository<ConfiguracionRol, UUID> {

    @Query("SELECT c FROM ConfiguracionRol c JOIN FETCH c.permiso p JOIN FETCH c.rol r ORDER BY p.orden ASC")
    List<ConfiguracionRol> findAllConDetalle();

    List<ConfiguracionRol> findByRolId(UUID rolId);

    Optional<ConfiguracionRol> findByRolIdAndPermisoClave(UUID rolId, String clave);

    /** Ids de los roles que tienen habilitada una clave. Evita un N+1 cuando hay que decidir
     * lo mismo para una lista entera de usuarios. */
    @Query("SELECT c.rol.id FROM ConfiguracionRol c WHERE c.permiso.clave = :clave AND c.habilitado = true")
    List<UUID> rolesConPermiso(String clave);

    /** La pregunta del control de acceso, resuelta en una consulta. */
    @Query("SELECT c.habilitado FROM ConfiguracionRol c WHERE c.rol.id = :rolId AND c.permiso.clave = :clave")
    Optional<Boolean> estaHabilitado(UUID rolId, String clave);
}
