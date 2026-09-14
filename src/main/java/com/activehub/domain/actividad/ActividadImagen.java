package com.activehub.domain.actividad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Una imagen de la galeria de una Actividad. La seccion 2 pide {@code imagenes[]} en plural;
 * hasta ahora solo existia {@code actividad.foto_path}, que se conserva como portada.
 *
 * <p>No extiende BaseEntity: borrar una imagen es un borrado real, no hay nada que auditar
 * ni recuperar de una foto quitada de la galeria (el archivo tambien se borra del disco).
 */
@Entity
@Table(name = "actividad_imagen")
@Getter
@Setter
@NoArgsConstructor
public class ActividadImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false)
    private int orden = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
