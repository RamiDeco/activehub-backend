package com.activehub.usecases.actualizartipoactividad;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.TipoActividadDuplicadoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarTipoActividadService {

    private final TipoActividadRepository tipoActividadRepository;
    private final CategoriaRepository categoriaRepository;
    private final AuditService auditService;

    public ActualizarTipoActividadService(
            TipoActividadRepository tipoActividadRepository,
            CategoriaRepository categoriaRepository,
            AuditService auditService
    ) {
        this.tipoActividadRepository = tipoActividadRepository;
        this.categoriaRepository = categoriaRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarTipoActividadResponse actualizar(UUID id, ActualizarTipoActividadRequest request, UUID actorId) {
        TipoActividad tipo = tipoActividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Tipo de actividad no encontrado."));

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada."));

        tipoActividadRepository.findByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse(request.nombre(), categoria.getId())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new TipoActividadDuplicadoException();
                });

        tipo.setNombre(request.nombre().trim());
        tipo.setCategoria(categoria);
        tipoActividadRepository.save(tipo);

        auditService.registrar(actorId, AuditAccion.TIPO_ACTIVIDAD_ACTUALIZADO, "TipoActividad", tipo.getId(), null);

        return new ActualizarTipoActividadResponse(tipo.getId(), tipo.getNombre(), categoria.getId());
    }
}
