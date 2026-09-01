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

    @Column(name = "nivel_intensidad", nullable = false, length = 20)
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

    @Column(name = "cupos_max", nullable = false)
    private int cuposMax;

    @Column
    private Double latitud;

    @Column
    private Double longitud;

    @Column(name = "foto_path")
    private String fotoPath;
}
