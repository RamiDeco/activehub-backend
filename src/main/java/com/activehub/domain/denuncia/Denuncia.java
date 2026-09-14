package com.activehub.domain.denuncia;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.resenia.Resenia;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * No extiende BaseEntity: "Resuelta" ya es un estado terminal de negocio que
 * cumple el rol del soft-delete, mismo criterio que Inscripcion.
 */
@Entity
@Table(name = "denuncia")
@Getter
@Setter
@NoArgsConstructor
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Objeto denunciado. Exactamente uno de {@code clase} / {@code resenia} viene cargado
     * (lo garantiza el CHECK {@code ck_denuncia_objeto}): el alumno denuncia una clase
     * (E3A-HU11) y el instructor denuncia una resenia (E2I-HU11 criterio 4). Antes
     * {@code clase_id} era NOT NULL, asi que lo segundo no se podia ni guardar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_id")
    private Clase clase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resenia_id")
    private Resenia resenia;

    /** Quien denuncia. En las denuncias de clase coincide con {@link #alumno}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    /** Solo en las denuncias de clase: el alumno que la hizo. Null si se denuncio una resenia. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Usuario alumno;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(nullable = false, length = 20)
    private EstadoDenuncia estado = EstadoDenuncia.PENDIENTE;

    /**
     * Como la cerro el admin (E3A-HU11 criterios 2 y 7: el alumno tiene que poder ver el
     * resultado). Antes la denuncia pasaba a Resuelta sin dejar rastro de que se decidio.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResolucionDenuncia resolucion;

    /** Texto libre que el admin le deja al denunciante junto con la resolucion. */
    @Column(columnDefinition = "TEXT")
    private String detalle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
