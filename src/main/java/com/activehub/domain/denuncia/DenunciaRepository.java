package com.activehub.domain.denuncia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

    boolean existsByClaseIdAndAlumnoId(UUID claseId, UUID alumnoId);

    /** Denuncias abiertas (no Resueltas) sobre una clase: retienen la liberacion del pago. */
    boolean existsByClaseIdAndEstadoNot(UUID claseId, EstadoDenuncia estado);

    @Query("SELECT d FROM Denuncia d "
            + "JOIN FETCH d.alumno "
            + "JOIN FETCH d.clase c JOIN FETCH c.actividad a JOIN FETCH a.instructor "
            + "ORDER BY d.createdAt DESC")
    List<Denuncia> findAllConDetalle();

    @Query("SELECT d FROM Denuncia d "
            + "JOIN FETCH d.clase c JOIN FETCH c.actividad a "
            + "WHERE d.alumno.id = :alumnoId "
            + "ORDER BY d.createdAt DESC")
    List<Denuncia> findByAlumnoIdConDetalle(UUID alumnoId);
}
