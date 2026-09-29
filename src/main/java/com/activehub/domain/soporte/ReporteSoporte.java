package com.activehub.domain.soporte;

import com.activehub.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Un reporte enviado desde el formulario "Reportar un problema" de la pantalla de Ayuda.
 *
 * <p>No extiende BaseEntity, por el mismo criterio que {@code Denuncia} e {@code Inscripcion}:
 * "Cerrado" ya es un estado terminal de negocio y cumple el rol del soft-delete. Un segundo
 * mecanismo de baja encima solo daria dos formas de que una fila desaparezca.
 *
 * <p><b>El autor es opcional a proposito.</b> {@code /ayuda} es una pantalla publica y el
 * formulario tiene que funcionar para alguien que todavia no tiene cuenta — que es justo quien
 * mas probablemente necesite escribir ("no puedo registrarme"). Por eso {@code usuario} es
 * nullable y el {@code email} de contacto es obligatorio: sin uno de los dos no habria forma
 * de responder.
 */
@Entity
@Table(name = "reporte_soporte")
@Getter
@Setter
@NoArgsConstructor
public class ReporteSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Quien lo envio, si estaba logueado. Null = lo mando un visitante sin cuenta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Siempre presente: es la unica via de respuesta cuando no hay cuenta detras. */
    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false, length = 20)
    private EstadoReporteSoporte estado = EstadoReporteSoporte.ABIERTO;

    /** Lo que el administrador contesto al cerrarlo. Opcional: se puede cerrar sin texto. */
    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por_id")
    private Usuario cerradoPor;

    @Column(name = "cerrado_at")
    private Instant cerradoAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
