package com.activehub.domain.penalizacion;

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
 * Comprobante de la transferencia con la que el instructor pagó una multa (V36). No extiende
 * BaseEntity: no se edita ni se da de baja, y un comprobante inválido queda como historial
 * junto al siguiente que se envíe. El archivo vive en {@code CarpetaArchivos.COMPROBANTES_PENALIZACION}
 * y se sirve por la API, nunca por una URL pública.
 */
@Entity
@Table(name = "comprobante_penalizacion")
@Getter
@Setter
@NoArgsConstructor
public class ComprobantePenalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalizacion_id", nullable = false)
    private Penalizacion penalizacion;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoComprobantePenalizacion estado = EstadoComprobantePenalizacion.EN_REVISION;

    @Column(name = "motivo_invalidez", columnDefinition = "TEXT")
    private String motivoInvalidez;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;

    @Column(name = "revisado_at")
    private Instant revisadoAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
