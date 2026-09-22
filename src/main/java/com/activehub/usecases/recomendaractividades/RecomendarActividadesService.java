package com.activehub.usecases.recomendaractividades;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.ActividadSpecifications;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.interaccion.InteraccionAlumnoRepository;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de recomendaciones (item 8 del roadmap).
 *
 * <h2>Que reemplaza</h2>
 *
 * Antes "Recomendado para vos" era un filtro en el cliente: las actividades cuyo
 * {@code tipoActividadId} estuviera entre los intereses declarados, <b>en el orden del
 * catalogo</b>, rellenando hasta tres con lo que hubiera. Eso tenia tres problemas y este
 * usecase existe para resolverlos: no puntuaba ni ordenaba, no miraba nada mas que los
 * intereses declarados, y dos alumnos con los mismos intereses veian exactamente lo mismo.
 *
 * <h2>Como funciona</h2>
 *
 * Dos pasos. Primero {@link PerfilDeGustos} resume al alumno en cuatro afinidades normalizadas
 * (tipo, categoria, nivel, instructor) a partir de seis señales. Despues cada actividad
 * candidata recibe un puntaje que suma esas afinidades, ponderadas, mas cuatro factores de la
 * actividad en si: calidad, precio, cercania y disponibilidad.
 *
 * <pre>
 * puntaje = 4,0·afinidadTipo + 2,0·afinidadCategoria + 1,5·afinidadNivel + 1,5·afinidadInstructor
 *         + 1,2·calidad + 0,8·precio + 1,0·cercania + 0,6·disponibilidad
 *         - 1,0 si ya hizo esa misma actividad
 * </pre>
 *
 * Los cuatro factores de la actividad estan todos en [-1, 1], igual que las afinidades, asi que
 * los pesos se leen directamente como importancia relativa: <b>el tipo de actividad pesa mas
 * que todo lo demas junto del lado de la actividad</b>, y eso es deliberado — el alumno busca
 * algo parecido a lo que le gusta, no lo mejor calificado de la plataforma.
 *
 * <h2>Que queda afuera</h2>
 *
 * <ul>
 *   <li><b>Sin señales no se recomienda nada.</b> Si el alumno no declaro intereses y no tiene
 *       historial, la respuesta viene vacia con {@code sinSenales: true}. Rellenar con
 *       cualquier cosa y llamarlo "recomendado" es exactamente la mentira que habia antes.</li>
 *   <li><b>Sus favoritos y las actividades en las que ya esta anotado</b> (clase futura): ya
 *       las tiene, recomendarselas no le agrega nada. Siguen contando como señal de gusto.</li>
 *   <li><b>Oferta de instructores no verificados</b> (RN-16), igual que el catalogo publico.</li>
 *   <li><b>Puntaje negativo</b>: si la unica evidencia es en contra (por ejemplo, reseñas malas
 *       de ese tipo de actividad), no se muestra.</li>
 * </ul>
 *
 * <h2>Aprende</h2>
 *
 * No hay entrenamiento ni modelo: el puntaje se recalcula en cada pedido sobre el estado
 * actual del alumno. Inscribirse, marcar un favorito, calificar, mirar una actividad o buscar
 * algo cambia el resultado del pedido siguiente, y lo viejo pesa cada vez menos (ver el
 * decaimiento de {@link PerfilDeGustos}).
 */
@Service
public class RecomendarActividadesService {

    // --- Pesos del puntaje final ---
    private static final double PESO_TIPO = 4.0;
    private static final double PESO_CATEGORIA = 2.0;
    private static final double PESO_NIVEL = 1.5;
    private static final double PESO_INSTRUCTOR = 1.5;
    private static final double PESO_CALIDAD = 1.2;
    private static final double PESO_PRECIO = 0.8;
    private static final double PESO_CERCANIA = 1.0;
    private static final double PESO_DISPONIBILIDAD = 0.6;
    private static final double PENALIZACION_YA_CURSADA = 1.0;

    private static final List<EstadoClase> ESTADOS_EXCLUIDOS = List.of(EstadoClase.Cancelada, EstadoClase.Finalizada);

    /** Cuantas interacciones se leen. Con el decaimiento, lo anterior ya no mueve la aguja. */
    private static final int TOPE_INTERACCIONES = 200;

