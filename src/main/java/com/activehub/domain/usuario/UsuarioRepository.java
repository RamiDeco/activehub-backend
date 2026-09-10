package com.activehub.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** Cuantas cuentas activas tiene ese rol (tarjeta de "Roles y permisos"). */
    long countByRolIdAndDeletedFalse(UUID rolId);

    Optional<Usuario> findByEmailIgnoreCaseAndDeletedFalse(String email);

    /**
     * Igual que la anterior pero con el Rol ya cargado.
     *
     * <p>La usa el login, que a proposito NO es @Transactional (si lo fuera, el rollback de la
     * excepcion de credenciales borraria el registro de auditoria del intento fallido). Sin
     * transaccion ambiente y con open-in-view=false no hay sesion abierta despues de la
     * consulta, asi que tocar el proxy lazy de Rol explotaba con LazyInitializationException.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE lower(u.email) = lower(:email) AND u.deleted = false")
    Optional<Usuario> findByEmailConRol(@Param("email") String email);

    /**
     * Login por DNI (nota de credenciales de la epica E1A: "el modulo de Autenticacion admite
     * ademas el ingreso por DNI"). Mismo JOIN FETCH y misma razon que {@link #findByEmailConRol}.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.dni = :dni AND u.deleted = false")
    Optional<Usuario> findByDniConRol(@Param("dni") String dni);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByDniAndDeletedFalse(String dni);

    List<Usuario> findAllByOrderByCreatedAtDesc();
}
