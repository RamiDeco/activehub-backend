package com.activehub.usecases.actualizarcategoria;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaDuplicadaException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarCategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final AuditService auditService;

    public ActualizarCategoriaService(CategoriaRepository categoriaRepository, AuditService auditService) {
        this.categoriaRepository = categoriaRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarCategoriaResponse actualizar(UUID id, ActualizarCategoriaRequest request, UUID actorId) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada."));

        categoriaRepository.findByNombreIgnoreCaseAndDeletedFalse(request.nombre())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new CategoriaDuplicadaException();
                });

        categoria.setNombre(request.nombre().trim());
        categoriaRepository.save(categoria);

        auditService.registrar(actorId, AuditAccion.CATEGORIA_ACTUALIZADA, "Categoria", categoria.getId(), null);

        return new ActualizarCategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
