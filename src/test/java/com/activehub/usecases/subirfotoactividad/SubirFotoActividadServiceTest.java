package com.activehub.usecases.subirfotoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
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
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private SubirFotoActividadService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new SubirFotoActividadService(actividadRepository, auditService, tempDir.toString());

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
