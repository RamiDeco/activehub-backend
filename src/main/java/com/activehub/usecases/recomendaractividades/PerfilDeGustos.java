package com.activehub.usecases.recomendaractividades;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.interaccion.InteraccionAlumno;
import com.activehub.domain.interaccion.TipoInteraccion;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.usuario.PerfilAlumno;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que sabemos de los gustos de un alumno, resumido en cuatro mapas de afinidad: por tipo de
 * actividad, por categoria, por nivel de intensidad y por instructor.
 *
 * <h2>De donde sale cada numero</h2>
 *
 * Seis señales, con el peso de la tabla de abajo. Las tres primeras ya vivian en el modelo y
 * no se usaban para recomendar; las dos ultimas son las que agrego V27.
 *
 * <table>
 *   <tr><th>Señal</th><th>Que dice</th><th>Peso</th></tr>
 *   <tr><td>Interes declarado</td><td>lo que el alumno dijo que le gusta</td><td>tipo 3,0 · categoria 1,2</td></tr>
 *   <tr><td>Inscripcion no cancelada</td><td>lo que efectivamente hizo</td><td>tipo 2,0 · cat 0,8 · nivel 1,2 · instructor 1,0</td></tr>
 *   <tr><td>Favorito</td><td>lo que guardo para despues</td><td>tipo 2,0 · cat 0,8 · nivel 0,6 · instructor 1,0</td></tr>
 *   <tr><td>Reseña propia</td><td>si le gusto o no</td><td>tipo ±2,0 · instructor ±1,5, segun el puntaje</td></tr>
 *   <tr><td>Vista del detalle</td><td>lo que miro</td><td>tipo 0,6 · cat 0,25</td></tr>
 *   <tr><td>Busqueda</td><td>lo que fue a buscar</td><td>tipo o categoria 0,8 si el termino coincide</td></tr>
 * </table>
 *
 * <h2>Dos decisiones que explican los numeros</h2>
 *
 * <b>Lo que hizo pesa mas que lo que miro.</b> Una inscripcion es plata y tiempo; una vista es
 * curiosidad. Por eso 2,0 contra 0,6.
 *
 * <b>Lo viejo pesa menos</b> ({@link #factorAntiguedad}): los gustos cambian, y sin
 * decaimiento el alumno queda casado para siempre con lo que hizo el primer mes. El interes
 * <i>declarado</i> es la excepcion y no decae: es una afirmacion vigente hasta que la edite.
 *
 * <h2>Una reseña mala resta</h2>
 *
 * Es la unica señal negativa, y hace falta: sin ella, haber ido una vez a una actividad que
 * resulto mala la vuelve una recomendacion mas fuerte todavia, que es exactamente lo contrario
 * de lo que el alumno espera. El factor es {@code (puntaje - 3) / 2}, asi que 3 es neutro.
 *
 * <h2>Al final se normaliza</h2>
 *
 * Cada mapa se divide por su valor absoluto maximo, asi que las afinidades quedan en
 * [-1, 1]. Sin esto, un alumno con dos años de historial tendria numeros diez veces mas
 * grandes que uno recien llegado y los pesos del motor significarian cosas distintas para cada
 * uno.
 */
final class PerfilDeGustos {

    // --- Pesos por señal ---
    private static final double INTERES_TIPO = 3.0;
    private static final double INTERES_CATEGORIA = 1.2;

    private static final double INSCRIPCION_TIPO = 2.0;
    private static final double INSCRIPCION_CATEGORIA = 0.8;
    private static final double INSCRIPCION_NIVEL = 1.2;
    private static final double INSCRIPCION_INSTRUCTOR = 1.0;

    private static final double FAVORITO_TIPO = 2.0;
    private static final double FAVORITO_CATEGORIA = 0.8;
    private static final double FAVORITO_NIVEL = 0.6;
    private static final double FAVORITO_INSTRUCTOR = 1.0;

    private static final double RESENIA_TIPO = 2.0;
    private static final double RESENIA_INSTRUCTOR = 1.5;

    private static final double VISTA_TIPO = 0.6;
    private static final double VISTA_CATEGORIA = 0.25;

    private static final double BUSQUEDA_COINCIDENCIA = 0.8;

    // --- Decaimiento temporal ---
    private static final Duration RECIENTE = Duration.ofDays(30);
    private static final Duration INTERMEDIO = Duration.ofDays(90);

    private final Map<UUID, Double> porTipo = new HashMap<>();
    private final Map<UUID, Double> porCategoria = new HashMap<>();
    private final Map<UUID, Double> porNivel = new HashMap<>();
    private final Map<UUID, Double> porInstructor = new HashMap<>();

    /** Para redactar el motivo: de donde viene la afinidad, no cuanto vale. */
    private final Set<UUID> tiposDeclarados = new HashSet<>();
    private final Set<UUID> tiposCursados = new HashSet<>();
    private final Set<UUID> instructoresCursados = new HashSet<>();
    private final Set<UUID> nivelesCursados = new HashSet<>();

    /** Actividades que no se recomiendan: ya las tiene guardadas o ya esta anotado. */
    private final Set<UUID> favoritas = new HashSet<>();
    private final Set<UUID> conInscripcionVigente = new HashSet<>();
    /** Ya las hizo: se pueden repetir, pero no encabezan la lista. */
    private final Set<UUID> yaCursadas = new HashSet<>();

    private final List<BigDecimal> preciosPagados = new ArrayList<>();
    private int señales = 0;

    private final Instant ahora;

    PerfilDeGustos(Instant ahora) {
        this.ahora = ahora;
    }

    /**
     * Lo declarado en el perfil. Es la unica señal sin decaimiento: mientras el alumno no la
     * edite, sigue siendo lo que dice que le gusta.
     */
    void agregarIntereses(PerfilAlumno perfil) {
        if (perfil == null) {
            return;
        }
        perfil.getIntereses().forEach(tipo -> {
            sumar(porTipo, tipo.getId(), INTERES_TIPO);
            sumar(porCategoria, tipo.getCategoria().getId(), INTERES_CATEGORIA);
            tiposDeclarados.add(tipo.getId());
            señales++;
        });
    }

    /**
     * Las inscripciones. Las canceladas no cuentan: el alumno se arrepintio o la clase se cayo,
     * y en ninguno de los dos casos es evidencia de gusto.
     */
    void agregarInscripciones(List<Inscripcion> inscripciones) {
        for (Inscripcion i : inscripciones) {
            if (i.getEstado() == EstadoInscripcion.CANCELADA) {
                continue;
            }
            Actividad actividad = i.getClase().getActividad();
            double factor = factorAntiguedad(i.getCreatedAt());
            aportar(actividad, factor,
                    INSCRIPCION_TIPO, INSCRIPCION_CATEGORIA, INSCRIPCION_NIVEL, INSCRIPCION_INSTRUCTOR);

            tiposCursados.add(actividad.getTipoActividad().getId());
            instructoresCursados.add(actividad.getInstructor().getId());
            nivelesCursados.add(actividad.getNivelIntensidad().getId());
            if (i.getPago() != null && i.getPago().getMonto() != null) {
                preciosPagados.add(i.getPago().getMonto());
            }

            // Una clase que todavia no se dicto es un plan hecho: recomendar esa misma
            // actividad no le agrega nada. Una que ya paso si se puede repetir.
            if (i.getClase().getFechaHora().isAfter(ahora)) {
                conInscripcionVigente.add(actividad.getId());
            } else {
                yaCursadas.add(actividad.getId());
            }
            señales++;
        }
    }

    void agregarFavoritos(List<ActividadFavorita> favoritos) {
        for (ActividadFavorita f : favoritos) {
            Actividad actividad = f.getActividad();
            double factor = factorAntiguedad(f.getCreatedAt());
            aportar(actividad, factor, FAVORITO_TIPO, FAVORITO_CATEGORIA, FAVORITO_NIVEL, FAVORITO_INSTRUCTOR);
            favoritas.add(actividad.getId());
            señales++;
        }
    }

    /** Puntaje 3 es neutro; 5 suma el maximo y 1 resta el maximo. */
    void agregarResenias(List<Resenia> resenias) {
        for (Resenia r : resenias) {
            Actividad actividad = r.getClase().getActividad();
            double opinion = (r.getPuntaje() - 3) / 2.0;
            double factor = factorAntiguedad(r.getCreatedAt()) * opinion;
            sumar(porTipo, actividad.getTipoActividad().getId(), RESENIA_TIPO * factor);
            sumar(porInstructor, actividad.getInstructor().getId(), RESENIA_INSTRUCTOR * factor);
            señales++;
        }
    }

    /**
     * Las señales de V27. La busqueda se resuelve contra los nombres del catalogo que le pasa
     * el Service ya normalizados (sin tildes): el termino es texto libre y no apunta a ningun
     * id.
     */
    void agregarInteracciones(List<InteraccionAlumno> interacciones, Map<String, List<UUID>> tiposPorNombre,
            Map<String, List<UUID>> categoriasPorNombre) {
        for (InteraccionAlumno i : interacciones) {
            double factor = factorAntiguedad(i.getCreatedAt());
            if (i.getTipo() == TipoInteraccion.VISTA_ACTIVIDAD && i.getActividad() != null) {
                Actividad actividad = i.getActividad();
                sumar(porTipo, actividad.getTipoActividad().getId(), VISTA_TIPO * factor);
                sumar(porCategoria, actividad.getTipoActividad().getCategoria().getId(), VISTA_CATEGORIA * factor);
                señales++;
            } else if (i.getTipo() == TipoInteraccion.BUSQUEDA && i.getTermino() != null) {
                String termino = Texto.normalizar(i.getTermino());
                boolean coincidio = false;
                for (var entrada : tiposPorNombre.entrySet()) {
                    if (coinciden(termino, entrada.getKey())) {
                        entrada.getValue().forEach(id -> sumar(porTipo, id, BUSQUEDA_COINCIDENCIA * factor));
                        coincidio = true;
                    }
                }
                for (var entrada : categoriasPorNombre.entrySet()) {
                    if (coinciden(termino, entrada.getKey())) {
                        entrada.getValue().forEach(id -> sumar(porCategoria, id, BUSQUEDA_COINCIDENCIA * factor));
                        coincidio = true;
                    }
                }
                if (coincidio) {
                    señales++;
                }
            }
        }
    }

    /** Normaliza los cuatro mapas a [-1, 1]. Se llama una vez, despues de cargar todo. */
    void cerrar() {
        normalizar(porTipo);
        normalizar(porCategoria);
        normalizar(porNivel);
        normalizar(porInstructor);
    }

    // --- Lectura ---

    boolean sinSeñales() {
        return señales == 0;
    }

    double afinidadTipo(UUID tipoId) {
        return porTipo.getOrDefault(tipoId, 0.0);
    }

    double afinidadCategoria(UUID categoriaId) {
        return porCategoria.getOrDefault(categoriaId, 0.0);
    }

    double afinidadNivel(UUID nivelId) {
        return porNivel.getOrDefault(nivelId, 0.0);
    }

    double afinidadInstructor(UUID instructorId) {
        return porInstructor.getOrDefault(instructorId, 0.0);
    }

    boolean esInteresDeclarado(UUID tipoId) {
        return tiposDeclarados.contains(tipoId);
    }

    boolean yaCursoEseTipo(UUID tipoId) {
        return tiposCursados.contains(tipoId);
    }

    boolean yaCursoConEseInstructor(UUID instructorId) {
        return instructoresCursados.contains(instructorId);
    }

    boolean yaCursoEseNivel(UUID nivelId) {
        return nivelesCursados.contains(nivelId);
    }

    boolean estaExcluida(UUID actividadId) {
        return favoritas.contains(actividadId) || conInscripcionVigente.contains(actividadId);
    }

    boolean yaLaCurso(UUID actividadId) {
        return yaCursadas.contains(actividadId);
    }

    /**
     * Lo que el alumno suele pagar, para no recomendarle algo diez veces mas caro. Es la
     * <b>mediana</b> y no el promedio: una sola actividad cara le corre el promedio y le
     * arruina el resto de las recomendaciones.
     */
    BigDecimal precioTipico() {
        if (preciosPagados.isEmpty()) {
            return null;
        }
        List<BigDecimal> ordenados = new ArrayList<>(preciosPagados);
        ordenados.sort(BigDecimal::compareTo);
        return ordenados.get(ordenados.size() / 2);
    }

    // --- Interno ---

    private void aportar(Actividad actividad, double factor,
            double pesoTipo, double pesoCategoria, double pesoNivel, double pesoInstructor) {
        var tipo = actividad.getTipoActividad();
        sumar(porTipo, tipo.getId(), pesoTipo * factor);
        sumar(porCategoria, tipo.getCategoria().getId(), pesoCategoria * factor);
        sumar(porNivel, actividad.getNivelIntensidad().getId(), pesoNivel * factor);
        sumar(porInstructor, actividad.getInstructor().getId(), pesoInstructor * factor);
    }

    private static void sumar(Map<UUID, Double> mapa, UUID clave, double valor) {
        mapa.merge(clave, valor, Double::sum);
    }

    private static void normalizar(Map<UUID, Double> mapa) {
        double max = mapa.values().stream().mapToDouble(Math::abs).max().orElse(0.0);
        if (max <= 0) {
            return;
        }
        mapa.replaceAll((clave, valor) -> valor / max);
    }

    /** 1,0 el ultimo mes; 0,6 hasta los tres meses; 0,3 de ahi en adelante. */
    private double factorAntiguedad(Instant cuando) {
        if (cuando == null) {
            return 1.0;
        }
        Duration edad = Duration.between(cuando, ahora);
        if (edad.compareTo(RECIENTE) <= 0) {
            return 1.0;
        }
        return edad.compareTo(INTERMEDIO) <= 0 ? 0.6 : 0.3;
    }

    /** Coincidencia por contencion en cualquiera de los dos sentidos: "yoga" contra "Yoga". */
    private static boolean coinciden(String termino, String nombreNormalizado) {
        return nombreNormalizado.contains(termino) || termino.contains(nombreNormalizado);
    }
}
