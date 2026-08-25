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
 * Documento/certificación subido por un instructor (PDF/JPG/PNG), para que
 * el admin lo revise al validar su solicitud. El archivo en si se guarda en
 * disco (ver "subirdocumento"); esta fila solo referencia dónde quedó.
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

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";
}
