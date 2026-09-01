package com.activehub.usecases.descargardocumentoinstructor;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescargarDocumentoInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final String directorioAlmacenamiento;

    public DescargarDocumentoInstructorService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            @Value("${app.storage.documentos-instructor-dir}") String directorioAlmacenamiento
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
    }

    @Transactional(readOnly = true)
    public DocumentoDescarga descargar(UUID instructorId, UUID documentoId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        DocumentoInstructor documento = documentoInstructorRepository
                .findByIdAndPerfilInstructorId(documentoId, perfil.getId())
                .orElseThrow(() -> new NoEncontradoException("Documento no encontrado."));

        try {
            byte[] contenido = Files.readAllBytes(Path.of(directorioAlmacenamiento).resolve(documento.getRutaArchivo()));
            return new DocumentoDescarga(documento.getNombreArchivo(), documento.getTipoDocumento(), contenido);
        } catch (IOException e) {
            throw new NoEncontradoException("No pudimos leer el archivo.");
        }
    }
}
