package com.activehub.shared.ia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Normalización de texto para buscar dentro del manual de usuario.
 *
 * <p>Es la misma idea que {@code lib/texto.ts} del frontend y que {@code Texto} del motor de
 * recomendaciones: sin tildes y en minúsculas, porque nadie escribe "inscripción" con tilde en un
 * chat. Vive acá y no se reusa la clase del slice de recomendaciones porque los "verbos" no se
 * comparten entre casos de uso (convención del repo) y esto además lo usan dos slices.
 */
public final class TextoIa {

    /**
     * Palabras que aparecen en cualquier pregunta y en casi toda sección del manual: si puntúan,
     * la sección más larga siempre gana. Se descartan antes de buscar.
     */
    private static final Set<String> VACIAS = Set.of(
            "como", "cual", "cuales", "cuando", "donde", "porque", "para", "esta", "este", "esto",
            "ese", "esa", "eso", "los", "las", "una", "unos", "unas", "del", "con", "sin", "por",
            "que", "the", "pero", "mas", "muy", "hay", "son", "ser", "soy", "tengo", "tiene",
            "tienen", "puedo", "puede", "pueden", "quiero", "queres", "hacer", "haces", "hago",
            "sobre", "desde", "hasta", "entre", "todo", "toda", "todos", "todas", "algo", "algun",
            "alguna", "mismo", "misma", "otro", "otra", "cosa", "cosas", "favor", "gracias",
            "hola", "buenas", "sistema", "activehub", "plataforma", "pantalla", "aplicacion");

    /** Largo de la raíz con la que se busca. Cinco alcanza para "cance…", "inscr…", "prein…". */
    private static final int LARGO_RAIZ = 5;

    /**
     * Cómo dice la gente lo que el manual escribe de otra manera, ya recortado a la raíz.
     *
     * <p>El manual está escrito con los textos de la interfaz ("instructor", "inscripción",
     * "reintegro"), pero en un chat nadie escribe así: escribe "el profe", "anotarme", "que me
     * devuelvan la plata". Sin esta traducción la búsqueda no encuentra la sección del tema y el
     * modelo contesta con lo que tenga a mano, que es más vago. La raíz original <b>se conserva</b>:
     * se agrega el sinónimo, no se reemplaza.
     *
     * <p>Es una lista corta y a mano, no un diccionario: cada entrada sale de una forma de preguntar
     * que apareció probando. Si aparece otra, se agrega acá y no se toca nada más.
     */
    private static final Map<String, String> SINONIMOS = Map.ofEntries(
            Map.entry("profe", "instru"),
            Map.entry("plata", "pago"),
            Map.entry("diner", "pago"),
            Map.entry("cobro", "pago"),
            Map.entry("anota", "inscr"),
            Map.entry("apunt", "inscr"),
            Map.entry("turno", "clase"),
            Map.entry("clave", "contra"),
            Map.entry("mail", "correo"),
            Map.entry("reemb", "reinte"),
            Map.entry("devol", "reinte"),
            Map.entry("queja", "denunc"),
            Map.entry("recla", "denunc"),
            Map.entry("punta", "resen"),
            Map.entry("calif", "resen"),
            Map.entry("estre", "resen"),
            Map.entry("falto", "inasis"),
            Map.entry("prese", "inasis"),
            Map.entry("ausen", "inasis"));

    private TextoIa() {
    }

    /** Minúsculas, sin tildes y con todo lo que no sea letra o número convertido en espacio. */
    public static String normalizar(String texto) {
        return minusculasSinTildes(texto).replaceAll("[^a-z0-9ñ]+", " ").trim();
    }

    /**
     * Minúsculas y sin tildes, <b>conservando la puntuación</b>.
     *
     * <p>La necesita {@link GuardiaDePrompt} para buscar rastros como {@code llama-} o {@code gpt-}:
     * con {@link #normalizar} el guion se vuelve un espacio y "llama " coincide con "se llama Juan",
     * que es una frase perfectamente normal. La puntuación es justamente lo que distingue el nombre
     * de un modelo de una palabra del idioma.
     */
    public static String minusculasSinTildes(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    /**
     * Las palabras con las que vale la pena buscar: normalizadas, sin repetidos, sin vacías y de al
     * menos tres caracteres ("dni", "pago" y "cbu" cuentan; "de" y "la" no).
     *
     * <p><b>Las largas se recortan a {@value #LARGO_RAIZ} caracteres</b>, que es un stemming de
     * pobre pero suficiente para el castellano: quien escribe "cancelo" tiene que encontrar
     * "cancelar" y "Cancelada", y quien escribe "inscribirme" tiene que encontrar "inscripción".
     * Sin esto, la conjugación del usuario y la del manual casi nunca coinciden y la búsqueda
     * devuelve la sección equivocada. La precisión que se pierde la recupera el peso triple del
     * título al puntuar.
     */
    public static List<String> palabrasClave(String texto) {
        List<String> claves = new ArrayList<>();
        for (String palabra : normalizar(texto).split(" ")) {
            if (palabra.length() < 3 || VACIAS.contains(palabra)) {
                continue;
            }
            String raiz = palabra.length() > LARGO_RAIZ ? palabra.substring(0, LARGO_RAIZ) : palabra;
            if (!claves.contains(raiz)) {
                claves.add(raiz);
            }
            String sinonimo = SINONIMOS.get(raiz);
            if (sinonimo != null && !claves.contains(sinonimo)) {
                claves.add(sinonimo);
            }
        }
        return claves;
    }
}
