package com.activehub.usecases.crearnivelintensidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NivelIntensidadDuplicadoException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** E4Ad-HU05 criterios 2 y 4. */
@ExtendWith(MockitoExtension.class)
class CrearNivelIntensidadServiceTest {

    @Mock
    private NivelIntensidadRepository nivelIntensidadRepository;
    @Mock
    private AuditService auditService;

    private CrearNivelIntensidadService service;

    @BeforeEach
    void setUp() {
        service = new CrearNivelIntensidadService(nivelIntensidadRepository, auditService);
    }

    private void persistenciaQueAsignaId() {
        when(nivelIntensidadRepository.saveAndFlush(any(NivelIntensidad.class))).thenAnswer(inv -> {
            NivelIntensidad n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(n, "createdAt", Instant.now());
            return n;
        });
    }

    @Test
    void crear_datosValidos_creaNivel() {
        when(nivelIntensidadRepository.existsByNombreIgnoreCaseAndDeletedFalse("Física extrema")).thenReturn(false);
        persistenciaQueAsignaId();

        CrearNivelIntensidadResponse response = service.crear(
                new CrearNivelIntensidadRequest("Física extrema", "Para atletas."), UUID.randomUUID());

        assertThat(response.id()).isNotNull();
        assertThat(response.nombre()).isEqualTo("Física extrema");
        assertThat(response.descripcion()).isEqualTo("Para atletas.");
    }

    @Test
    void crear_recortaEspaciosDeNombreYDescripcion() {
        when(nivelIntensidadRepository.existsByNombreIgnoreCaseAndDeletedFalse("  Física extrema  ".trim()))
                .thenReturn(false);
        persistenciaQueAsignaId();

        CrearNivelIntensidadResponse response = service.crear(
                new CrearNivelIntensidadRequest("  Física extrema  ", "  Para atletas.  "), UUID.randomUUID());

        assertThat(response.nombre()).isEqualTo("Física extrema");
        assertThat(response.descripcion()).isEqualTo("Para atletas.");
    }

    @Test
    void crear_registraAuditoria() {
        when(nivelIntensidadRepository.existsByNombreIgnoreCaseAndDeletedFalse(any())).thenReturn(false);
        persistenciaQueAsignaId();
        UUID actorId = UUID.randomUUID();

        service.crear(new CrearNivelIntensidadRequest("Física extrema", "Para atletas."), actorId);

        verify(auditService).registrar(
                eq(actorId), eq(AuditAccion.NIVEL_INTENSIDAD_CREADO), eq("NivelIntensidad"), any(), eq(null));
    }

    @Test
    void crear_nombreDuplicado_lanzaDuplicadoYNoPersiste() {
        when(nivelIntensidadRepository.existsByNombreIgnoreCaseAndDeletedFalse("Física baja")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(
                new CrearNivelIntensidadRequest("Física baja", "otra desc"), UUID.randomUUID()))
                .isInstanceOf(NivelIntensidadDuplicadoException.class);

        verify(nivelIntensidadRepository, never()).saveAndFlush(any());
    }
}
