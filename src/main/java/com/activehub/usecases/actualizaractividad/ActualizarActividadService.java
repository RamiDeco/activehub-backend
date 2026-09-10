package com.activehub.usecases.actualizaractividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarActividadService {

    private final ActividadRepository actividadRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;

    public ActualizarActividadService(
            ActividadRepository actividadRepository,
            TipoActividadRepository tipoActividadRepository,
            NivelIntensidadRepository nivelIntensidadRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            AuditService auditService
    ) {
        this.actividadRepository = actividadRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarActividadResponse actualizar(UUID id, ActualizarActividadRequest request, UUID instructorId) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!actividad.getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés modificar una actividad que no te pertenece.");
        }

        boolean aprobado = perfilInstructorRepository.findByUsuarioId(instructorId)
                .map(p -> p.getEstadoVerificacion() == EstadoVerificacion.APROBADO)
                .orElse(false);
        if (!aprobado) {
            throw new SinPermisoException(
                    "Tu perfil de instructor todavía no fue aprobado. No podés editar actividades hasta que un administrador lo valide.");
        }

        TipoActividad tipo = tipoActividadRepository.findById(request.tipoActividadId())
                .orElseThrow(() -> new NoEncontradoException("Tipo de actividad no encontrado."));

        NivelIntensidad nivel = nivelIntensidadRepository.findById(request.nivelIntensidadId())
                .orElseThrow(() -> new NoEncontradoException("Nivel de intensidad no encontrado."));

        if ((request.latitud() == null) != (request.longitud() == null)) {
            throw new ValidacionException("Latitud y longitud deben enviarse juntas, o ninguna de las dos.");
        }

        actividad.setNombre(request.nombre().trim());
        actividad.setDescripcion(request.descripcion().trim());
        actividad.setTipoActividad(tipo);
        actividad.setNivelIntensidad(nivel);
        actividad.setPrecio(request.precio());
        actividad.setUbicacion(request.ubicacion().trim());
        actividad.setPhotoTint(request.photoTint());
        actividad.setDuracionMin(request.duracionMin());
        actividad.setLatitud(request.latitud());
        actividad.setLongitud(request.longitud());
        actividadRepository.save(actividad);

        auditService.registrar(instructorId, AuditAccion.ACTIVIDAD_ACTUALIZADA, "Actividad", actividad.getId(), null);

        return new ActualizarActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                new ActualizarActividadResponse.TipoActividad(tipo.getId(), tipo.getNombre()),
                new ActualizarActividadResponse.Categoria(tipo.getCategoria().getId(), tipo.getCategoria().getNombre()),
                new ActualizarActividadResponse.NivelIntensidad(nivel.getId(), nivel.getNombre()),
                new ActualizarActividadResponse.Instructor(
                        actividad.getInstructor().getId(), actividad.getInstructor().getNombre(), actividad.getInstructor().getApellido()),
                actividad.getPrecio(),
                actividad.getUbicacion(),
                actividad.getPhotoTint(),
                actividad.getRating(),
                actividad.getDuracionMin(),
                actividad.getLatitud(),
                actividad.getLongitud()
        );
    }
}
