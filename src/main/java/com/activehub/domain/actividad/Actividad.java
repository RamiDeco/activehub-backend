package com.activehub.domain.actividad;

import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "actividad")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Actividad extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_actividad_id", nullable = false)
    private TipoActividad tipoActividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_intensidad_id", nullable = false)
    private NivelIntensidad nivelIntensidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Usuario instructor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 255)
    private String ubicacion;

    @Column(name = "photo_tint", nullable = false, length = 255)
    private String photoTint;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    // E2I-HU03 criterio 1: el formulario ya pedia la duracion y no se guardaba.
    @Column(name = "duracion_min", nullable = false)
    private int duracionMin = 60;

    @Column
    private Double latitud;

    @Column
    private Double longitud;

    // Portada. La galeria completa (seccion 2: `imagenes[]`) vive en ActividadImagen.
    @Column(name = "foto_path")
    private String fotoPath;
}
