package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * RN-19 como test de arquitectura.
 *
 * <p>La regla dice que los permisos se resuelven contra {@code ConfiguracionRol} en la base,
 * no con condicionales hardcodeados. Se violó dos veces por el mismo camino: alguien agrega
 * un endpoint y lo protege con {@code hasRole('INSTRUCTOR')} porque es lo que Spring sugiere,
 * y a partir de ahí darle ese permiso a otro rol deja de hacer nada. Eso no rompe ningún
 * test funcional — el rol "correcto" sigue entrando — así que hace falta chequearlo acá.
 *
 * <p>El segundo chequeo cierra el agujero inverso: una clave escrita con un typo en un
 * {@code @PreAuthorize} no existe en la tabla {@code permiso}, y {@code PermisosService}
 * niega por defecto, así que el endpoint queda inaccesible para todos sin que nadie se
 * entere hasta que un usuario lo reporta.
 */
class GuardasDePermisoTest {

    private static final Path MAIN = Path.of("src/main/java/com/activehub");
    private static final Path MIGRACIONES = Path.of("src/main/resources/db/migration");

    /** El filtro JWT sí construye la authority `ROLE_x`: es quien traduce el claim del token. */
    private static final Set<String> EXENTOS = Set.of("JwtAuthenticationFilter.java", "JwtService.java");

    private static List<Path> fuentesJava() throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN)) {
            return paths.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !EXENTOS.contains(p.getFileName().toString()))
                    .toList();
        }
    }

    private static String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + p, e);
        }
    }

    @Test
    void ningunEndpointDecideElAccesoPorElNombreDelRol() throws IOException {
        Pattern porRol = Pattern.compile("hasRole\\(|hasAnyRole\\(|\"ROLE_[A-Z]+\"");

        Set<String> infractores = new TreeSet<>();
        for (Path p : fuentesJava()) {
            if (porRol.matcher(leer(p)).find()) {
                infractores.add(p.getFileName().toString());
            }
        }

        assertThat(infractores)
                .as("RN-19: el acceso se resuelve por permiso (@permisos.puede('clave')), "
                        + "nunca comparando contra el nombre del rol")
                .isEmpty();
    }

    @Test
    void todaClaveUsadaEnUnaGuardaExisteEnElCatalogoDePermisos() throws IOException {
        Set<String> enCodigo = clavesUsadasEnElCodigo();

        assertThat(enCodigo).as("el proyecto usa guardas por permiso").isNotEmpty();
        assertThat(enCodigo).allSatisfy(clave ->
                assertThat(clave).as("las claves son 'modulo.accion'").matches("[a-z]+\\.[a-zA-Z]+"));
        assertThat(enCodigo)
                .as("toda clave de un @PreAuthorize tiene que estar insertada en la tabla `permiso`; "
                        + "si no, PermisosService niega por defecto y el endpoint queda muerto")
                .isSubsetOf(clavesDelCatalogo());
    }

    @Test
    void todaClaveDelCatalogoTieneAlMenosUnaGuardaQueLaUse() throws IOException {
        assertThat(clavesDelCatalogo())
                .as("un permiso que la pantalla de HU08 deja tildar pero que ningún endpoint "
                        + "consulta es una casilla que no hace nada")
                .isSubsetOf(clavesUsadasEnElCodigo());
    }

    /**
     * Claves consultadas desde el código, en sus dos formas: la expresión SpEL de un
     * {@code @PreAuthorize} y la llamada directa a {@code PermisosService.puede(actorId, "…")},
     * que usan los endpoints que necesitan el id del actor para decidir (moderar la actividad
     * de otro instructor).
     */
    private static Set<String> clavesUsadasEnElCodigo() throws IOException {
        Set<String> claves = new TreeSet<>();
        List<Pattern> usos = List.of(
                Pattern.compile("@permisos\\.puede\\('([^']+)'\\)"),
                Pattern.compile("\\.puede\\([^,)]+,\\s*\"([^\"]+)\"\\)"));
        for (Path p : fuentesJava()) {
            String fuente = leer(p);
            for (Pattern uso : usos) {
                Matcher m = uso.matcher(fuente);
                while (m.find()) {
                    claves.add(m.group(1));
                }
            }
        }
        return claves;
    }

    /** Claves insertadas en la tabla `permiso` por las migraciones. */
    private static Set<String> clavesDelCatalogo() throws IOException {
        Set<String> claves = new TreeSet<>();
        // Las filas del INSERT tienen la forma ('clave', 'Modulo', 'Accion', bool, orden)
        Pattern fila = Pattern.compile("\\(\\s*'([a-z]+\\.[a-zA-Z]+)'\\s*,\\s*'");
        try (Stream<Path> paths = Files.list(MIGRACIONES)) {
            for (Path p : paths.filter(p -> p.toString().endsWith(".sql")).toList()) {
                String sql = leer(p);
                if (!sql.contains("INSERT INTO permiso")) {
                    continue;
                }
                Matcher m = fila.matcher(sql);
                while (m.find()) {
                    claves.add(m.group(1));
                }
            }
        }
        assertThat(claves).as("el catálogo de permisos se siembra en las migraciones").isNotEmpty();
        return claves;
    }
}
