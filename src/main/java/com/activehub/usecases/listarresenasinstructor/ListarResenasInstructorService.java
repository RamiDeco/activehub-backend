package com.activehub.usecases.listarresenasinstructor;

import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarResenasInstructorService {

    private final ReseniaRepository reseniaRepository;
    private final DenunciaRepository denunciaRepository;

    public ListarResenasInstructorService(
            ReseniaRepository reseniaRepository, DenunciaRepository denunciaRepository) {
        this.reseniaRepository = reseniaRepository;
        this.denunciaRepository = denunciaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarResenasInstructorResponse> listar(UUID instructorId) {
        List<Resenia> resenias = reseniaRepository.findByInstructorIdConDetalle(instructorId);

        // Una sola consulta para todas: `enModeracion` (pendiente de aprobación) y `denunciada`
        // son cosas distintas, y la UI las pintaba como si fueran lo mismo.
        List<UUID> ids = resenias.stream().map(Resenia::getId).toList();
        Set<UUID> denunciadas = ids.isEmpty()
                ? Set.of()
                : Set.copyOf(denunciaRepository.findReseniaIdsDenunciadas(ids, EstadoDenuncia.RESUELTA));

        return resenias.stream()
                .map(r -> {
                    var clase = r.getClase();
                    var actividad = clase.getActividad();
                    var alumno = r.getAlumno();
                    return new ListarResenasInstructorResponse(
                            r.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            new ListarResenasInstructorResponse.Alumno(alumno.getId(), alumno.getNombre(), alumno.getApellido()),
                            r.getPuntaje(),
                            r.getComentario(),
                            r.isEnModeracion(),
                            r.getRespuestaInstructor(),
                            r.getRespuestaInstructorAt(),
                            denunciadas.contains(r.getId()),
                            r.isOculta(),
                            r.getCreatedAt());
                })
                .toList();
    }
}
