package com.activehub.domain.actividad;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "clase")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Clase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    // E2I-HU06 criterios 1 y 4: el modal pide "Hora fin*" y valida fin > inicio.
    @Column(name = "hora_fin", nullable = false)
    private Instant horaFin;

    // Null si la clase se creo suelta; apunta a la agenda que la genero si es recurrente.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agenda_clases_id")
    private AgendaClases agendaClases;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoClase estado = EstadoClase.Programada;

    @Column(name = "cupos_max", nullable = false)
    private int cuposMax;

    @Column(name = "cupos_ocupados", nullable = false)
    private int cuposOcupados = 0;

    /**
     * Precio de ESTA clase, copiado de la actividad al crearla (V23).
     *
     * <p>No es una desnormalizacion por performance: es el precio que se le prometio a quien
     * ya se anoto. Antes el cobro leia {@code clase.getActividad().getPrecio()} en el momento
     * de inscribirse, asi que editar la actividad cambiaba retroactivamente el precio de las
     * clases en curso y dos alumnos de la misma clase podian pagar distinto. Editar la
     * actividad ahora solo propaga a las clases NO congeladas
     * ({@code VentanaInscripcion.estaCongelada}).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
}
