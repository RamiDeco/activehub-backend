package com.activehub.usecases.recibirnotificacionmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.CuentaMercadoPagoGuard;
import com.activehub.shared.payments.FirmaWebhookMercadoPago;
import com.activehub.shared.payments.OperacionesPagoMercadoPago;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.payments.PaymentGateway.ResultadoPago;
import com.activehub.usecases.recibirnotificacionmercadopago.RecibirNotificacionMercadoPagoService.Resultado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecibirNotificacionMercadoPagoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-08T15:00:00Z");
    private static final String ORDEN = "ORDTST01ABC";
    private static final String TOKEN = "APP_USR-token-del-vendedor";
    private static final RecibirNotificacionMercadoPagoRequest AVISO =
            new RecibirNotificacionMercadoPagoRequest(ORDEN, "order", "ts=1,v1=abc", "req-1");

    @Mock private FirmaWebhookMercadoPago firma;
    @Mock private PagoRepository pagoRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private ClaseRepository claseRepository;
    @Mock private CuentaMercadoPagoGuard cuentaGuard;
    @Mock private PaymentGateway paymentGateway;
    @Mock private OperacionesPagoMercadoPago operacionesPago;
    @Mock private AuditService auditService;
    @Mock private NotificacionService notificacionService;

    private RecibirNotificacionMercadoPagoService service;
    private Pago pago;
    private Inscripcion inscripcion;
    private UUID instructorId;
    private UUID alumnoId;
    private UUID claseId;

    @BeforeEach
    void setUp() {
        service = new RecibirNotificacionMercadoPagoService(firma, pagoRepository, inscripcionRepository,
                claseRepository, cuentaGuard, paymentGateway, operacionesPago, auditService, notificacionService,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        instructorId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        claseId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);
        Actividad actividad = new Actividad();
        actividad.setNombre("Fútbol 5 recreativo");
        actividad.setInstructor(instructor);
        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(AHORA.plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(clase, "id", claseId);

        inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.PAGO_PENDIENTE);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());

        pago = new Pago();
        pago.setInscripcion(inscripcion);
        pago.setEstado(EstadoPago.EnRevision);
        pago.setMpOrderId(ORDEN);
        ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
    }

    private void stubOrdenEnMp(String status, String detail) {
        when(firma.esValida("ts=1,v1=abc", "req-1", ORDEN)).thenReturn(true);
        when(pagoRepository.bloquearPorMpOrderId(ORDEN)).thenReturn(Optional.of(pago));
        when(pagoRepository.findConDetalleByMpOrderId(ORDEN)).thenReturn(Optional.of(pago));
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.consultar(TOKEN, ORDEN)).thenReturn(new ResultadoPago(ORDEN, status, detail));
    }

    @Test
    void firmaInvalida_noProcesaNada() {
        when(firma.esValida(any(), any(), any())).thenReturn(false);

        assertThat(service.procesar(AVISO)).isEqualTo(Resultado.FIRMA_INVALIDA);
        verifyNoInteractions(pagoRepository, paymentGateway, auditService, notificacionService, claseRepository);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.EnRevision);
    }

    @Test
    void enRevisionAprobadoPorMp_confirmaLaInscripcionAuditaYAvisa() {
        stubOrdenEnMp("action_required", "waiting_capture");

        assertThat(service.procesar(AVISO)).isEqualTo(Resultado.PROCESADA);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        assertThat(pago.getStatusDetail()).isEqualTo("waiting_capture");
        assertThat(pago.getAutorizadoHasta()).isEqualTo(AHORA.plus(Duration.ofDays(5)));
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.INSCRIPTO);
        verify(auditService).registrar(null, AuditAccion.PAGO_CONFIRMADO_MP, "Pago", pago.getId(), ORDEN);
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.INSCRIPCION_CONFIRMADA), any(), any(), any());
    }

    @Test
    void laMismaNotificacionDosVeces_unSoloCambioYUnaSolaAuditoria() {
        stubOrdenEnMp("action_required", "waiting_capture");

        service.procesar(AVISO);
        service.procesar(AVISO);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        verify(auditService, times(1)).registrar(any(), eq(AuditAccion.PAGO_CONFIRMADO_MP), any(), any(), any());
        verify(notificacionService, times(1)).notificar(any(), any(), any(), any(), any());
        verify(pagoRepository, times(1)).save(pago);
    }

    @Test
    void enRevisionRechazadoPorMp_cancelaLaInscripcionYLiberaElCupo() {
        stubOrdenEnMp("failed", "failed");

        service.procesar(AVISO);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Rechazado);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        verify(claseRepository).liberarCupo(claseId);
        verify(auditService).registrar(null, AuditAccion.PAGO_RECHAZADO_MP, "Pago", pago.getId(), ORDEN);
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.INSCRIPCION_CANCELADA), any(), any(), any());
    }

    @Test
    void enRevisionQueVence_quedaCanceladoYLiberaElCupo() {
        stubOrdenEnMp("expired", "expired");

        service.procesar(AVISO);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(claseRepository).liberarCupo(claseId);
    }

    @Test
    void mpTodaviaNoDecidio_noCambiaNada() {
        stubOrdenEnMp("processing", "in_review");

        assertThat(service.procesar(AVISO)).isEqualTo(Resultado.PROCESADA);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.EnRevision);
        verifyNoInteractions(auditService, notificacionService, claseRepository);
    }

    @Test
    void mpApruebaTardeConLaInscripcionYaCancelada_cancelaLaOrdenEnMp() {
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        stubOrdenEnMp("action_required", "waiting_capture");

        service.procesar(AVISO);

        verify(operacionesPago).reintegrar(pago, instructorId, null);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void reembolsoHechoDesdeElPanelDeMp_marcaReembolsado() {
        pago.setEstado(EstadoPago.Retenido);
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        stubOrdenEnMp("refunded", "refunded");

        service.procesar(AVISO);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Reembolsado);
        verify(auditService).registrar(null, AuditAccion.PAGO_REEMBOLSADO, "Pago", pago.getId(), "desde el panel de Mercado Pago");
    }

    @Test
    void ordenQueNoEsNuestra_seIgnoraSinConsultarAMp() {
        when(firma.esValida(any(), any(), any())).thenReturn(true);
        when(pagoRepository.bloquearPorMpOrderId(ORDEN)).thenReturn(Optional.empty());

        assertThat(service.procesar(AVISO)).isEqualTo(Resultado.IGNORADA);
        verifyNoInteractions(paymentGateway, auditService);
    }

    @Test
    void bloqueaLaFilaDelPagoAntesDeLeerloYDeConsultarAMp() {
        // KAN-66: si hay un reintegro de ActiveHub en curso, el bloqueo hace esperar al webhook.
        stubOrdenEnMp("action_required", "waiting_capture");

        service.procesar(AVISO);

        var orden = org.mockito.Mockito.inOrder(pagoRepository, paymentGateway);
        orden.verify(pagoRepository).bloquearPorMpOrderId(ORDEN);
        orden.verify(pagoRepository).findConDetalleByMpOrderId(ORDEN);
        orden.verify(paymentGateway).consultar(TOKEN, ORDEN);
    }

    @Test
    void ordenReembolsadaCuyoPagoYaFiguraReembolsado_noAuditaDeNuevo() {
        // KAN-66: el caso que duplicaba la auditoria, visto despues de esperar el bloqueo: el
        // reembolso lo pidio ActiveHub y ya quedo registrado.
        pago.setEstado(EstadoPago.Reembolsado);
        stubOrdenEnMp("refunded", "refunded");

        assertThat(service.procesar(AVISO)).isEqualTo(Resultado.PROCESADA);

        verifyNoInteractions(auditService);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void otroTipoDeNotificacion_seIgnora() {
        when(firma.esValida(any(), any(), any())).thenReturn(true);

        assertThat(service.procesar(new RecibirNotificacionMercadoPagoRequest("123", "payment", "s", "r")))
                .isEqualTo(Resultado.IGNORADA);
        verifyNoInteractions(pagoRepository, paymentGateway);
    }
}
