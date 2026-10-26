package com.activehub.usecases.subirfotoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubirFotoActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ActividadImagenRepository actividadImagenRepository;
    @Mock
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private SubirFotoActividadService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        AlmacenamientoDisco almacenamiento =
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString());
        service = new SubirFotoActividadService(actividadRepository, actividadImagenRepository, auditService, almacenamiento);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    @Test
    void subir_dueño_actualizaActividadYAudita() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "act.jpg", "image/jpeg", "contenido".getBytes());
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        SubirFotoActividadResponse response = service.subir(actividadId, archivo, instructorId, false);

        assertThat(response.actividadId()).isEqualTo(actividadId);
        assertThat(actividad.getFotoPath()).isNotNull();
        verify(actividadRepository).save(actividad);
        verify(auditService).registrar(
                eq(instructorId), eq(AuditAccion.FOTO_ACTIVIDAD_SUBIDA), eq("Actividad"), eq(actividadId), isNull());
    }

    /**
     * Regresión: la primera imagen de la galería queda también como portada y comparten el
     * archivo. Cambiar la portada borraba ese archivo y la imagen de la galería quedaba rota.
     */
    @Test
    void subir_portadaAnteriorEsDeLaGaleria_noBorraElArchivo() throws Exception {
        Path anterior = Files.writeString(tempDir.resolve("de-la-galeria.jpg"), "galeria");
        actividad.setFotoPath("de-la-galeria.jpg");
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.existsByActividadIdAndPath(actividadId, "de-la-galeria.jpg")).thenReturn(true);
        MockMultipartFile archivo = new MockMultipartFile("archivo", "nueva.jpg", "image/jpeg", "nueva".getBytes());

        service.subir(actividadId, archivo, instructorId, false);

        assertThat(actividad.getFotoPath()).isNotEqualTo("de-la-galeria.jpg");
        assertThat(anterior).exists();
    }

    @Test
    void subir_portadaAnteriorSubidaAparte_borraElArchivo() throws Exception {
        Path anterior = Files.writeString(tempDir.resolve("suelta.jpg"), "portada");
        actividad.setFotoPath("suelta.jpg");
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.existsByActividadIdAndPath(actividadId, "suelta.jpg")).thenReturn(false);
        MockMultipartFile archivo = new MockMultipartFile("archivo", "nueva.jpg", "image/jpeg", "nueva".getBytes());

        service.subir(actividadId, archivo, instructorId, false);

        assertThat(anterior).doesNotExist();
    }

    @Test
    void subir_admin_puedeSubirAunSinSerDueño() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "act.jpg", "image/jpeg", "contenido".getBytes());
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        SubirFotoActividadResponse response = service.subir(actividadId, archivo, UUID.randomUUID(), true);

        assertThat(response.actividadId()).isEqualTo(actividadId);
    }

    @Test
    void subir_noDueño_lanzaSinPermiso() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "act.jpg", "image/jpeg", "contenido".getBytes());
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.subir(actividadId, archivo, UUID.randomUUID(), false))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void subir_formatoNoPermitido_lanzaValidacion() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "video.mp4", "video/mp4", "x".getBytes());

        assertThatThrownBy(() -> service.subir(actividadId, archivo, instructorId, false))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void subir_actividadInexistente_lanzaNoEncontrado() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "act.jpg", "image/jpeg", "contenido".getBytes());
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subir(actividadId, archivo, instructorId, false))
                .isInstanceOf(NoEncontradoException.class);
    }
}
