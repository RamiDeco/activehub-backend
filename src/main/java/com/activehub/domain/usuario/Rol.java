package com.activehub.domain.usuario;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Rol de la plataforma. Los tres del sistema (ALUMNO, INSTRUCTOR, ADMIN) son los unicos que
 * conoce el motor de seguridad — {@link #getNombreSistema()} los mapea al enum {@link RolNombre}.
 *
 * <p>El admin puede crear roles adicionales desde "Roles y permisos" (E4Ad-HU08 criterio 3);
 * por eso {@code nombre} es texto libre y no un enum: un rol nuevo nace sin permisos y sin
 * usuarios, y no se puede asignar a una cuenta hasta que exista el flujo que lo permita.
 */
@Entity
@Table(name = "rol")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Rol extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    /** Los roles del sistema no se pueden borrar ni renombrar. */
    @Column(nullable = false)
    private boolean sistema;

    /** El {@link RolNombre} correspondiente, o null si es un rol creado por el admin. */
    public RolNombre getNombreSistema() {
        for (RolNombre valor : RolNombre.values()) {
            if (valor.name().equals(nombre)) {
                return valor;
            }
        }
        return null;
    }
}
