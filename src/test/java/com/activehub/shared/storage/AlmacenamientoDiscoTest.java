package com.activehub.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Se ejercita contra un directorio real y no contra un mock de {@code Files}: lo que importa
 * es que escribir y leer funcionen juntos, y que cada carpeta caiga en su propia ruta.
 */
class AlmacenamientoDiscoTest {

    @TempDir
    Path raiz;

    private Path perfiles;
    private Path actividades;
    private Path documentos;
    private AlmacenamientoDisco almacenamiento;

    @BeforeEach
    void setUp() {
        perfiles = raiz.resolve("fotos-perfil");
        actividades = raiz.resolve("fotos-actividad");
        documentos = raiz.resolve("documentos-instructor");
        almacenamiento = new AlmacenamientoDisco(
                perfiles.toString(), actividades.toString(), documentos.toString());
    }

    @Test
    void guardar_creaElDirectorioSiNoExisteYEscribeElArchivo() {
        almacenamiento.guardar(CarpetaArchivos.FOTOS_PERFIL, "foto.jpg", "contenido".getBytes(), "image/jpeg");

        assertThat(perfiles.resolve("foto.jpg")).exists();
        assertThat(almacenamiento.leer(CarpetaArchivos.FOTOS_PERFIL, "foto.jpg"))
                .isEqualTo("contenido".getBytes());
    }

    /** Cada carpeta tiene su destino propio: un documento no puede terminar entre las fotos. */
    @Test
    void guardar_cadaCarpetaEscribeEnSuPropioDirectorio() {
        almacenamiento.guardar(
                CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, "dni.pdf", "contenido".getBytes(), "application/pdf");

        assertThat(documentos.resolve("dni.pdf")).exists();
        assertThat(perfiles.resolve("dni.pdf")).doesNotExist();
        assertThat(actividades.resolve("dni.pdf")).doesNotExist();
    }

    @Test
    void leer_archivoInexistente_lanzaAlmacenamientoException() {
        assertThatThrownBy(() -> almacenamiento.leer(CarpetaArchivos.FOTOS_ACTIVIDAD, "no-esta.jpg"))
                .isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void borrarSiExiste_borraElArchivo() throws Exception {
        Files.createDirectories(actividades);
        Files.write(actividades.resolve("vieja.jpg"), "vieja".getBytes());

        almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_ACTIVIDAD, "vieja.jpg");

        assertThat(actividades.resolve("vieja.jpg")).doesNotExist();
    }

    /**
     * El contrato dice que nunca lanza: se llama para limpiar archivos que ya no importan
     * (una foto reemplazada, un huérfano de un rollback) y fallar ahí convertiría una
     * operación exitosa en un error para el usuario.
     */
    @Test
    void borrarSiExiste_archivoQueNoEsta_noLanza() {
        assertThatCode(() -> almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_PERFIL, "no-esta.jpg"))
                .doesNotThrowAnyException();
    }
}
