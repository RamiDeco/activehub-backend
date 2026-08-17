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

    @Column(name = "en_moderacion", nullable = false)
    private boolean enModeracion = true;
}
