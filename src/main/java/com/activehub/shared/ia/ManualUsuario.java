package com.activehub.shared.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * El manual de usuario de ActiveHub, partido en secciones y consultable por palabras.
 *
 * <h2>Es la ÚNICA fuente de verdad del asistente de ayuda</h2>
 *
 * El chatbot no responde con lo que el modelo sepa del mundo: responde con lo que dice el manual.
 * Este componente es el que decide qué pedazos del manual entran en el prompt, así que también es
 * el que define de qué puede hablar el asistente. Lo que no está acá, no existe para él.
 *
 * <h2>Por qué no se manda el manual entero en cada pregunta</h2>
 *
 * El manual son ~46 KB (unos 13.000 tokens). Mandarlo completo funciona en un modelo de 128k de
 * contexto, pero <b>el plan gratuito de Groq limita los tokens por minuto</b> (del orden de 12.000
 * para los modelos grandes): una sola pregunta consumiría la cuota del minuto entero y la segunda
 * recibiría un 429. Con las 4 o 5 secciones que importan el prompt baja a ~3.000 tokens, entran
 * varias preguntas por minuto y además el modelo se distrae menos.
 *
 * <p>La búsqueda es por coincidencia de palabras, no por embeddings: no hay vectores que mantener,
 * no hay un segundo servicio al que pedirle nada y el manual está escrito con las mismas palabras
 * que usa la gente en el chat, porque los títulos y los textos de botones son los de la interfaz
 * ("Preinscribirme", "Mis clases", "Retenido"). Si algún día el manual crece a cientos de páginas,
 * este es el punto donde se cambia la estrategia — el resto del código no se enteraría.
 *
 * <h2>Cómo se parte</h2>
 *
 * Por los títulos numerados del propio manual ("1.1 Crear una cuenta", "2. Manual del Alumno", …).
 * Cada sección se indexa con su título, que pesa triple al puntuar: quien pregunta "¿cómo cancelo?"
 * tiene que caer en "Cancelar una clase" y no en cualquier párrafo donde la palabra aparezca de
 * paso. El texto anterior al primer título numerado (introducción y convenciones) queda como una
 * sección más.
 *
 * <p>El archivo se lee <b>una sola vez al arrancar</b>, del classpath: viaja dentro del jar, así
 * que no depende de ninguna ruta del disco del servidor. Actualizar el manual es reemplazar
 * {@code src/main/resources/manual/manual-usuario-activehub.txt} y volver a compilar.
 */
@Component
public class ManualUsuario {

    private static final Logger log = LoggerFactory.getLogger(ManualUsuario.class);

    private static final String RUTA = "manual/manual-usuario-activehub.txt";

    /**
     * Techo de caracteres del manual que entran en un prompt (~1.500 tokens).
     *
     * <p>Se bajó de 10.000 a 6.000 después de la primera prueba real: con el prompt más grande, el
     * tope de <b>tokens por minuto</b> del plan gratuito se topaba a la segunda pregunta y la
     * tercera recibía un 429. Cada carácter que entra acá se paga dos veces, en cuota y en
     * distracción del modelo, así que el número correcto es el más chico que siga respondiendo bien.
     */
    private static final int PRESUPUESTO_CARACTERES = 6_000;

    /** Una sección del manual: su título tal como está escrito y su texto completo. */
    public record Seccion(String titulo, String texto) {

        /** Título + texto, que es la forma en que la sección entra en el prompt. */
        public String completa() {
            return titulo + "\n" + texto;
        }
    }

    private final List<Seccion> secciones;
    private final List<String> tituloNormalizado = new ArrayList<>();
    private final List<String> textoNormalizado = new ArrayList<>();

    public ManualUsuario() {
        this(new ClassPathResource(RUTA));
    }

    /** Para los tests, que le pasan un manual chico en vez del real. */
    public ManualUsuario(Resource recurso) {
        this.secciones = partir(leer(recurso));
        for (Seccion s : secciones) {
            tituloNormalizado.add(TextoIa.normalizar(s.titulo()));
            textoNormalizado.add(TextoIa.normalizar(s.texto()));
        }
        log.info("Manual de usuario cargado: {} secciones", secciones.size());
    }

    public int cantidadSecciones() {
        return secciones.size();
    }

