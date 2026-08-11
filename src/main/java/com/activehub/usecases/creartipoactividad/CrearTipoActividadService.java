package com.activehub.usecases.creartipoactividad;

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
public class CrearTipoActividadService {

    private final TipoActividadRepository tipoActividadRepository;
    private final CategoriaRepository categoriaRepository;
    private final AuditService auditService;

    public CrearTipoActividadService(
            TipoActividadRepository tipoActividadRepository,
            CategoriaRepository categoriaRepository,
            AuditService auditService
    ) {
        this.tipoActividadRepository = tipoActividadRepository;
        this.categoriaRepository = categoriaRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CrearTipoActividadResponse crear(CrearTipoActividadRequest request, UUID actorId) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada."));

        if (tipoActividadRepository.existsByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse(
                request.nombre(), categoria.getId())) {
            throw new TipoActividadDuplicadoException();
        }

        TipoActividad tipo = new TipoActividad();
        tipo.setNombre(request.nombre().trim());
        tipo.setCategoria(categoria);
        tipo = tipoActividadRepository.saveAndFlush(tipo);

        auditService.registrar(actorId, AuditAccion.TIPO_ACTIVIDAD_CREADO, "TipoActividad", tipo.getId(), null);

        return new CrearTipoActividadResponse(tipo.getId(), tipo.getNombre(), categoria.getId());
    }
}
