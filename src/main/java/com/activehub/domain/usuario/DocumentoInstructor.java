package com.activehub.domain.usuario;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Tabla preparada para un slice futuro (verificacion de documentacion del instructor).
 * No se usa desde ningun caso de uso de Auth todavia.
 */
@Entity
@Table(name = "documento_instructor")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoInstructor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_instructor_id", nullable = false)
    private PerfilInstructor perfilInstructor;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    private String tipoDocumento;

    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";
}
