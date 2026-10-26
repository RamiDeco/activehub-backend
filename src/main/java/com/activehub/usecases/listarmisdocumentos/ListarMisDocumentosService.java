package com.activehub.usecases.listarmisdocumentos;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Mis datos" del instructor: la documentación que tiene cargada (2026-10-26). Antes sólo la
 * podía ver el administrador, así que el instructor no sabía qué había mandado ni qué le
 * faltaba. No devuelve los documentos que él mismo retiró: esos son historial del admin.
 */
@Service
public class ListarMisDocumentosService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;

    public ListarMisDocumentosService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisDocumentosResponse> listar(UUID instructorId) {
        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("No encontramos tu perfil de instructor."));
        Instant rechazadoAt = perfil.getRechazadoAt();
        return documentoInstructorRepository
                .findByPerfilInstructorIdAndEstadoNotOrderByCreatedAtDesc(perfil.getId(), DocumentoInstructor.RETIRADO)
                .stream()
                .map(d -> new ListarMisDocumentosResponse(
                        d.getId(),
                        d.getNombreArchivo(),
                        d.getTipoDocumento(),
                        d.getTamanioBytes(),
                        d.getCreatedAt(),
                        rechazadoAt != null && d.getCreatedAt() != null && d.getCreatedAt().isAfter(rechazadoAt)))
                .toList();
    }
}
