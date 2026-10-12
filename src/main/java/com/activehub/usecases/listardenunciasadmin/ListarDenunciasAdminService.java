package com.activehub.usecases.listardenunciasadmin;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.usuario.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarDenunciasAdminService {

    private final DenunciaRepository denunciaRepository;
    private final InscripcionRepository inscripcionRepository;

    public ListarDenunciasAdminService(DenunciaRepository denunciaRepository, InscripcionRepository inscripcionRepository) {
        this.denunciaRepository = denunciaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarDenunciasAdminResponse> listar() {
        return denunciaRepository.findAllConDetalle().stream()
                .map(d -> d.getResenia() != null ? deResenia(d) : deClase(d))
                .toList();
    }

    private ListarDenunciasAdminResponse deClase(Denuncia d) {
        var clase = d.getClase();
        Actividad actividad = clase.getActividad();
        Usuario instructor = actividad.getInstructor();
        Usuario alumno = d.getAlumno();

        ListarDenunciasAdminResponse.Pago pagoDto = inscripcionRepository
                .findByClaseIdAndAlumnoIdAndEstadoNot(clase.getId(), alumno.getId(), EstadoInscripcion.CANCELADA)
                .map(i -> i.getPago())
                .filter(p -> p != null)
                .map(p -> new ListarDenunciasAdminResponse.Pago(
                        p.getId(), p.getEstado().name(), p.getMonto(), p.getMetodo().getEtiqueta()))
                .orElse(null);

        return new ListarDenunciasAdminResponse(
                d.getId(),
                "CLASE",
                clase.getId(),
                clase.getFechaHora(),
                actividad.getId(),
                actividad.getNombre(),
                persona(alumno),
                persona(instructor),
                persona(d.getDenunciante()),
                null,
                d.getMotivo(),
                d.getEstado().getEtiqueta(),
                d.getResolucion() != null ? d.getResolucion().name() : null,
                d.getDetalle(),
                pagoDto,
                d.getCreatedAt());
    }

    private ListarDenunciasAdminResponse deResenia(Denuncia d) {
        var resenia = d.getResenia();
        var clase = resenia.getClase();
        Actividad actividad = clase.getActividad();

        return new ListarDenunciasAdminResponse(
                d.getId(),
                "RESENIA",
                clase.getId(),
                clase.getFechaHora(),
                actividad.getId(),
                actividad.getNombre(),
                // No hay "alumno denunciado": el autor de la reseña viaja dentro de `resenia`.
                null,
                persona(actividad.getInstructor()),
                persona(d.getDenunciante()),
                new ListarDenunciasAdminResponse.Resenia(
                        resenia.getId(),
                        resenia.getPuntaje(),
                        resenia.getComentario(),
                        persona(resenia.getAlumno()),
                        resenia.isOculta()),
                d.getMotivo(),
                d.getEstado().getEtiqueta(),
                d.getResolucion() != null ? d.getResolucion().name() : null,
                d.getDetalle(),
                // Una denuncia de reseña no mueve plata.
                null,
                d.getCreatedAt());
    }

    private ListarDenunciasAdminResponse.Persona persona(Usuario u) {
        return u == null ? null : new ListarDenunciasAdminResponse.Persona(u.getId(), u.getNombre(), u.getApellido());
    }
}
