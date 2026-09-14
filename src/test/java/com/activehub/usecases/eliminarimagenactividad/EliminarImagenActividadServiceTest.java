package com.activehub.usecases.eliminarimagenactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EliminarImagenActividadServiceTest {

    @Mock private ActividadRepository actividadRepository;
    @Mock private ActividadImagenRepository actividadImagenRepository;
    @Mock private AuditService auditService;

    private EliminarImagenActividadService service;
    private Path directorio;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() throws IOException {
        directorio = Files.createTempDirectory("ah-imagenes-del-test");
        service = new EliminarImagenActividadService(
                actividadRepository, actividadImagenRepository, auditService, directorio.toString());

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    private ActividadImagen imagen(String path) {
        ActividadImagen i = new ActividadImagen();
        i.setActividad(actividad);
        i.setPath(path);
        ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
        return i;
    }

    @Test
    void eliminar_borraLaFilaYElArchivo() throws IOException {
        ActividadImagen i = imagen("a.jpg");
        Files.write(directorio.resolve("a.jpg"), new byte[]{1});
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.findById(i.getId())).thenReturn(Optional.of(i));

        service.eliminar(actividadId, i.getId(), instructorId, false);

        verify(actividadImagenRepository).delete(i);
        assertThat(Files.exists(directorio.resolve("a.jpg"))).isFalse();
    }

    @Test
    void eliminar_siEraLaPortada_promueveLaSiguiente() {
        ActividadImagen portada = imagen("portada.jpg");
        ActividadImagen otra = imagen("otra.jpg");
        actividad.setFotoPath("portada.jpg");

        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.findById(portada.getId())).thenReturn(Optional.of(portada));
        when(actividadImagenRepository.findByActividadIdOrderByOrdenAsc(actividadId))
                .thenReturn(List.of(portada, otra));

        service.eliminar(actividadId, portada.getId(), instructorId, false);

        assertThat(actividad.getFotoPath()).isEqualTo("otra.jpg");
        verify(actividadRepository).save(actividad);
    }

    @Test
    void eliminar_ultimaImagenQueEraPortada_dejaLaActividadSinFoto() {
        ActividadImagen unica = imagen("unica.jpg");
        actividad.setFotoPath("unica.jpg");

        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.findById(unica.getId())).thenReturn(Optional.of(unica));
        when(actividadImagenRepository.findByActividadIdOrderByOrdenAsc(actividadId)).thenReturn(List.of(unica));

        service.eliminar(actividadId, unica.getId(), instructorId, false);

        assertThat(actividad.getFotoPath()).isNull();
    }

    @Test
    void eliminar_imagenDeOtraActividad_lanzaNoEncontrado() {
        Actividad ajena = new Actividad();
        ReflectionTestUtils.setField(ajena, "id", UUID.randomUUID());
        ActividadImagen i = imagen("a.jpg");
        i.setActividad(ajena);

        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.eliminar(actividadId, i.getId(), instructorId, false))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void eliminar_actividadAjena_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.eliminar(actividadId, UUID.randomUUID(), UUID.randomUUID(), false))
                .isInstanceOf(SinPermisoException.class);
    }
}
