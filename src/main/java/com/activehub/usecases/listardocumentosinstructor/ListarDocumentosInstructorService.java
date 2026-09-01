package com.activehub.usecases.listardocumentosinstructor;

import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarDocumentosInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;

    public ListarDocumentosInstructorService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarDocumentosInstructorResponse> listar(UUID instructorId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        return documentoInstructorRepository.findByPerfilInstructorIdOrderByCreatedAtDesc(perfil.getId()).stream()
                .map(d -> new ListarDocumentosInstructorResponse(
                        d.getId(), d.getNombreArchivo(), d.getTipoDocumento(), d.getTamanioBytes(), d.getCreatedAt()))
                .toList();
    }
}