    /** Los títulos, en el orden del manual. Sirven para que el modelo sepa qué temas existen. */
    public List<String> titulos() {
        return secciones.stream().map(Seccion::titulo).toList();
    }

    /**
     * Las secciones más relacionadas con la consulta, de la más relacionada a la menos, cortando
     * por {@link #PRESUPUESTO_CARACTERES}.
     *
     * <p>Devuelve lista vacía si ninguna palabra de la consulta aparece en el manual. Eso <b>no</b>
     * significa "responder que no sabemos": el caso de uso igual le manda al modelo un contexto
     * mínimo, porque una pregunta legítima puede estar escrita con palabras que el manual no usa.
     * La negativa la decide el modelo con las reglas del prompt, no este método.
     */
    public List<Seccion> buscar(String consulta, int maximo) {
        List<String> claves = TextoIa.palabrasClave(consulta);
        if (claves.isEmpty()) {
            return List.of();
        }

        record Candidata(Seccion seccion, int puntaje) {
        }
        List<Candidata> candidatas = new ArrayList<>();
        for (int i = 0; i < secciones.size(); i++) {
            int puntaje = 0;
            for (String clave : claves) {
                // El título pesa triple: es lo que dice de qué trata la sección.
                puntaje += 3 * apariciones(tituloNormalizado.get(i), clave);
                // Las apariciones en el cuerpo se topean en 3 por palabra, si no una sección larga
                // que repite "clase" veinte veces le gana a la que trata del tema.
                puntaje += Math.min(3, apariciones(textoNormalizado.get(i), clave));
            }
            if (puntaje > 0) {
                candidatas.add(new Candidata(secciones.get(i), puntaje));
            }
        }

        candidatas.sort(Comparator.comparingInt(Candidata::puntaje).reversed());

        List<Seccion> elegidas = new ArrayList<>();
        int usados = 0;
        for (Candidata c : candidatas) {
            if (elegidas.size() >= maximo) {
                break;
            }
            int largo = c.seccion().completa().length();
            // La primera entra siempre, aunque sola se pase del presupuesto: mejor una sección
            // grande que ninguna. Las siguientes se saltean si no caben.
            if (usados + largo > PRESUPUESTO_CARACTERES && !elegidas.isEmpty()) {
                continue;
            }
            elegidas.add(c.seccion());
            usados += largo;
        }
        return elegidas;
    }

    private int apariciones(String texto, String palabra) {
        int total = 0;
        int desde = 0;
        while (true) {
            int pos = texto.indexOf(palabra, desde);
            if (pos < 0) {
                return total;
            }
            total++;
            desde = pos + palabra.length();
        }
    }

    private static String leer(Resource recurso) {
        try (InputStream in = recurso.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Sin manual el asistente no tiene de qué hablar, y eso es un error de empaquetado, no
            // algo que deba descubrirse en la primera pregunta de un usuario.
            throw new IllegalStateException(
                    "No se pudo leer el manual de usuario en " + recurso.getDescription(), e);
        }
    }

    /**
     * Parte por títulos numerados. Acepta "2.", "2.1" y "2.1.3", siempre al principio de la línea y
     * seguidos de texto — así "El plazo es de 4 días" no abre una sección.
     */
    private static List<Seccion> partir(String contenido) {
        List<Seccion> resultado = new ArrayList<>();
        String[] lineas = contenido.replace("\r\n", "\n").split("\n");
        String tituloActual = "Introducción y convenciones";
        StringBuilder cuerpo = new StringBuilder();

        for (String linea : lineas) {
            if (esTitulo(linea)) {
                if (!cuerpo.toString().isBlank()) {
                    resultado.add(new Seccion(tituloActual, cuerpo.toString().strip()));
                }
                tituloActual = linea.strip();
                cuerpo.setLength(0);
            } else {
                cuerpo.append(linea).append('\n');
            }
        }
        if (!cuerpo.toString().isBlank()) {
            resultado.add(new Seccion(tituloActual, cuerpo.toString().strip()));
        }
        return resultado;
    }

    private static boolean esTitulo(String linea) {
        return linea.strip().matches("^\\d+(\\.\\d+)*\\.?\\s+\\p{L}.*");
    }
}
