package com.activehub.usecases.verfotoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoDisco;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerFotoActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @TempDir
    Path tempDir;

    private VerFotoActividadService service;
    private UUID actividadId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        AlmacenamientoDisco almacenamiento =
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString());
        service = new VerFotoActividadService(actividadRepository, almacenamiento);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    @Test
    void ver_fotoExistente_devuelveContenido() throws Exception {
        Files.write(tempDir.resolve("foto.jpg"), "contenido".getBytes());
        actividad.setFotoPath("foto.jpg");
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        FotoDescarga foto = service.ver(actividadId);

        assertThat(foto.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.contenido()).isEqualTo("contenido".getBytes());
    }

    @Test
    void ver_sinFoto_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.ver(actividadId)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void ver_actividadInexistente_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ver(actividadId)).isInstanceOf(NoEncontradoException.class);
    }
}
