package com.activehub.usecases.listarmisclases;

import com.activehub.domain.actividad.ClaseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clases propias del instructor logueado, en orden cronologico y SIN filtrar por estado:
 * incluye Finalizadas y Canceladas para que el instructor pueda ver su historial.
 *
 * <p>Existe porque hasta ahora el unico listado de clases por instructor vivia en
 * {@code listarclasesinstructor}, bajo /api/admin y con @PreAuthorize ADMIN. El instructor
 * no tenia forma de pedir sus propias clases, y por eso el frontend las leia de una cache
 * parcial que solo se llenaba al visitar el detalle de una actividad: al entrar por URL
 * directa o con F5 el Panel, Proximas clases y Metricas mostraban cero.
 */
@Service
public class ListarMisClasesService {

    private final ClaseRepository claseRepository;

    public ListarMisClasesService(ClaseRepository claseRepository) {
        this.claseRepository = claseRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisClasesResponse> listar(UUID instructorId) {
        return claseRepository.findByInstructorIdConDetalle(instructorId).stream()
                .map(c -> {
                    var actividad = c.getActividad();
                    return new ListarMisClasesResponse(
                            c.getId(),
                            actividad.getId(),
                            actividad.getNombre(),
                            actividad.getUbicacion(),
                            c.getFechaHora(),
                            c.getHoraFin(),
                            c.getEstado().name(),
                            c.getCuposMax(),
                            c.getCuposOcupados(),
                            c.getPrecio());
                })
                .toList();
    }
}
