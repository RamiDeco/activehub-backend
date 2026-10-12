package com.activehub.usecases.listarreseniaspublicadas;

import com.activehub.domain.resenia.ReseniaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las resenias ya aprobadas, para que el admin pueda moderarlas DESPUES de publicadas.
 *
 * <p>{@code listarresenaspendientes} solo devuelve las que estan en la cola
 * ({@code enModeracion = true}), asi que una resenia impropia que se colo en la moderacion
 * desaparecia del alcance del admin. Ver {@code ocultarresenia}.
 */
@Service
public class ListarReseniasPublicadasService {

    private final ReseniaRepository reseniaRepository;

    public ListarReseniasPublicadasService(ReseniaRepository reseniaRepository) {
        this.reseniaRepository = reseniaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarReseniasPublicadasResponse> listar() {
        return reseniaRepository.findPublicadasConDetalle().stream()
                .map(r -> {
                    var clase = r.getClase();
                    var actividad = clase.getActividad();
                    var alumno = r.getAlumno();
                    return new ListarReseniasPublicadasResponse(
                            r.getId(),
                            clase.getId(),
                            clase.getFechaHora(),
                            actividad.getId(),
                            actividad.getNombre(),
                            new ListarReseniasPublicadasResponse.Alumno(
                                    alumno.getId(), alumno.getNombre(), alumno.getApellido()),
                            r.getPuntaje(),
                            r.getComentario(),
                            r.isOculta(),
                            r.getCreatedAt());
                })
                .toList();
    }
}