    /** Tope duro del pedido: es una lista curada, no un catalogo paginado. */
    private static final int LIMITE_MAXIMO = 20;
    private static final int LIMITE_POR_DEFECTO = 3;

    private static final double RADIO_TIERRA_KM = 6371.0;

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;
    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final FavoritoRepository favoritoRepository;
    private final ReseniaRepository reseniaRepository;
    private final InteraccionAlumnoRepository interaccionRepository;
    private final Clock clock;

    public RecomendarActividadesService(
            ActividadRepository actividadRepository,
            ClaseRepository claseRepository,
            PerfilAlumnoRepository perfilAlumnoRepository,
            InscripcionRepository inscripcionRepository,
            FavoritoRepository favoritoRepository,
            ReseniaRepository reseniaRepository,
            InteraccionAlumnoRepository interaccionRepository,
            Clock clock
    ) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.favoritoRepository = favoritoRepository;
        this.reseniaRepository = reseniaRepository;
        this.interaccionRepository = interaccionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecomendarActividadesResponse recomendar(UUID alumnoId, Double latitud, Double longitud, Integer limite) {
        Instant ahora = clock.instant();

        PerfilDeGustos gustos = new PerfilDeGustos(ahora);
        gustos.agregarIntereses(perfilAlumnoRepository.findByUsuarioId(alumnoId).orElse(null));
        gustos.agregarInscripciones(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null));
        gustos.agregarFavoritos(favoritoRepository.findByUsuarioId(alumnoId));
        gustos.agregarResenias(reseniaRepository.findByAlumnoIdConDetalle(alumnoId));

        // Solo la oferta vigente de instructores verificados, igual que el catalogo publico.
        List<Actividad> candidatas = actividadRepository.findAll(ActividadSpecifications.deInstructorVerificado());

        gustos.agregarInteracciones(
                interaccionRepository.ultimasDelAlumno(alumnoId, Limit.of(TOPE_INTERACCIONES)),
                nombresPorId(candidatas, true),
                nombresPorId(candidatas, false));
        gustos.cerrar();

        if (gustos.sinSeñales()) {
            return new RecomendarActividadesResponse(true, List.of());
        }

        Map<UUID, Clase> proximaPorActividad = proximasClases(candidatas);
        BigDecimal precioTipico = gustos.precioTipico();

        List<Puntuada> puntuadas = new ArrayList<>();
        for (Actividad a : candidatas) {
            if (gustos.estaExcluida(a.getId())) {
                continue;
            }
            Puntuada puntuada = puntuar(a, gustos, proximaPorActividad.get(a.getId()), precioTipico,
                    latitud, longitud, ahora);
            if (puntuada.puntaje > 0) {
                puntuadas.add(puntuada);
            }
        }

        int tope = Math.clamp(limite != null ? limite : LIMITE_POR_DEFECTO, 1, LIMITE_MAXIMO);
        List<RecomendarActividadesResponse.Recomendada> recomendadas = puntuadas.stream()
                // A igual puntaje, la mejor calificada primero: un desempate estable evita que
                // la lista se reordene sola entre dos pedidos identicos.
                .sorted(Comparator.comparingDouble((Puntuada p) -> p.puntaje).reversed()
                        .thenComparing(p -> p.actividad.getRating(), Comparator.reverseOrder())
                        .thenComparing(p -> p.actividad.getNombre()))
                .limit(tope)
                .map(p -> mapear(p, proximaPorActividad.get(p.actividad.getId())))
                .toList();

