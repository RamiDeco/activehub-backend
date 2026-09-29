package com.activehub.shared.notificacion;

import com.activehub.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * No extiende BaseEntity: es de solo-append como AuditLog/Penalizacion, con
 * la unica excepcion de que "leida" se actualiza in-place al consultarla.
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "entidad_id")
    private UUID entidadId;

    /**
     * A donde lleva el click. Nunca nulo: lo que no tiene pantalla propia se guarda como
     * {@link DestinoNotificacion#NINGUNO} y el frontend lo muestra sin link.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "destino_tipo", nullable = false, length = 30)
    private DestinoNotificacion destinoTipo = DestinoNotificacion.NINGUNO;

    /** El parametro de ruta de esa pantalla, ya resuelto. Nulo si el destino es NINGUNO. */
    @Column(name = "destino_id")
    private UUID destinoId;

    @Column(nullable = false)
    private boolean leida = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notificacion(Usuario usuario, TipoNotificacion tipo, String mensaje, UUID entidadId, Destino destino) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.entidadId = entidadId;
        this.destinoTipo = destino.tipo();
        this.destinoId = destino.id();
    }
}
