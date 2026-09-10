package com.activehub.domain.actividad;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * E4Ad-HU05. Era un `enum` de tres valores fijos; la HU pide crear, editar y eliminar
 * niveles desde la pantalla, así que pasa a ser entidad (V21).
 *
 * <p>Es una cosa distinta de {@link Categoria} y de {@link TipoActividad}: la categoría
 * agrupa tipos, el tipo dice QUÉ es la actividad y el nivel dice CUÁNTO esfuerzo pide.
 * La sección 2 del backlog las separa explícitamente.
 */
@Entity
@Table(name = "nivel_intensidad")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class NivelIntensidad extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 300)
    private String descripcion;
}
