package com.activehub.usecases.eliminarcategoria;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarCategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final AuditService auditService;

    public EliminarCategoriaService(
            CategoriaRepository categoriaRepository,
            TipoActividadRepository tipoActividadRepository,
            AuditService auditService
    ) {
        this.categoriaRepository = categoriaRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID actorId) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada."));

        if (tipoActividadRepository.existsByCategoriaIdAndDeletedFalse(id)) {
            throw new CategoriaEnUsoException();
        }

        categoria.marcarBorrado();
        categoriaRepository.save(categoria);

        auditService.registrar(actorId, AuditAccion.CATEGORIA_ELIMINADA, "Categoria", id, null);
    }
}
