package com.activehub.domain.actividad;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Programacion recurrente de una actividad (seccion 2 de la especificacion).
 *
 * <p>E2I-HU06 criterio 6: cuando el instructor tilda "Repetir cada semana" no se generan N
 * clases de una, se guarda esta agenda y {@code MaterializarAgendasScheduler} crea cada
 * {@link Clase} <b>una semana antes</b> de su dictado, para que se la pueda dar de baja con
 * anticipacion. Sin esta entidad la recurrencia no existia: el check del modal no hacia nada.
 */
@Entity
@Table(name = "agenda_clases")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class AgendaClases extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    /** 1 = lunes … 7 = domingo, igual que {@link DayOfWeek#getValue()}. */
    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    // La spec define el rango etario en la agenda, pero ninguna HU lo usa todavia
    // (esta anotado como hueco conocido en la propia especificacion, punto 8).
    @Column(name = "edad_min")
    private Integer edadMin;

    @Column(name = "edad_max")
    private Integer edadMax;

    @Column(name = "cupos_max", nullable = false)
    private int cuposMax;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    /** Null = sin fecha de corte: la agenda sigue generando clases indefinidamente. */
    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    public DayOfWeek getDiaSemanaEnum() {
        return DayOfWeek.of(diaSemana);
    }

    public void setDiaSemanaEnum(DayOfWeek dia) {
        this.diaSemana = (short) dia.getValue();
    }

    public boolean vigenteEn(LocalDate fecha) {
        if (fecha.isBefore(vigenciaDesde)) {
            return false;
        }
        return vigenciaHasta == null || !fecha.isAfter(vigenciaHasta);
    }
}
