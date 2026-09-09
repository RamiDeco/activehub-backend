package com.activehub.usecases.listaractividades;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.ActividadSpecifications;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.shared.error.ValidacionException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarActividadesService {

    private static final List<EstadoClase> ESTADOS_EXCLUIDOS = List.of(EstadoClase.Cancelada, EstadoClase.Finalizada);

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;

    public ListarActividadesService(ActividadRepository actividadRepository, ClaseRepository claseRepository) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarActividadesResponse> listar(
            String texto, UUID categoriaId, UUID tipoActividadId, String nivelIntensidad,
            BigDecimal precioMax, boolean soloConCupos, UUID instructorId, String sort) {

        List<Specification<Actividad>> partes = new ArrayList<>();
        // El catálogo público solo muestra oferta de instructores verificados (RN-16).
        partes.add(ActividadSpecifications.deInstructorVerificado());
        partes.add(ActividadSpecifications.conTexto(texto));
        partes.add(ActividadSpecifications.conCategoria(categoriaId));
        partes.add(ActividadSpecifications.conTipo(tipoActividadId));
        partes.add(ActividadSpecifications.conNivel(parseNivelIntensidad(nivelIntensidad)));
        partes.add(ActividadSpecifications.precioMenorIgual(precioMax));
        partes.add(ActividadSpecifications.conInstructor(instructorId));

        Specification<Actividad> spec = Specification.unrestricted();
        for (Specification<Actividad> parte : partes) {
            if (parte != null) {
                spec = spec.and(parte);
            }
        }

        List<Actividad> actividades = actividadRepository.findAll(spec, sortDe(sort));

        List<UUID> ids = actividades.stream().map(Actividad::getId).toList();
        Map<UUID, Clase> proximaPorActividad = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (Clase c : claseRepository.findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(ids, ESTADOS_EXCLUIDOS)) {
                proximaPorActividad.putIfAbsent(c.getActividad().getId(), c);
            }
        }

        return actividades.stream()
                .map(a -> mapear(a, proximaPorActividad.get(a.getId())))
                .filter(dto -> !soloConCupos || tieneCupos(dto))
                .toList();
    }

    private static NivelIntensidad parseNivelIntensidad(String nivelIntensidad) {
        if (nivelIntensidad == null) {
            return null;
        }
        try {
            return NivelIntensidad.fromEtiqueta(nivelIntensidad);
        } catch (IllegalArgumentException ex) {
            throw new ValidacionException("Nivel de intensidad inválido: " + nivelIntensidad);
        }
    }

    private static Sort sortDe(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.ASC, "nombre");
        }
        return switch (sort) {
            case "precio_asc" -> Sort.by(Sort.Direction.ASC, "precio");
            case "precio_desc" -> Sort.by(Sort.Direction.DESC, "precio");
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "rating");
            default -> Sort.by(Sort.Direction.ASC, "nombre");
        };
    }

    private static boolean tieneCupos(ListarActividadesResponse dto) {
        var proxima = dto.proximaClase();
        return proxima == null || (proxima.cuposMax() - proxima.cuposOcupados()) > 0;
    }

    private ListarActividadesResponse mapear(Actividad a, Clase proxima) {
        var tipo = a.getTipoActividad();
        var categoria = tipo.getCategoria();
        var instructor = a.getInstructor();

        var proximaDto = proxima != null
                ? new ListarActividadesResponse.ProximaClase(
                        proxima.getFechaHora(), proxima.getEstado().name(), proxima.getCuposMax(), proxima.getCuposOcupados())
                : null;

        return new ListarActividadesResponse(
                a.getId(),
                a.getNombre(),
                new ListarActividadesResponse.TipoActividad(tipo.getId(), tipo.getNombre()),
                new ListarActividadesResponse.Categoria(categoria.getId(), categoria.getNombre()),
                a.getNivelIntensidad().getEtiqueta(),
                new ListarActividadesResponse.Instructor(instructor.getId(), instructor.getNombre(), instructor.getApellido()),
                a.getPrecio(),
                a.getUbicacion(),
                a.getPhotoTint(),
                a.getRating(),
                a.getCuposMax(),
                proximaDto,
                a.getLatitud(),
                a.getLongitud()
        );
    }
}
