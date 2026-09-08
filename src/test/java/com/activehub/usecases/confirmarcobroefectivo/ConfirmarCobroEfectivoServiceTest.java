package com.activehub.usecases.confirmarcobroefectivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConfirmarCobroEfectivoServiceTest {

    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private AuditService auditService;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private ConfirmarCobroEfectivoService service;
    private UUID inscripcionId;
    private UUID instructorId;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
<<<<<<< Updated upstream
        service = new ConfirmarCobroEfectivoService(inscripcionRepository, auditService);
=======
        service = new ConfirmarCobroEfectivoService(inscripcionRepository, auditService, notificacionService, instructorVerificadoGuard);
>>>>>>> Stashed changes

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);

        Clase clase = new Clase();
        clase.setActividad(actividad);

        inscripcionId = UUID.randomUUID();
        inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.PAGO_PENDIENTE);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
    }

    @Test
    void confirmar_pagoPendiente_pasaAInscripto() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        ConfirmarCobroEfectivoResponse response = service.confirmar(inscripcionId, instructorId);

        assertThat(response.estado()).isEqualTo("Inscripto");
    }

    @Test
    void confirmar_noEsElDueño_lanzaSinPermiso() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.confirmar(inscripcionId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void confirmar_noEstaPagoPendiente_lanzaValidacion() {
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.confirmar(inscripcionId, instructorId))
                .isInstanceOf(ValidacionException.class);
    }
}
