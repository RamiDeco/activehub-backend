package com.activehub.usecases.inscribirse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CuentaMercadoPagoNoVinculadaException;
import com.activehub.shared.error.InscripcionYaExisteException;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.error.PagoRechazadoException;
import com.activehub.shared.error.SinCuposDisponiblesException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.CuentaMercadoPagoGuard;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.InstructorVerificadoGuard;

@ExtendWith(MockitoExtension.class)
class InscribirseServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-01T00:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private InstructorVerificadoGuard instructorVerificadoGuard;
    @Mock
    private CuentaMercadoPagoGuard cuentaMercadoPagoGuard;

    private InscribirseService service;
    private UUID claseId;
    private UUID alumnoId;
    private UUID instructorId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new InscribirseService(
                claseRepository, inscripcionRepository, pagoRepository, usuarioRepository, paymentGateway, auditService,
                notificacionService, instructorVerificadoGuard, cuentaMercadoPagoGuard, clock);

        claseId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        instructorId = UUID.randomUUID();

        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setPrecio(new BigDecimal("4500"));
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        clase = new Clase();
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(AHORA.plus(Duration.ofDays(2)));
        clase.setActividad(actividad);
        // El cobro sale del precio de la CLASE, no del de la actividad (V23): cada clase nace
        // con el precio vigente al crearse y no cambia si despues se edita la actividad.
        clase.setPrecio(new BigDecimal("4500"));
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    @AfterEach
    void limpiarSincronizacion() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void stubGuardados() {
        stubInscripcionGuardada();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
    }

    private void stubInscripcionGuardada() {
        when(inscripcionRepository.saveAndFlush(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            if (i.getId() == null) {
                ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            }
            ReflectionTestUtils.setField(i, "createdAt", AHORA);
            return i;
        });
    }

    private static final DatosPagoRequest DATOS_PAGO = new DatosPagoRequest("card-tok-1", "master", "credit_card", 1);

    private void stubCuentaVinculada() {
        when(cuentaMercadoPagoGuard.exigirCuentaVinculada(instructorId)).thenReturn("TEST-seller-token");
    }

    private void stubAutorizado() {
        when(paymentGateway.autorizar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaymentGateway.ResultadoPago("ORD01TEST", "action_required", "waiting_capture"));
    }

    private void stubInscripcionNueva() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
    }

    @Test
    void inscribirse_mercadoPago_autorizaConElTokenDelInstructorYDejaPagoRetenido() {
        stubInscripcionNueva();
        stubCuentaVinculada();
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        Usuario alumno = new Usuario();
        alumno.setEmail("alumno@test.com");
        when(usuarioRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        stubAutorizado();
        stubGuardados();

        InscribirseResponse response = service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId);

        assertThat(response.estado()).isEqualTo("Inscripto");
        var tarjeta = ArgumentCaptor.forClass(PaymentGateway.DatosTarjeta.class);
        var key = ArgumentCaptor.forClass(String.class);
        // El monto es el precio de la clase y la referencia la inscripcion: nada de eso viene del cliente.
        verify(paymentGateway).autorizar(eq("TEST-seller-token"), tarjeta.capture(), eq(new BigDecimal("4500")),
                eq(BigDecimal.ZERO), eq(response.id().toString()), key.capture());
        assertThat(tarjeta.getValue().token()).isEqualTo("card-tok-1");
        assertThat(tarjeta.getValue().emailPagador()).isEqualTo("alumno@test.com");

        var pago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pago.capture());
        assertThat(pago.getValue().getEstado()).isEqualTo(EstadoPago.Retenido);
        assertThat(pago.getValue().getMpOrderId()).isEqualTo("ORD01TEST");
        assertThat(pago.getValue().getReferenciaExterna()).isEqualTo("ORD01TEST");
        assertThat(pago.getValue().getIdempotencyKey()).isEqualTo(key.getValue());
        assertThat(pago.getValue().getMontoInstructor()).isEqualByComparingTo("4500");
        assertThat(pago.getValue().getAutorizadoHasta()).isEqualTo(AHORA.plus(Duration.ofDays(5)));
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.INSCRIPCION_CONFIRMADA), any(), any(), any());
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.NUEVA_INSCRIPCION), any(), any(), eq(Destino.clase(claseId)));
    }

    @Test
    void inscribirse_mercadoPago_mismoPedidoReintentado_usaLaMismaIdempotencyKey() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        stubCuentaVinculada();
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        Inscripcion existente = new Inscripcion();
        existente.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        ReflectionTestUtils.setField(existente, "id", UUID.randomUUID());
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(existente));
        stubAutorizado();
        stubGuardados();

        service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId);
        existente.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId);
        existente.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        service.inscribirse(claseId, new InscribirseRequest("Mercado Pago",
                new DatosPagoRequest("card-tok-2", "visa", "credit_card", 1)), alumnoId);

        var keys = ArgumentCaptor.forClass(String.class);
        verify(paymentGateway, org.mockito.Mockito.times(3)).autorizar(any(), any(), any(), any(), any(), keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
        assertThat(keys.getAllValues().get(2)).isNotEqualTo(keys.getAllValues().get(0));
    }

    @Test
    void inscribirse_mercadoPago_sinDatosPago_lanzaValidacionSinOcuparCupo() {
        stubInscripcionNueva();

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Mercado Pago"), alumnoId))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Los datos de pago son obligatorios para Mercado Pago.");
        verify(claseRepository, never()).ocuparCupo(any());
    }

    @Test
    void inscribirse_mercadoPago_instructorSinCuentaVinculada_lanza409SinOcuparCupo() {
        stubInscripcionNueva();
        when(cuentaMercadoPagoGuard.exigirCuentaVinculada(instructorId)).thenThrow(new CuentaMercadoPagoNoVinculadaException());

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId))
                .isInstanceOf(CuentaMercadoPagoNoVinculadaException.class)
                .hasMessageContaining("solo acepta pago en efectivo");
        verify(claseRepository, never()).ocuparCupo(any());
        verify(paymentGateway, never()).autorizar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void inscribirse_mercadoPago_rechazado_propagaSinGuardarPago() {
        stubInscripcionNueva();
        stubCuentaVinculada();
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        stubInscripcionGuardada();
        when(paymentGateway.autorizar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PagoRechazadoException("cc_rejected_insufficient_amount"));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId))
                .isInstanceOf(PagoRechazadoException.class)
                .hasMessage("La tarjeta no tiene fondos suficientes.");
        // La transaccion revierte la inscripcion y el cupo; aca alcanza con que el pago no se grabe.
        verify(pagoRepository, never()).save(any());
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void inscribirse_mercadoPago_sinAutorizacionConfirmada_lanzaPagoException() {
        stubInscripcionNueva();
        stubCuentaVinculada();
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        stubInscripcionGuardada();
        when(paymentGateway.autorizar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaymentGateway.ResultadoPago("ORD01TEST", "pending", "pending_review"));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId))
                .isInstanceOf(PagoException.class);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void inscribirse_efectivo_creaPagoPendienteYPagoEfectivo_sinLlamarGateway() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        stubGuardados();

        InscribirseResponse response = service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId);

        assertThat(response.estado()).isEqualTo("PagoPendiente");
        verify(paymentGateway, never()).autorizar(any(), any(), any(), any(), any(), any());
        // KAN-57: el efectivo no depende de que el instructor tenga Mercado Pago.
        verify(cuentaMercadoPagoGuard, never()).exigirCuentaVinculada(any());
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.INSCRIPCION_CONFIRMADA), any(), any(), any());
    }

    @Test
    void inscribirse_conPreinscripcionExistente_reutilizaLaMismaFila() {
        Inscripcion existente = new Inscripcion();
        existente.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        UUID inscripcionId = UUID.randomUUID();
        ReflectionTestUtils.setField(existente, "id", inscripcionId);
        ReflectionTestUtils.setField(existente, "createdAt", AHORA.minus(Duration.ofDays(6)));

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(existente));
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        stubGuardados();
        stubCuentaVinculada();
        stubAutorizado();

        InscribirseResponse response = service.inscribirse(claseId, new InscribirseRequest("Mercado Pago", DATOS_PAGO), alumnoId);

        assertThat(response.id()).isEqualTo(inscripcionId);
        verify(inscripcionRepository).saveAndFlush(eq(existente));
        verify(usuarioRepository, never()).getReferenceById(any());
        verify(paymentGateway).autorizar(any(), any(), any(), any(), eq(inscripcionId.toString()), any());
    }

    @Test
    void inscribirse_masDe4Dias_lanzaValidacion() {
        clase.setFechaHora(AHORA.plus(Duration.ofDays(10)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void inscribirse_menosDe1Hora_lanzaValidacion() {
        clase.setFechaHora(AHORA.plus(Duration.ofMinutes(30)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void inscribirse_sinCupos_lanzaSinCuposDisponibles() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
        when(claseRepository.ocuparCupo(claseId)).thenReturn(0);

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(SinCuposDisponiblesException.class);

        verify(inscripcionRepository, never()).saveAndFlush(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void inscribirse_yaTieneInscripcionDefinitivaActiva_lanzaYaExiste() {
        Inscripcion existente = new Inscripcion();
        existente.setEstado(EstadoInscripcion.INSCRIPTO);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(InscripcionYaExisteException.class);

        verify(claseRepository, never()).ocuparCupo(any());
    }
}
