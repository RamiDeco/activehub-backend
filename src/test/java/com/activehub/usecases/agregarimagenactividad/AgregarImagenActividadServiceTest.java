package com.activehub.usecases.agregarimagenactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.storage.AlmacenamientoDisco;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AgregarImagenActividadServiceTest {

    @Mock private ActividadRepository actividadRepository;
    @Mock private ActividadImagenRepository actividadImagenRepository;
    @Mock private AuditService auditService;

    private AgregarImagenActividadService service;
    private Path directorio;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() throws IOException {
        directorio = Files.createTempDirectory("ah-imagenes-test");
        AlmacenamientoDisco almacenamiento =
                new AlmacenamientoDisco(directorio.toString(), directorio.toString(), directorio.toString());
        service = new AgregarImagenActividadService(
                actividadRepository, actividadImagenRepository, auditService, almacenamiento);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    @AfterEach
    void limpiar() throws IOException {
        try (var archivos = Files.walk(directorio)) {
            archivos.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // temp dir
                }
            });
        }
    }

    private MockMultipartFile jpg() {
        return new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private void imagenSeGuardaConId() {
        when(actividadImagenRepository.saveAndFlush(any(ActividadImagen.class))).thenAnswer(inv -> {
            ActividadImagen i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            return i;
        });
    }

    @Test
    void agregar_guardaElArchivoYLaFila() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.countByActividadId(actividadId)).thenReturn(2);
        imagenSeGuardaConId();

        AgregarImagenActividadResponse response = service.agregar(actividadId, jpg(), instructorId, false);

        assertThat(response.actividadId()).isEqualTo(actividadId);
        assertThat(response.orden()).isEqualTo(2);
        assertThat(directorio.toFile().listFiles()).hasSize(1);
    }

    @Test
    void agregar_primeraImagenSinPortada_laDejaComoPortada() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.countByActividadId(actividadId)).thenReturn(0);
        imagenSeGuardaConId();

        service.agregar(actividadId, jpg(), instructorId, false);

        assertThat(actividad.getFotoPath()).isNotNull();
        verify(actividadRepository).save(actividad);
    }

    @Test
    void agregar_conPortadaExistente_noLaPisa() {
        actividad.setFotoPath("portada.jpg");
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.countByActividadId(actividadId)).thenReturn(1);
        imagenSeGuardaConId();

        service.agregar(actividadId, jpg(), instructorId, false);

        assertThat(actividad.getFotoPath()).isEqualTo("portada.jpg");
        verify(actividadRepository, never()).save(any());
    }

    @Test
    void agregar_alLlegarAlMaximo_lanzaValidacion() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.countByActividadId(actividadId))
                .thenReturn(AgregarImagenActividadService.MAXIMO_POR_ACTIVIDAD);

        assertThatThrownBy(() -> service.agregar(actividadId, jpg(), instructorId, false))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("máximo");

        assertThat(directorio.toFile().listFiles()).isEmpty();
    }

    @Test
    void agregar_formatoNoPermitido_lanzaValidacion() {
        var pdf = new MockMultipartFile("archivo", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.agregar(actividadId, pdf, instructorId, false))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void agregar_archivoVacio_lanzaValidacion() {
        var vacio = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.agregar(actividadId, vacio, instructorId, false))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void agregar_actividadAjena_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.agregar(actividadId, jpg(), UUID.randomUUID(), false))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void agregar_actividadAjenaPeroEsAdmin_loPermite() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(actividadImagenRepository.countByActividadId(actividadId)).thenReturn(1);
        imagenSeGuardaConId();

        assertThat(service.agregar(actividadId, jpg(), UUID.randomUUID(), true)).isNotNull();
    }

    @Test
    void agregar_actividadInexistente_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregar(actividadId, jpg(), instructorId, false))
                .isInstanceOf(NoEncontradoException.class);
    }
}
