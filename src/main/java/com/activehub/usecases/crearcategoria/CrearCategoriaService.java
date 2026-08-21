package com.activehub.usecases.crearcategoria;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaDuplicadaException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearCategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final AuditService auditService;

    public CrearCategoriaService(CategoriaRepository categoriaRepository, AuditService auditService) {
        this.categoriaRepository = categoriaRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CrearCategoriaResponse crear(CrearCategoriaRequest request, UUID actorId) {
        if (categoriaRepository.existsByNombreIgnoreCaseAndDeletedFalse(request.nombre())) {
            throw new CategoriaDuplicadaException();
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(request.nombre().trim());
        categoria = categoriaRepository.saveAndFlush(categoria);

        auditService.registrar(actorId, AuditAccion.CATEGORIA_CREADA, "Categoria", categoria.getId(), null);

        return new CrearCategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
