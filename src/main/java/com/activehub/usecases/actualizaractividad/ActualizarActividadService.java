package com.activehub.usecases.actualizaractividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.domain.inscripcion.VentanaInscripcion;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarActividadService {

    private final ActividadRepository actividadRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final ClaseRepository claseRepository;
    private final AuditService auditService;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;
    private final Clock clock;

    public ActualizarActividadService(
            ActividadRepository actividadRepository,
            TipoActividadRepository tipoActividadRepository,
            NivelIntensidadRepository nivelIntensidadRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            ClaseRepository claseRepository,
            AuditService auditService,
            PenalizacionVigenteGuard penalizacionVigenteGuard,
            Clock clock
    ) {
        this.actividadRepository = actividadRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.claseRepository = claseRepository;
        this.auditService = auditService;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.clock = clock;
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

        // Una suspension vigente corta la operacion, no la sesion: el penalizado entra y ve
        // lo suyo, pero no publica ni modifica oferta mientras dure la sancion.
        penalizacionVigenteGuard.exigirSinSuspensionVigente(instructorId, "editar actividades");

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

        propagarPrecioAClasesNoCongeladas(actividad);

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

    /**
     * El precio nuevo baja a las clases futuras, pero <b>nunca a una clase congelada</b>: esa
     * ya tiene gente anotada que pago el precio anterior (V23).
     *
     * <p>Antes no hacia falta propagar nada porque la clase no tenia precio y el cobro lo leia
     * de la actividad — que es justamente lo que estaba mal: editar el precio se lo cambiaba
     * retroactivamente a todas las clases, incluidas las que ya se estaban vendiendo.
     */
    private void propagarPrecioAClasesNoCongeladas(Actividad actividad) {
        Instant ahora = Instant.now(clock);
        List<Clase> clases = claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(
                actividad.getId(), EnumSet.of(EstadoClase.Cancelada, EstadoClase.Finalizada));
        for (Clase clase : clases) {
            if (VentanaInscripcion.estaCongelada(ahora, clase.getFechaHora(), clase.getCuposOcupados())) {
                continue;
            }
            if (clase.getPrecio() == null || clase.getPrecio().compareTo(actividad.getPrecio()) != 0) {
                clase.setPrecio(actividad.getPrecio());
                claseRepository.save(clase);
            }
        }
    }
}
