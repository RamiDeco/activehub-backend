package com.activehub.domain.permiso;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Accion configurable del sistema (RN-19). El catalogo es fijo: cada {@code clave} se
 * corresponde con una guarda real en el codigo (ver {@code PermisosService}), no con una
 * fila decorativa — si aparece una clave sin guarda, el checkbox miente.
 */
@Entity
@Table(name = "permiso")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Permiso extends BaseEntity {

    /**
     * Permisos que NO son una decision del administrador: los tiene todo rol, presente o
     * futuro, y no aparecen en la lista de seleccion de "Roles y permisos".
     *
     * <p>{@code catalogo.explorar} es el caso: cualquiera que entre a la plataforma explora el
     * catalogo, y la unica guarda que lo usa son los favoritos. Como checkbox solo servia para
     * romper un rol sin querer. Se conserva la clave, la fila y las guardas — lo que cambia es
     * que nadie puede apagarla (ver V22 y {@code ActualizarPermisosRolService}).
     */
    public static final Set<String> IMPLICITOS = Set.of("catalogo.explorar");

    /** Configurable = el administrador decide si lo tiene o no. Los implicitos, no. */
    public boolean esConfigurable() {
        return !IMPLICITOS.contains(clave);
    }

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(nullable = false, length = 120)
    private String accion;

    /** Sin el, la plataforma se queda sin administracion (E4Ad-HU08 criterio 6). */
    @Column(nullable = false)
    private boolean critico;

    @Column(nullable = false)
    private int orden;
}
