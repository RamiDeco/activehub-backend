package com.activehub.shared.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.activehub.domain.usuario.PropositoVerificacion;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * El mail no se puede "mirar" en un test, pero sí se pueden fijar las cosas que romperían
 * silenciosamente: que el código llegue, que el texto cambie entre alta y cambio de correo, y
 * que el nombre del usuario no pueda inyectar HTML.
 *
 * <p>Además escribe los dos mails a {@code target/mails-preview/} para poder abrirlos en el
 * navegador: es la única forma real de revisar un diseño de mail sin mandarlo.
 */
class PlantillaEmailTest {

    @Test
    void cuerpo_alta_traeElCodigoYLosColoresDeLaMarca() throws Exception {
        String html = PlantillaEmail.cuerpo(PropositoVerificacion.REGISTRO, "Martina", "482915", 15);

        assertThat(html).contains("482915");
        assertThat(html).contains("Confirmá tu correo");
        assertThat(html).contains("Bienvenido a ActiveHub");
        assertThat(html).contains("vence en 15 minutos".toLowerCase());
        // Los hex de la identidad visual (los mismos del frontend).
        assertThat(html).contains("#0E2A47").contains("#FF6A2B");

        guardarPreview("alta.html", html);
    }

    @Test
    void cuerpo_cambioDeEmail_cambiaElTexto_noSoloElAsunto() throws Exception {
        String html = PlantillaEmail.cuerpo(PropositoVerificacion.CAMBIO_EMAIL, "Martina", "482915", 15);

        assertThat(html).contains("Confirmá tu nuevo correo");
        assertThat(html).contains("Pediste cambiar el correo");
        // Lo que tranquiliza a quien recibe el mail sin haberlo pedido.
        assertThat(html).contains("tu cuenta sigue funcionando con el correo anterior");

        guardarPreview("cambio-email.html", html);
    }

    @Test
    void asunto_llevaElCodigo_paraVerloSinAbrirElMail() {
        assertThat(PlantillaEmail.asunto(PropositoVerificacion.REGISTRO, "482915")).startsWith("482915 —");
        assertThat(PlantillaEmail.asunto(PropositoVerificacion.CAMBIO_EMAIL, "482915")).contains("nuevo correo");
    }

    /** El nombre lo escribe el usuario en el registro y termina dentro de un documento HTML. */
    @Test
    void cuerpo_escapaElNombre_noInyectaHtml() {
        String html = PlantillaEmail.cuerpo(
                PropositoVerificacion.REGISTRO, "<script>alert(1)</script>", "000000", 15);

        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    private void guardarPreview(String nombre, String html) throws Exception {
        Path dir = Path.of("target", "mails-preview");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), html);
    }
}
