package com.activehub.usecases.actualizarmisintereses;

import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intereses del alumno (E3A-HU12 criterio 1: los chips editables de "Intereses deportivos").
 * Hasta V19 eran texto libre y no existia endpoint: el frontend los guardaba en su store local,
 * se perdian al cambiar de navegador y no los veia nadie del lado del servidor.
 *
 * <p>Ahora un interes <b>es</b> un {@code TipoActividad} del catalogo, asi que trae su categoria
 * y el Home puede cruzar por id en vez de comparar texto.
 *
 * <p>Se guarda la lista completa (reemplazo), no un alta/baja por interes: la pantalla edita
 * chips y manda el resultado final, y asi el endpoint es idempotente.
 */
@Service
public class ActualizarMisInteresesService {

    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final AuditService auditService;

    public ActualizarMisInteresesService(
            PerfilAlumnoRepository perfilAlumnoRepository,
            TipoActividadRepository tipoActividadRepository,
            AuditService auditService) {
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarMisInteresesResponse actualizar(UUID usuarioId, ActualizarMisInteresesRequest request) {
        PerfilAlumno perfil = perfilAlumnoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Solo un alumno puede editar sus intereses."));

        // Deduplica manteniendo el orden en que los eligió: la tabla tiene UNIQUE
        // (perfil_alumno_id, tipo_actividad_id) y mandar dos veces el mismo id reventaría.
        Set<UUID> ids = new LinkedHashSet<>(request.tiposActividadId());

        Set<TipoActividad> elegidos = new LinkedHashSet<>();
        for (UUID id : ids) {
            // findById respeta el @SQLRestriction: un tipo dado de baja no se puede elegir.
            TipoActividad tipo = tipoActividadRepository.findById(id)
                    .orElseThrow(() -> new ValidacionException("Uno de los intereses elegidos ya no existe."));
            elegidos.add(tipo);
        }

        perfil.getIntereses().clear();
        perfil.getIntereses().addAll(elegidos);
        perfilAlumnoRepository.save(perfil);

        auditService.registrar(usuarioId, AuditAccion.INTERESES_ACTUALIZADOS, "PerfilAlumno", perfil.getId(), null);

        return new ActualizarMisInteresesResponse(usuarioId, mapear(perfil.getInteresesOrdenados()));
    }

    /** Mismo shape que usa {@code obtenerusuarioactual}, para que el frontend no traduzca dos veces. */
    static List<ActualizarMisInteresesResponse.Interes> mapear(List<TipoActividad> tipos) {
        return tipos.stream()
                .map(t -> new ActualizarMisInteresesResponse.Interes(
                        t.getId(), t.getNombre(), t.getCategoria().getId(), t.getCategoria().getNombre()))
                .toList();
    }
}
