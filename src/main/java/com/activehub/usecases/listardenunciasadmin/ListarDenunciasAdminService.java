package com.activehub.usecases.listardenunciasadmin;

import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
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
                .map(d -> {
                    var clase = d.getClase();
                    var actividad = clase.getActividad();
                    var instructor = actividad.getInstructor();
                    var alumno = d.getAlumno();

                    ListarDenunciasAdminResponse.Pago pagoDto = inscripcionRepository
                            .findByClaseIdAndAlumnoIdAndEstadoNot(clase.getId(), alumno.getId(), EstadoInscripcion.CANCELADA)
                            .map(i -> i.getPago())
                            .filter(p -> p != null)
                            .map(p -> new ListarDenunciasAdminResponse.Pago(
                                    p.getId(), p.getEstado().name(), p.getMonto(), p.getMetodo().getEtiqueta()))
                            .orElse(null);

                    return new ListarDenunciasAdminResponse(
                            d.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            new ListarDenunciasAdminResponse.Alumno(alumno.getId(), alumno.getNombre(), alumno.getApellido()),
                            new ListarDenunciasAdminResponse.Instructor(instructor.getId(), instructor.getNombre(), instructor.getApellido()),
                            d.getMotivo(),
                            d.getEstado().getEtiqueta(),
                            pagoDto,
                            d.getCreatedAt());
                })
                .toList();
    }
}
