package com.activehub.usecases.listarinscripcionesadmin;

import com.activehub.domain.inscripcion.InscripcionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarInscripcionesAdminService {

    private final InscripcionRepository inscripcionRepository;

    public ListarInscripcionesAdminService(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarInscripcionesAdminResponse> listar() {
        return inscripcionRepository.findAllConDetalle().stream()
                .map(i -> {
                    var pago = i.getPago();
                    var pagoDto = pago != null
                            ? new ListarInscripcionesAdminResponse.Pago(
                                    pago.getId(), pago.getEstado().name(), pago.getMonto(), pago.getMetodo().getEtiqueta())
                            : null;

                    return new ListarInscripcionesAdminResponse(
                            i.getId(),
                            i.getClase().getId(),
                            i.getClase().getActividad().getId(),
                            i.getAlumno().getId(),
                            i.getEstado().getEtiqueta(),
                            i.getCreatedAt(),
                            pagoDto);
                })
                .toList();
    }
}
