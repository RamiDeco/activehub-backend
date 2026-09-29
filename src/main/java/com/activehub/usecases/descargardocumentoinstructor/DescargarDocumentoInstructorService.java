package com.activehub.usecases.descargardocumentoinstructor;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescargarDocumentoInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final AlmacenamientoArchivos almacenamiento;

    public DescargarDocumentoInstructorService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public DocumentoDescarga descargar(UUID instructorId, UUID documentoId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        DocumentoInstructor documento = documentoInstructorRepository
                .findByIdAndPerfilInstructorId(documentoId, perfil.getId())
                .orElseThrow(() -> new NoEncontradoException("Documento no encontrado."));

        try {
            byte[] contenido = almacenamiento.leer(
                    CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, documento.getRutaArchivo());
            return new DocumentoDescarga(documento.getNombreArchivo(), documento.getTipoDocumento(), contenido);
        } catch (AlmacenamientoException e) {
            throw new NoEncontradoException("No pudimos leer el archivo.");
        }
    }
}
