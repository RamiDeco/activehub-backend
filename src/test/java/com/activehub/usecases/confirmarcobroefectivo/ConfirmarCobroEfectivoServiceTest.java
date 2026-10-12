package com.activehub.usecases.confirmarcobroefectivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.time.Instant;
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
    @Mock
    private NotificacionService notificacionService;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private ConfirmarCobroEfectivoService service;
    private UUID inscripcionId;
    private UUID instructorId;
    private UUID alumnoId;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        service = new ConfirmarCobroEfectivoService(inscripcionRepository, auditService, notificacionService, instructorVerificadoGuard);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.parse("2026-08-20T12:00:00Z"));

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        inscripcionId = UUID.randomUUID();
        inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setAlumno(alumno);
        inscripcion.setEstado(EstadoInscripcion.PAGO_PENDIENTE);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
        inscripcion.setPago(pago(MetodoPago.EFECTIVO, EstadoPago.Efectivo));
    }

    private Pago pago(MetodoPago metodo, EstadoPago estado) {
        Pago p = new Pago();
        p.setInscripcion(inscripcion);
        p.setMetodo(metodo);
        p.setEstado(estado);
        return p;
    }

    @Test
    void confirmar_pagoPendiente_pasaAInscripto() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        ConfirmarCobroEfectivoResponse response = service.confirmar(inscripcionId, instructorId);

        assertThat(response.estado()).isEqualTo("Inscripto");
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.COBRO_EFECTIVO_CONFIRMADO), any(), eq(inscripcionId), eq(Destino.inscripcion(inscripcionId)));
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

    @Test
    void confirmar_pagoDeMercadoPagoEnRevision_lanzaValidacionYNoInscribe() {
        // KAN-58: tambien esta en PagoPendiente, pero lo resuelve Mercado Pago, no el instructor.
        inscripcion.setPago(pago(MetodoPago.MERCADO_PAGO, EstadoPago.EnRevision));
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.confirmar(inscripcionId, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Mercado Pago");
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.PAGO_PENDIENTE);
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }
}
