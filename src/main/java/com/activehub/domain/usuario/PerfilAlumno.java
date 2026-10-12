package com.activehub.domain.usuario;

import com.activehub.domain.actividad.TipoActividad;
import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "perfil_alumno")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class PerfilAlumno extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /**
     * Intereses del alumno. Desde V19 <b>no son texto libre</b>: apuntan a {@link TipoActividad},
     * asi que cada interes trae consigo su categoria (Trekking sabe que es de Aventura) y el
     * "Recomendado para vos" del Home puede cruzarlos por id en vez de comparar strings.
     *
     * <p>La tabla intermedia tiene su propio {@code id} con default en la base: Hibernate solo
     * escribe las dos claves foraneas y el default se encarga del resto.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "perfil_alumno_interes",
            joinColumns = @JoinColumn(name = "perfil_alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_actividad_id")
    )
    private Set<TipoActividad> intereses = new LinkedHashSet<>();

    @Column(name = "condicion_salud", columnDefinition = "TEXT")
    private String condicionSalud;

    public PerfilAlumno(Usuario usuario, List<TipoActividad> intereses) {
        this.usuario = usuario;
        this.intereses = intereses != null ? new LinkedHashSet<>(intereses) : new LinkedHashSet<>();
    }

    /** Los ids de los tipos elegidos, en orden estable. */
    public List<TipoActividad> getInteresesOrdenados() {
        List<TipoActividad> ordenados = new ArrayList<>(intereses);
        ordenados.sort((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
        return ordenados;
    }
}
