package com.activehub.domain.resenia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * La regla de las reseñas como test de arquitectura (V33).
 *
 * <p>Una reseña se publica en el momento en que el alumno la crea, y después sólo admite dos
 * cosas: que el instructor la responda o la denuncie, y que el administrador la oculte (por una
 * denuncia resuelta o por su cuenta). No hay moderación previa, ni edición, ni baja. Se
 * retiraron {@code aprobarresenia}, {@code rechazarresenia}, {@code listarresenaspendientes},
 * {@code actualizarresenia} y {@code eliminarresenia}.
 *
 * <p>Ningún test funcional detectaría que alguien vuelva a agregar un {@code PUT} o un
 * {@code DELETE} sobre una reseña: el slice nuevo pasaría sus propios tests. Por eso se chequea
 * acá, leyendo los controllers, igual que {@code GuardasDePermisoTest} con {@code hasRole}.
 */
class ReseniaInmutableTest {

    private static final Path USECASES = Path.of("src/main/java/com/activehub/usecases");

    private static final Set<String> SLICES_RETIRADOS = Set.of(
            "aprobarresenia", "rechazarresenia", "listarresenaspendientes", "actualizarresenia", "eliminarresenia");

    private static List<Path> controllersDeResenias() throws IOException {
        try (Stream<Path> paths = Files.walk(USECASES)) {
            return paths.filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .filter(p -> leer(p).contains("/resenas"))
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
    void ningunEndpointDeReseniasLaEditaNiLaBorra() throws IOException {
        Pattern modifica = Pattern.compile("@(PutMapping|PatchMapping|DeleteMapping)\\b");

        List<Path> controllers = controllersDeResenias();
        assertThat(controllers).as("hay controllers de reseñas que revisar").isNotEmpty();

        Set<String> infractores = new TreeSet<>();
        for (Path p : controllers) {
            if (modifica.matcher(leer(p)).find()) {
                infractores.add(p.getFileName().toString());
            }
        }

        assertThat(infractores)
                .as("una reseña publicada no se edita ni se elimina: sólo se responde, se denuncia o se oculta")
                .isEmpty();
    }

    @Test
    void noHayModeracionPreviaDeResenias() throws IOException {
        Pattern moderacion = Pattern.compile("\"/\\{id}/(aprobar|rechazar)\"");

        Set<String> infractores = new TreeSet<>();
        for (Path p : controllersDeResenias()) {
            if (moderacion.matcher(leer(p)).find()) {
                infractores.add(p.getFileName().toString());
            }
        }

        assertThat(infractores)
                .as("las reseñas se publican al crearse; el administrador atiende denuncias, no aprueba reseñas")
                .isEmpty();
    }

    @Test
    void losSlicesRetiradosNoVuelven() {
        assertThat(SLICES_RETIRADOS)
                .allSatisfy(slice -> assertThat(USECASES.resolve(slice)).doesNotExist());
    }
}
