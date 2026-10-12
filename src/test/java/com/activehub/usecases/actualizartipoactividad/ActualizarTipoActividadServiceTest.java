package com.activehub.usecases.actualizartipoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.TipoActividadDuplicadoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActualizarTipoActividadServiceTest {

    @Mock private TipoActividadRepository tipoActividadRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private AuditService auditService;

    private ActualizarTipoActividadService service;

    private UUID tipoId;
    private UUID categoriaId;
    private UUID actorId;
    private TipoActividad tipo;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new ActualizarTipoActividadService(tipoActividadRepository, categoriaRepository, auditService);

        tipoId = UUID.randomUUID();
        categoriaId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        categoria = new Categoria();
        categoria.setNombre("Deportivas");
        ReflectionTestUtils.setField(categoria, "id", categoriaId);

        tipo = new TipoActividad();
        tipo.setNombre("Running");
        tipo.setCategoria(categoria);
        ReflectionTestUtils.setField(tipo, "id", tipoId);
    }

    @Test
    void actualizar_datosValidos_guardaYAudita() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse("Trail running", categoriaId))
                .thenReturn(Optional.empty());

        var response = service.actualizar(tipoId, new ActualizarTipoActividadRequest("Trail running", categoriaId), actorId);

        assertThat(response.nombre()).isEqualTo("Trail running");
        assertThat(response.categoriaId()).isEqualTo(categoriaId);
        verify(tipoActividadRepository).save(tipo);
        verify(auditService).registrar(
                eq(actorId), eq(AuditAccion.TIPO_ACTIVIDAD_ACTUALIZADO), eq("TipoActividad"), eq(tipoId), any());
    }

    @Test
    void actualizar_recortaEspaciosDelNombre() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        var response = service.actualizar(tipoId, new ActualizarTipoActividadRequest("  Yoga  ", categoriaId), actorId);

        assertThat(response.nombre()).isEqualTo("Yoga");
    }

    @Test
    void actualizar_mismoNombreEnOtroTipoDeLaCategoria_lanzaDuplicado() {
        TipoActividad otro = new TipoActividad();
        otro.setNombre("Yoga");
        ReflectionTestUtils.setField(otro, "id", UUID.randomUUID());

        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse("Yoga", categoriaId))
                .thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.actualizar(
                tipoId, new ActualizarTipoActividadRequest("Yoga", categoriaId), actorId))
                .isInstanceOf(TipoActividadDuplicadoException.class);

        verify(tipoActividadRepository, never()).save(any());
    }

    @Test
    void actualizar_conservandoSuPropioNombre_noEsDuplicado() {
        // El chequeo tiene que excluir la propia fila: renombrar "Running" a "Running" con
        // otra categoría no puede chocar consigo mismo.
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse("Running", categoriaId))
                .thenReturn(Optional.of(tipo));

        var response = service.actualizar(tipoId, new ActualizarTipoActividadRequest("Running", categoriaId), actorId);

        assertThat(response.id()).isEqualTo(tipoId);
        verify(tipoActividadRepository).save(tipo);
    }

    @Test
    void actualizar_tipoInexistente_lanzaNoEncontrado() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                tipoId, new ActualizarTipoActividadRequest("Running", categoriaId), actorId))
                .isInstanceOf(NoEncontradoException.class);

        verify(tipoActividadRepository, never()).save(any());
    }

    @Test
    void actualizar_categoriaInexistente_lanzaNoEncontrado() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                tipoId, new ActualizarTipoActividadRequest("Running", categoriaId), actorId))
                .isInstanceOf(NoEncontradoException.class);

        verify(tipoActividadRepository, never()).save(any());
    }
}
