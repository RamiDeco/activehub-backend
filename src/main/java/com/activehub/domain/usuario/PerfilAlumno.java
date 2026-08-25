package com.activehub.domain.usuario;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_alumno_interes", joinColumns = @JoinColumn(name = "perfil_alumno_id"))
    @Column(name = "interes", nullable = false, length = 100)
    private List<String> intereses = new ArrayList<>();

    @Column(name = "condicion_salud", columnDefinition = "TEXT")
    private String condicionSalud;

    public PerfilAlumno(Usuario usuario, List<String> intereses) {
        this.usuario = usuario;
        this.intereses = intereses != null ? new ArrayList<>(intereses) : new ArrayList<>();
    }
}