        return new RecomendarActividadesResponse(false, recomendadas);
    }

    /**
     * El puntaje de una actividad, con sus motivos. Los motivos se arman <b>mientras</b> se
     * puntua y no despues: reconstruirlos aparte es la forma segura de que expliquen algo
     * distinto de lo que se calculo.
     */
    private Puntuada puntuar(Actividad a, PerfilDeGustos gustos, Clase proxima, BigDecimal precioTipico,
            Double latitud, Double longitud, Instant ahora) {
        var tipo = a.getTipoActividad();
        var categoria = tipo.getCategoria();
        var nivel = a.getNivelIntensidad();
        var instructor = a.getInstructor();

        double afTipo = gustos.afinidadTipo(tipo.getId());
        double afCategoria = gustos.afinidadCategoria(categoria.getId());
        double afNivel = gustos.afinidadNivel(nivel.getId());
        double afInstructor = gustos.afinidadInstructor(instructor.getId());

        double calidad = calidad(a.getRating());
        double afPrecio = afinidadPrecio(a.getPrecio(), precioTipico);
        Double distanciaKm = distanciaKm(a, latitud, longitud);
        double cercania = cercania(distanciaKm);
        double disponibilidad = disponibilidad(proxima);

        double puntaje = PESO_TIPO * afTipo
                + PESO_CATEGORIA * afCategoria
                + PESO_NIVEL * afNivel
                + PESO_INSTRUCTOR * afInstructor
                + PESO_CALIDAD * calidad
                + PESO_PRECIO * afPrecio
                + PESO_CERCANIA * cercania
                + PESO_DISPONIBILIDAD * disponibilidad;

        if (gustos.yaLaCurso(a.getId())) {
            puntaje -= PENALIZACION_YA_CURSADA;
        }

        List<String> motivos = new ArrayList<>();
        if (afTipo > 0 && gustos.esInteresDeclarado(tipo.getId())) {
            motivos.add("Coincide con tu interés en " + tipo.getNombre());
        } else if (afTipo > 0 && gustos.yaCursoEseTipo(tipo.getId())) {
            motivos.add("Parecida a las clases de " + tipo.getNombre() + " que ya hiciste");
        } else if (afTipo > 0) {
            motivos.add("Del tipo de actividades que venís mirando: " + tipo.getNombre());
        } else if (afCategoria > 0) {
            motivos.add("De " + categoria.getNombre() + ", una categoría que te interesa");
        }
        if (afInstructor > 0 && gustos.yaCursoConEseInstructor(instructor.getId())) {
            motivos.add("Con " + instructor.getNombre() + " " + instructor.getApellido() + ", que ya te dio clases");
        }
        if (afNivel > 0 && gustos.yaCursoEseNivel(nivel.getId())) {
            motivos.add("Mismo nivel que venís haciendo: " + nivel.getNombre());
        }
        if (calidad > 0) {
            motivos.add("Bien calificada por otros alumnos (" + unDecimal(a.getRating()) + ")");
        }
        if (distanciaKm != null && cercania > 0) {
            motivos.add(distanciaKm < 0.1
                    ? "A menos de 100 m tuyo"
                    : "A " + unDecimal(BigDecimal.valueOf(distanciaKm)) + " km tuyo");
        }
        if (afPrecio > 0) {
            motivos.add("En tu rango de precio habitual");
        }
        if (gustos.yaLaCurso(a.getId())) {
            motivos.add("Ya la hiciste antes");
        }

        return new Puntuada(a, puntaje, motivos);
    }

    /**
     * Un decimal con coma, que es como se escribe un numero en castellano. Los motivos son
     * texto que se le muestra al alumno, no un dato para la maquina: "4.4" ahi se lee mal.
     */
    private static String unDecimal(BigDecimal valor) {
        return valor.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Rating 3 es neutro, 5 el maximo y 1 el minimo. Sin reseñas (0) no suma ni resta. */
    private static double calidad(BigDecimal rating) {
        if (rating == null || rating.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return Math.clamp((rating.doubleValue() - 3.0) / 2.0, -1.0, 1.0);
    }

    /**
     * 1 cuando el precio es el que el alumno suele pagar y baja a medida que se aleja; llega a
     * -1 cuando cuesta el doble (o la mitad) de lo habitual. Sin historial de pagos es 0: no
     * se puede afirmar nada sobre su presupuesto.
     */
    private static double afinidadPrecio(BigDecimal precio, BigDecimal tipico) {
        if (tipico == null || tipico.compareTo(BigDecimal.ZERO) <= 0 || precio == null) {
            return 0.0;
        }
        double desvio = Math.abs(precio.doubleValue() - tipico.doubleValue()) / tipico.doubleValue();
        return Math.clamp(1.0 - desvio, -1.0, 1.0);
    }

    /** Escalones y no una curva: la distancia importa por tramos, no por metro. */
    private static double cercania(Double distanciaKm) {
        if (distanciaKm == null) {
            return 0.0;
        }
        if (distanciaKm <= 2) {
            return 1.0;
        }
        if (distanciaKm <= 5) {
            return 0.6;
        }
        return distanciaKm <= 10 ? 0.3 : 0.0;
    }

    /**
     * Una actividad sin proxima clase no se puede cursar aunque sea perfecta, y una sin cupo
     * tampoco: las dos bajan, pero no se descartan — el alumno puede querer verla igual.
     */
    private static double disponibilidad(Clase proxima) {
        if (proxima == null) {
            return -1.0;
        }
        return proxima.getCuposOcupados() < proxima.getCuposMax() ? 1.0 : -0.5;
    }

    /**
     * Haversine, la misma formula que ya usa el frontend en {@code lib/geo.ts} para el filtro
     * por radio de Explorar. Se calcula aca y no alla porque el puntaje tiene que salir armado
     * del backend; el cliente manda sus coordenadas como parametro opcional y, sin ellas, la
     * cercania simplemente no participa del puntaje.
     */
    private static Double distanciaKm(Actividad a, Double latitud, Double longitud) {
        if (latitud == null || longitud == null || a.getLatitud() == null || a.getLongitud() == null) {
            return null;
        }
        double dLat = Math.toRadians(a.getLatitud() - latitud);
        double dLon = Math.toRadians(a.getLongitud() - longitud);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitud)) * Math.cos(Math.toRadians(a.getLatitud()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    /** Los nombres normalizados del catalogo, para cruzar contra los terminos buscados. */
    private static Map<String, List<UUID>> nombresPorId(List<Actividad> actividades, boolean tipos) {
        Map<String, List<UUID>> mapa = new LinkedHashMap<>();
        for (Actividad a : actividades) {
            var tipo = a.getTipoActividad();
            String nombre = tipos ? tipo.getNombre() : tipo.getCategoria().getNombre();
            UUID id = tipos ? tipo.getId() : tipo.getCategoria().getId();
            mapa.computeIfAbsent(Texto.normalizar(nombre), clave -> new ArrayList<>());
            if (!mapa.get(Texto.normalizar(nombre)).contains(id)) {
                mapa.get(Texto.normalizar(nombre)).add(id);
            }
        }
        return mapa;
    }

    private Map<UUID, Clase> proximasClases(List<Actividad> actividades) {
        List<UUID> ids = actividades.stream().map(Actividad::getId).toList();
        Map<UUID, Clase> proximaPorActividad = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return proximaPorActividad;
        }
        for (Clase c : claseRepository.findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(ids, ESTADOS_EXCLUIDOS)) {
            proximaPorActividad.putIfAbsent(c.getActividad().getId(), c);
        }
        return proximaPorActividad;
    }

    private RecomendarActividadesResponse.Recomendada mapear(Puntuada p, Clase proxima) {
        Actividad a = p.actividad;
        var tipo = a.getTipoActividad();
        var categoria = tipo.getCategoria();
        var instructor = a.getInstructor();

        var proximaDto = proxima != null
                ? new RecomendarActividadesResponse.ProximaClase(
                        proxima.getFechaHora(), proxima.getEstado().name(),
                        proxima.getCuposMax(), proxima.getCuposOcupados())
                : null;

        return new RecomendarActividadesResponse.Recomendada(
                a.getId(),
                a.getNombre(),
                new RecomendarActividadesResponse.TipoActividad(tipo.getId(), tipo.getNombre()),
                new RecomendarActividadesResponse.Categoria(categoria.getId(), categoria.getNombre()),
                new RecomendarActividadesResponse.NivelIntensidad(
                        a.getNivelIntensidad().getId(), a.getNivelIntensidad().getNombre()),
                new RecomendarActividadesResponse.Instructor(
                        instructor.getId(), instructor.getNombre(), instructor.getApellido()),
                a.getPrecio(),
                a.getUbicacion(),
                a.getPhotoTint(),
                a.getRating(),
                a.getDuracionMin(),
                proximaDto,
                a.getLatitud(),
                a.getLongitud(),
                BigDecimal.valueOf(p.puntaje).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                p.motivos);
    }

    private record Puntuada(Actividad actividad, double puntaje, List<String> motivos) {
    }
}
