package com.activehub.usecases.verimagenactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Galería de la actividad. Se apoya en el disco, así que el test usa un directorio temporal
 * real en vez de mockear {@code Files}: lo que importa es justamente que la lectura del
 * archivo y la deducción del Content-Type funcionen juntas.
 */
@ExtendWith(MockitoExtension.class)
class VerImagenActividadServiceTest {

    @TempDir
    Path directorio;

    @Mock
    private ActividadImagenRepository actividadImagenRepository;

    private VerImagenActividadService service;
    private UUID imagenId;

    @BeforeEach
    void setUp() {
        service = new VerImagenActividadService(actividadImagenRepository, directorio.toString());
        imagenId = UUID.randomUUID();
    }

    private ActividadImagen imagenEnDisco(String nombreArchivo, byte[] contenido) throws IOException {
        Files.write(directorio.resolve(nombreArchivo), contenido);
        ActividadImagen imagen = new ActividadImagen();
        imagen.setPath(nombreArchivo);
        return imagen;
    }

    @Test
    void ver_archivoJpg_devuelveContenidoYContentTypeJpeg() throws IOException {
        byte[] bytes = {1, 2, 3, 4};
        when(actividadImagenRepository.findById(imagenId))
                .thenReturn(Optional.of(imagenEnDisco("foto.jpg", bytes)));

        ImagenDescarga descarga = service.ver(imagenId);

        assertThat(descarga.contenido()).isEqualTo(bytes);
        assertThat(descarga.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    void ver_archivoPng_devuelveContentTypePng() throws IOException {
        when(actividadImagenRepository.findById(imagenId))
                .thenReturn(Optional.of(imagenEnDisco("foto.PNG", new byte[] {9})));

        assertThat(service.ver(imagenId).tipoContenido()).isEqualTo("image/png");
    }

    @Test
    void ver_imagenInexistente_lanzaNoEncontrado() {
        when(actividadImagenRepository.findById(imagenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ver(imagenId))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessageContaining("Imagen no encontrada");
    }

    @Test
    void ver_filaEnBasePeroArchivoBorradoDelDisco_lanzaNoEncontrado() {
        // Pasa si alguien limpia el directorio de subidas a mano: la fila sobrevive.
        ActividadImagen huerfana = new ActividadImagen();
        huerfana.setPath("no-existe.jpg");
        when(actividadImagenRepository.findById(imagenId)).thenReturn(Optional.of(huerfana));

        assertThatThrownBy(() -> service.ver(imagenId))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessageContaining("No pudimos leer la imagen");
    }
}
