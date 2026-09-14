package com.activehub.domain.denuncia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

    boolean existsByClaseIdAndAlumnoId(UUID claseId, UUID alumnoId);

    /** Denuncias abiertas (no Resueltas) sobre una clase: retienen la liberacion del pago. */
    boolean existsByClaseIdAndEstadoNot(UUID claseId, EstadoDenuncia estado);

    boolean existsByReseniaIdAndDenuncianteId(UUID reseniaId, UUID denuncianteId);

    /** Reseñas con una denuncia todavía abierta, para marcarlas en el listado del instructor. */
    @Query("SELECT d.resenia.id FROM Denuncia d WHERE d.resenia.id IN :reseniaIds AND d.estado <> :estado")
    List<UUID> findReseniaIdsDenunciadas(
            @Param("reseniaIds") List<UUID> reseniaIds, @Param("estado") EstadoDenuncia estado);

    /**
     * LEFT JOIN en todo el camino de la clase a propósito: desde que se puede denunciar una
     * reseña ({@code clase_id} nullable), un INNER JOIN dejaba esas denuncias fuera del listado
     * del admin — existían en la base y no las veía nadie.
     */
    @Query("SELECT d FROM Denuncia d "
            + "JOIN FETCH d.denunciante "
            + "LEFT JOIN FETCH d.alumno "
            + "LEFT JOIN FETCH d.clase c LEFT JOIN FETCH c.actividad a LEFT JOIN FETCH a.instructor "
            + "LEFT JOIN FETCH d.resenia r LEFT JOIN FETCH r.alumno "
            + "LEFT JOIN FETCH r.clase rc LEFT JOIN FETCH rc.actividad ra LEFT JOIN FETCH ra.instructor "
            + "ORDER BY d.createdAt DESC")
    List<Denuncia> findAllConDetalle();

    /** Las que hizo un usuario, sea alumno (clase) o instructor (reseña). */
    @Query("SELECT d FROM Denuncia d "
            + "LEFT JOIN FETCH d.clase c LEFT JOIN FETCH c.actividad "
            + "LEFT JOIN FETCH d.resenia r LEFT JOIN FETCH r.clase rc LEFT JOIN FETCH rc.actividad "
            + "WHERE d.denunciante.id = :denuncianteId "
            + "ORDER BY d.createdAt DESC")
    List<Denuncia> findByDenuncianteIdConDetalle(@Param("denuncianteId") UUID denuncianteId);
}
