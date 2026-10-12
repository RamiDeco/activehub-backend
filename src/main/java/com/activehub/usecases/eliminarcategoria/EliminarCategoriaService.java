package com.activehub.usecases.eliminarcategoria;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de una Categoría <b>en cascada sobre sus Tipos de actividad</b>.
 *
 * <p>Antes, tener un solo Tipo colgando bloqueaba la baja. Eso dejaba al administrador
 * borrando los Tipos uno por uno para poder borrar la Categoría que los agrupa, aunque
 * ninguno se estuviera usando: un Tipo sin actividades no es un dato que haya que
 * preservar, es parte de la taxonomía que se está dando de baja.
 *
 * <p>Lo que sí bloquea es lo que tiene datos reales detrás: si <b>alguno</b> de los Tipos
 * tiene una Actividad publicada, no se borra nada. Es la misma regla de
 * {@code eliminartipoactividad}, elevada un nivel — quien decide es la actividad, no el Tipo.
 */
@Service
public class EliminarCategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public EliminarCategoriaService(
            CategoriaRepository categoriaRepository,
            TipoActividadRepository tipoActividadRepository,
            ActividadRepository actividadRepository,
            AuditService auditService
    ) {
        this.categoriaRepository = categoriaRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID actorId) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada."));

        List<TipoActividad> tipos = tipoActividadRepository.findByCategoriaIdAndDeletedFalseOrderByNombreAsc(id);

        // Primero se revisan TODOS y recién después se borra: con la comprobación intercalada,
        // un segundo tipo en uso dejaba al primero ya dado de baja dentro de la transacción.
        List<String> enUso = tipos.stream()
                .filter(t -> actividadRepository.existsByTipoActividadIdAndDeletedFalse(t.getId()))
                .map(TipoActividad::getNombre)
                .toList();
        if (!enUso.isEmpty()) {
            throw new CategoriaEnUsoException(
                    "No se puede eliminar la categoría \"" + categoria.getNombre() + "\": "
                            + (enUso.size() == 1 ? "el tipo de actividad " : "los tipos de actividad ")
                            + String.join(", ", enUso)
                            + (enUso.size() == 1 ? " tiene" : " tienen")
                            + " actividades asociadas.");
        }

        for (TipoActividad tipo : tipos) {
            tipo.marcarBorrado();
            tipoActividadRepository.save(tipo);
            auditService.registrar(
                    actorId, AuditAccion.TIPO_ACTIVIDAD_ELIMINADO, "TipoActividad", tipo.getId(),
                    "baja en cascada de la categoría " + categoria.getNombre());
        }

        categoria.marcarBorrado();
        categoriaRepository.save(categoria);

        auditService.registrar(
                actorId, AuditAccion.CATEGORIA_ELIMINADA, "Categoria", id,
                tipos.isEmpty() ? null : "tipos eliminados en cascada: " + tipos.size());
    }
}
