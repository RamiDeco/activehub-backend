package com.activehub.usecases.actualizarmisintereses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Desde V19 un interés es un TipoActividad, así que arrastra su categoría: eso es lo que
 * permite que "Trekking" sepa que es de Aventura sin comparar texto.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActualizarMisInteresesServiceTest {

    @Mock private PerfilAlumnoRepository perfilAlumnoRepository;
    @Mock private TipoActividadRepository tipoActividadRepository;
    @Mock private AuditService auditService;

    private ActualizarMisInteresesService service;
    private UUID usuarioId;
    private PerfilAlumno perfil;
    private TipoActividad trekking;
    private TipoActividad yoga;

    @BeforeEach
    void setUp() {
        service = new ActualizarMisInteresesService(perfilAlumnoRepository, tipoActividadRepository, auditService);

        usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        Categoria aventura = categoria("Aventura");
        Categoria bienestar = categoria("Bienestar");
        trekking = tipo("Trekking", aventura);
        yoga = tipo("Yoga", bienestar);

        perfil = new PerfilAlumno(usuario, List.of(trekking));
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());

        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(tipoActividadRepository.findById(trekking.getId())).thenReturn(Optional.of(trekking));
        when(tipoActividadRepository.findById(yoga.getId())).thenReturn(Optional.of(yoga));
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    private TipoActividad tipo(String nombre, Categoria categoria) {
        TipoActividad t = new TipoActividad();
        t.setNombre(nombre);
        t.setCategoria(categoria);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        return t;
    }

    @Test
    void actualizar_reemplazaLaListaCompleta() {
        var response = service.actualizar(
                usuarioId, new ActualizarMisInteresesRequest(List.of(yoga.getId())));

        assertThat(response.intereses()).extracting(ActualizarMisInteresesResponse.Interes::nombre)
                .containsExactly("Yoga");
        assertThat(perfil.getIntereses()).containsExactly(yoga);
        verify(perfilAlumnoRepository).save(perfil);
    }

    @Test
    void actualizar_devuelveLaCategoriaDeCadaInteres() {
        // Es el punto del cambio: el interés ya sabe a qué categoría pertenece.
        var response = service.actualizar(
                usuarioId, new ActualizarMisInteresesRequest(List.of(trekking.getId())));

        assertThat(response.intereses()).hasSize(1);
        assertThat(response.intereses().get(0).categoria()).isEqualTo("Aventura");
        assertThat(response.intereses().get(0).tipoActividadId()).isEqualTo(trekking.getId());
    }

    @Test
    void actualizar_listaVacia_dejaAlAlumnoSinIntereses() {
        var response = service.actualizar(usuarioId, new ActualizarMisInteresesRequest(List.of()));

        assertThat(response.intereses()).isEmpty();
        assertThat(perfil.getIntereses()).isEmpty();
    }

    @Test
    void actualizar_idRepetido_noDuplica() {
        // La tabla tiene UNIQUE (perfil_alumno_id, tipo_actividad_id).
        var response = service.actualizar(
                usuarioId, new ActualizarMisInteresesRequest(List.of(yoga.getId(), yoga.getId())));

        assertThat(response.intereses()).hasSize(1);
    }

    @Test
    void actualizar_tipoInexistente_lanzaValidacion() {
        UUID fantasma = UUID.randomUUID();
        when(tipoActividadRepository.findById(fantasma)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                usuarioId, new ActualizarMisInteresesRequest(List.of(fantasma))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya no existe");

        verify(perfilAlumnoRepository, never()).save(any());
    }

    @Test
    void actualizar_audita() {
        service.actualizar(usuarioId, new ActualizarMisInteresesRequest(List.of(yoga.getId())));

        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.INTERESES_ACTUALIZADOS), eq("PerfilAlumno"), any(), any());
    }

    @Test
    void actualizar_sinPerfilDeAlumno_lanzaNoEncontrado() {
        UUID instructorId = UUID.randomUUID();
        when(perfilAlumnoRepository.findByUsuarioId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                instructorId, new ActualizarMisInteresesRequest(List.of(yoga.getId()))))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessageContaining("alumno");

        verify(perfilAlumnoRepository, never()).save(any());
    }
}
