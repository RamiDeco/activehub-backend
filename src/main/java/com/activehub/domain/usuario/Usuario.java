package com.activehub.domain.usuario;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "usuario")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 255)
    private String email;

    /**
     * Si el dueño confirmó este correo con el código de 6 dígitos.
     *
     * <p><b>Es lo que reserva el correo.</b> El índice único de {@code usuario(lower(email))}
     * es parcial: sólo alcanza a las cuentas con {@code email_verificado = true} (V26). Un
     * correo sin confirmar no bloquea a nadie — otra persona puede registrarse con el mismo —,
     * y recién al confirmarlo queda tomado. Antes la unicidad era sobre toda cuenta viva, así
     * que equivocarse al tipear el correo de un tercero se lo inutilizaba para siempre.
     */
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    @Column(name = "email_verificado_at")
    private java.time.Instant emailVerificadoAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_proveedor", nullable = false, length = 20)
    private AuthProveedor authProveedor = AuthProveedor.LOCAL;

    // Credencial alternativa de login y clave de unicidad junto al email
    // (precondicion de E1A-HU03 y E1A-HU04). Nullable: las cuentas viejas no lo tienen.
    @Column(length = 20)
    private String dni;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 30)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "cantidad_penalizaciones", nullable = false)
    private int cantidadPenalizaciones = 0;

    @Column(name = "foto_path")
    private String fotoPath;
}
