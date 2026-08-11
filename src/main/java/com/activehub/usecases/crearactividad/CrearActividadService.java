package com.activehub.usecases.crearactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearActividadService {

    private final ActividadRepository actividadRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;

    public CrearActividadService(
            ActividadRepository actividadRepository,
            TipoActividadRepository tipoActividadRepository,
            UsuarioRepository usuarioRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            AuditService auditService
    ) {
        this.actividadRepository = actividadRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CrearActividadResponse crear(CrearActividadRequest request, UUID instructorId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(instructorId)
                .orElseThrow(() -> new SinPermisoException(
                        "Tu perfil de instructor todavía no fue aprobado. No podés crear actividades hasta que un administrador lo valide."));
        if (perfil.getEstadoVerificacion() != EstadoVerificacion.APROBADO) {
            throw new SinPermisoException(
                    "Tu perfil de instructor todavía no fue aprobado. No podés crear actividades hasta que un administrador lo valide.");
        }

        TipoActividad tipo = tipoActividadRepository.findById(request.tipoActividadId())
                .orElseThrow(() -> new NoEncontradoException("Tipo de actividad no encontrado."));

        Usuario instructor = usuarioRepository.findById(instructorId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        Actividad actividad = new Actividad();
        actividad.setNombre(request.nombre().trim());
        actividad.setDescripcion(request.descripcion().trim());
        actividad.setTipoActividad(tipo);
        actividad.setNivelIntensidad(NivelIntensidad.fromEtiqueta(request.nivelIntensidad()));
        actividad.setInstructor(instructor);
        actividad.setPrecio(request.precio());
        actividad.setUbicacion(request.ubicacion().trim());
        actividad.setPhotoTint(request.photoTint());
        actividad.setCuposMax(request.cuposMax());
        actividad = actividadRepository.saveAndFlush(actividad);

        auditService.registrar(instructorId, AuditAccion.ACTIVIDAD_CREADA, "Actividad", actividad.getId(), null);

        return new CrearActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                new CrearActividadResponse.TipoActividad(tipo.getId(), tipo.getNombre()),
                new CrearActividadResponse.Categoria(tipo.getCategoria().getId(), tipo.getCategoria().getNombre()),
                actividad.getNivelIntensidad().getEtiqueta(),
                new CrearActividadResponse.Instructor(instructor.getId(), instructor.getNombre(), instructor.getApellido()),
                actividad.getPrecio(),
                actividad.getUbicacion(),
                actividad.getPhotoTint(),
                actividad.getRating(),
                actividad.getCuposMax(),
                List.of()
        );
    }
}
