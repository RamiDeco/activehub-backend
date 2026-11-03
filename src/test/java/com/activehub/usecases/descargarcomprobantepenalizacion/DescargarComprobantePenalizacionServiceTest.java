package com.activehub.usecases.descargarcomprobantepenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoDisco;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** V36: el administrador abre el comprobante. Con un {@code @TempDir} real, como los documentos. */
@ExtendWith(MockitoExtension.class)
class DescargarComprobantePenalizacionServiceTest {

    @Mock private ComprobantePenalizacionRepository comprobantePenalizacionRepository;

    @TempDir Path tempDir;

    private DescargarComprobantePenalizacionService service() {
        return new DescargarComprobantePenalizacionService(
                comprobantePenalizacionRepository,
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString(), tempDir.toString()));
    }

    @Test
    void descargar_devuelveElArchivoConSuNombreYTipo() throws Exception {
        Files.write(tempDir.resolve("guardado.pdf"), "contenido".getBytes());
        ComprobantePenalizacion comprobante = new ComprobantePenalizacion();
        comprobante.setRutaArchivo("guardado.pdf");
        comprobante.setNombreArchivo("transferencia.pdf");
        comprobante.setTipoContenido("application/pdf");
        UUID id = UUID.randomUUID();
        when(comprobantePenalizacionRepository.findById(id)).thenReturn(Optional.of(comprobante));

        ComprobanteDescarga descarga = service().descargar(id);

        assertThat(descarga.nombreArchivo()).isEqualTo("transferencia.pdf");
        assertThat(descarga.tipoContenido()).isEqualTo("application/pdf");
        assertThat(descarga.contenido()).isEqualTo("contenido".getBytes());
    }

    @Test
    void descargar_comprobanteInexistente_lanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(comprobantePenalizacionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().descargar(id)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void descargar_sinElArchivo_lanzaNoEncontrado() {
        ComprobantePenalizacion comprobante = new ComprobantePenalizacion();
        comprobante.setRutaArchivo("no-existe.pdf");
        comprobante.setNombreArchivo("transferencia.pdf");
        comprobante.setTipoContenido("application/pdf");
        UUID id = UUID.randomUUID();
        when(comprobantePenalizacionRepository.findById(id)).thenReturn(Optional.of(comprobante));

        assertThatThrownBy(() -> service().descargar(id)).isInstanceOf(NoEncontradoException.class);
    }
}
