package com.activehub.usecases.descargarmidocumento;

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

/**
 * El instructor abre uno de SUS documentos (2026-10-26). El documento se busca junto con el
 * perfil del que llama: un id ajeno da 404, igual que uno inexistente. Uno retirado también.
 */
@Service
public class DescargarMiDocumentoService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final AlmacenamientoArchivos almacenamiento;

    public DescargarMiDocumentoService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public DescargarMiDocumentoResponse descargar(UUID instructorId, UUID documentoId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("No encontramos tu perfil de instructor."));
        DocumentoInstructor documento = documentoInstructorRepository
                .findByIdAndPerfilInstructorId(documentoId, perfil.getId())
                .filter(d -> !d.isRetirado())
                .orElseThrow(() -> new NoEncontradoException("Documento no encontrado."));
        try {
            byte[] contenido = almacenamiento.leer(CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, documento.getRutaArchivo());
            return new DescargarMiDocumentoResponse(documento.getNombreArchivo(), documento.getTipoDocumento(), contenido);
        } catch (AlmacenamientoException e) {
            throw new NoEncontradoException("No pudimos leer el archivo.");
        }
    }
}
