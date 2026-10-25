package com.activehub.domain.resenia;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "resenia")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Resenia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    @Column(nullable = false)
    private int puntaje;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comentario;

    /** E2I-HU11 criterio 4: el instructor responde públicamente la reseña. */
    @Column(name = "respuesta_instructor", columnDefinition = "TEXT")
    private String respuestaInstructor;

    @Column(name = "respuesta_instructor_at")
    private Instant respuestaInstructorAt;

    /**
     * Baja del listado público y del rating, sin borrar la reseña: por una denuncia resuelta
     * con {@code OCULTAR_RESENIA} o por {@code ocultarresenia}. Desde V33 una reseña se publica
     * al crearse (no hay moderación previa) y ésta es la única forma de sacarla de circulación.
     */
    @Column(nullable = false)
    private boolean oculta = false;
}
