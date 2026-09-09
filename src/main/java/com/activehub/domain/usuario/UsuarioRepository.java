package com.activehub.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

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

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    List<Usuario> findAllByOrderByCreatedAtDesc();
}
