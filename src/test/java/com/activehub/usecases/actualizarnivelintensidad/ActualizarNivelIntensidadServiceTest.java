package com.activehub.usecases.actualizarnivelintensidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NivelIntensidadDuplicadoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** E4Ad-HU05 criterios 4 y 5. */
@ExtendWith(MockitoExtension.class)
class ActualizarNivelIntensidadServiceTest {

    @Mock
    private NivelIntensidadRepository nivelIntensidadRepository;
    @Mock
    private AuditService auditService;

    private ActualizarNivelIntensidadService service;
    private UUID nivelId;
    private NivelIntensidad nivel;

    @BeforeEach
    void setUp() {
        service = new ActualizarNivelIntensidadService(nivelIntensidadRepository, auditService);
        nivelId = UUID.randomUUID();
        nivel = nivelCon(nivelId, "Física baja", "Bajo impacto.");
    }

    private static NivelIntensidad nivelCon(UUID id, String nombre, String descripcion) {
        NivelIntensidad n = new NivelIntensidad();
        n.setNombre(nombre);
        n.setDescripcion(descripcion);
        ReflectionTestUtils.setField(n, "id", id);
        return n;
    }

    @Test
    void actualizar_datosValidos_cambiaNombreYDescripcion() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.of(nivel));
        when(nivelIntensidadRepository.findByNombreIgnoreCaseAndDeletedFalse("Física suave"))
                .thenReturn(Optional.empty());

        ActualizarNivelIntensidadResponse response = service.actualizar(
                nivelId, new ActualizarNivelIntensidadRequest("Física suave", "Nueva desc."), UUID.randomUUID());

        assertThat(response.nombre()).isEqualTo("Física suave");
        assertThat(response.descripcion()).isEqualTo("Nueva desc.");
        assertThat(nivel.getNombre()).isEqualTo("Física suave");
    }

    @Test
    void actualizar_mismoNivelConSuPropioNombre_noLoTomaComoDuplicado() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.of(nivel));
        // El único que tiene ese nombre es él mismo: editar solo la descripción tiene que andar.
        when(nivelIntensidadRepository.findByNombreIgnoreCaseAndDeletedFalse("Física baja"))
                .thenReturn(Optional.of(nivel));

        ActualizarNivelIntensidadResponse response = service.actualizar(
                nivelId, new ActualizarNivelIntensidadRequest("Física baja", "Descripción corregida."),
                UUID.randomUUID());

        assertThat(response.descripcion()).isEqualTo("Descripción corregida.");
    }

    @Test
    void actualizar_nombreDeOtroNivel_lanzaDuplicadoYNoGuarda() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.of(nivel));
        when(nivelIntensidadRepository.findByNombreIgnoreCaseAndDeletedFalse("Física alta"))
                .thenReturn(Optional.of(nivelCon(UUID.randomUUID(), "Física alta", "otra")));

        assertThatThrownBy(() -> service.actualizar(
                nivelId, new ActualizarNivelIntensidadRequest("Física alta", "desc"), UUID.randomUUID()))
                .isInstanceOf(NivelIntensidadDuplicadoException.class);

        verify(nivelIntensidadRepository, never()).save(any());
    }

    @Test
    void actualizar_nivelInexistente_lanzaNoEncontrado() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                nivelId, new ActualizarNivelIntensidadRequest("Física suave", "desc"), UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
