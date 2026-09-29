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
    /**
     * OJO: desde V26 esto puede devolver MÁS DE UNA fila y lanzar
     * {@code IncorrectResultSizeDataAccessException}. Varias cuentas pueden compartir un
     * correo mientras ninguna lo confirmó. **El login usa {@link #findAllByEmailConRol}**;
     * esta queda para los casos en que el correo ya está verificado y es único por definición.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE lower(u.email) = lower(:email) AND u.deleted = false")
    Optional<Usuario> findByEmailConRol(@Param("email") String email);

    /**
     * Todas las cuentas vivas con ese correo, las verificadas primero.
     *
     * <p>Existe por la regla de V26: un correo sin confirmar no es único, así que "buscar el
     * usuario por email" ya no tiene una única respuesta. Lo que desambigua es la
     * <b>contraseña</b> — dos personas distintas que tipearon el mismo correo tienen claves
     * distintas —, así que el login recorre esta lista y se queda con la que coincide. El
     * orden pone primero a la verificada: es la dueña del correo y la que tiene que ganar si
     * hubiera empate de contraseña.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE lower(u.email) = lower(:email) AND u.deleted = false "
            + "ORDER BY u.emailVerificado DESC, u.createdAt DESC")
    List<Usuario> findAllByEmailConRol(@Param("email") String email);

    /**
     * Si otra cuenta ya <b>verificó</b> ese correo. Es la pregunta que reemplaza al viejo
     * {@code existsByEmailIgnoreCase...}: lo que bloquea un correo es haberlo confirmado, no
     * haberlo tipeado.
     *
     * @param excluirUsuarioId la cuenta que pregunta, para que su propio correo no cuente.
     *                         Puede venir en null (alta, donde todavía no hay cuenta).
     */
    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE lower(u.email) = lower(:email) "
            + "AND u.deleted = false AND u.emailVerificado = true "
            + "AND (:excluirUsuarioId IS NULL OR u.id <> :excluirUsuarioId)")
    boolean existsVerificadoConEmail(
            @Param("email") String email, @Param("excluirUsuarioId") UUID excluirUsuarioId);

    /**
     * Login por DNI (nota de credenciales de la epica E1A: "el modulo de Autenticacion admite
     * ademas el ingreso por DNI"). Mismo JOIN FETCH y misma razon que {@link #findByEmailConRol}.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.dni = :dni AND u.deleted = false")
    Optional<Usuario> findByDniConRol(@Param("dni") String dni);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByDniAndDeletedFalse(String dni);

    /** El mismo chequeo, sin contar a la propia cuenta: para editar el perfil. */
    boolean existsByDniAndIdNotAndDeletedFalse(String dni, UUID id);

    List<Usuario> findAllByOrderByCreatedAtDesc();
}
