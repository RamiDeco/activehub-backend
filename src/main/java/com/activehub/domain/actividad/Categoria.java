package com.activehub.domain.actividad;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "categoria")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Categoria extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String nombre;
}
