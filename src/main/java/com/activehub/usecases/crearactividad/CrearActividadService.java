package com.activehub.usecases.crearactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearActividadService {

    private final ActividadRepository actividadRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;

    public CrearActividadService(
            ActividadRepository actividadRepository,
            TipoActividadRepository tipoActividadRepository,
            NivelIntensidadRepository nivelIntensidadRepository,
            UsuarioRepository usuarioRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            AuditService auditService,
            PenalizacionVigenteGuard penalizacionVigenteGuard
    ) {
        this.actividadRepository = actividadRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
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

        // Una suspension vigente corta la operacion, no la sesion: el penalizado entra y ve
        // lo suyo, pero no publica ni modifica oferta mientras dure la sancion.
        penalizacionVigenteGuard.exigirSinSuspensionVigente(instructorId, "publicar actividades");

        TipoActividad tipo = tipoActividadRepository.findById(request.tipoActividadId())
                .orElseThrow(() -> new NoEncontradoException("Tipo de actividad no encontrado."));

        NivelIntensidad nivel = nivelIntensidadRepository.findById(request.nivelIntensidadId())
                .orElseThrow(() -> new NoEncontradoException("Nivel de intensidad no encontrado."));

        Usuario instructor = usuarioRepository.findById(instructorId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        if ((request.latitud() == null) != (request.longitud() == null)) {
            throw new ValidacionException("Latitud y longitud deben enviarse juntas, o ninguna de las dos.");
        }

        Actividad actividad = new Actividad();
        actividad.setNombre(request.nombre().trim());
        actividad.setDescripcion(request.descripcion().trim());
        actividad.setTipoActividad(tipo);
        actividad.setNivelIntensidad(nivel);
        actividad.setInstructor(instructor);
        actividad.setPrecio(request.precio());
        actividad.setUbicacion(request.ubicacion().trim());
        actividad.setPhotoTint(request.photoTint());
        actividad.setDuracionMin(request.duracionMin());
        actividad.setLatitud(request.latitud());
        actividad.setLongitud(request.longitud());
        actividad = actividadRepository.saveAndFlush(actividad);

        auditService.registrar(instructorId, AuditAccion.ACTIVIDAD_CREADA, "Actividad", actividad.getId(), null);

        return new CrearActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                new CrearActividadResponse.TipoActividad(tipo.getId(), tipo.getNombre()),
                new CrearActividadResponse.Categoria(tipo.getCategoria().getId(), tipo.getCategoria().getNombre()),
                new CrearActividadResponse.NivelIntensidad(nivel.getId(), nivel.getNombre()),
                new CrearActividadResponse.Instructor(instructor.getId(), instructor.getNombre(), instructor.getApellido()),
                actividad.getPrecio(),
                actividad.getUbicacion(),
                actividad.getPhotoTint(),
                actividad.getRating(),
                actividad.getDuracionMin(),
                List.of(),
                actividad.getLatitud(),
                actividad.getLongitud()
        );
    }
}
