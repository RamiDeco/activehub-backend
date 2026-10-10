package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CuentaMercadoPagoNoVinculadaException;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.payments.PaymentGateway.ResultadoPago;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OperacionesPagoMercadoPagoTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T12:00:00Z");
    private static final String TOKEN = "seller-token";
    private static final String ORDEN = "ORD01ABC";

    @Mock private PaymentGateway paymentGateway;
    @Mock private CuentaMercadoPagoGuard cuentaGuard;
    @Mock private PagoRepository pagoRepository;
    @Mock private AuditService auditService;
    @Mock private EntityManager entityManager;

    private OperacionesPagoMercadoPago operaciones;
    private UUID instructorId;
    private UUID actorId;
    private Pago pago;

    @BeforeEach
    void setUp() {
        operaciones = new OperacionesPagoMercadoPago(
                paymentGateway, cuentaGuard, pagoRepository, auditService, Clock.fixed(AHORA, ZoneOffset.UTC), entityManager);
        instructorId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMpOrderId(ORDEN);
        pago.setIdempotencyKey("idem-1");
        pago.setAutorizadoHasta(AHORA.plus(Duration.ofDays(2)));
        ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
    }

    // --- capturar ---------------------------------------------------------------------

    @Test
    void capturar_autorizadoVigente_capturaConElTokenDelInstructorYSigueRetenido() {
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.capturar(TOKEN, ORDEN, "idem-1:capture"))
                .thenReturn(new ResultadoPago(ORDEN, "processed", "accredited"));

        operaciones.capturar(pago, instructorId);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        assertThat(pago.getCapturadoAt()).isEqualTo(AHORA);
        assertThat(pago.getAutorizadoHasta()).isNull();
        assertThat(pago.getStatusDetail()).isEqualTo("accredited");
        verify(pagoRepository).save(pago);
        verify(auditService).registrar(isNull(), eq(AuditAccion.PAGO_CAPTURADO), eq("Pago"), eq(pago.getId()), isNull());
    }

    @Test
    void capturar_yaCapturado_noVuelveACapturar() {
        pago.setCapturadoAt(AHORA.minus(Duration.ofHours(1)));

        operaciones.capturar(pago, instructorId);

        verifyNoInteractions(paymentGateway, cuentaGuard, pagoRepository);
    }

    @Test
    void capturar_autorizacionVencida_pasaACanceladoSinLlamarAMercadoPago() {
        pago.setAutorizadoHasta(AHORA.minus(Duration.ofMinutes(1)));

        operaciones.capturar(pago, instructorId);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        assertThat(pago.getCapturadoAt()).isNull();
        verifyNoInteractions(paymentGateway);
        verify(pagoRepository).save(pago);
    }

    @Test
    void capturar_mercadoPagoFalla_propagaYNoMarcaCapturado() {
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.capturar(any(), any(), any())).thenThrow(new PagoException("caido"));

        assertThatThrownBy(() -> operaciones.capturar(pago, instructorId)).isInstanceOf(PagoException.class);

        assertThat(pago.getCapturadoAt()).isNull();
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void capturar_pagoSimuladoViejo_soloMarcaCapturadoSinLlamarAMercadoPago() {
        pago.setMpOrderId(null);

        operaciones.capturar(pago, instructorId);

        assertThat(pago.getCapturadoAt()).isEqualTo(AHORA);
        verifyNoInteractions(paymentGateway, cuentaGuard);
    }

    // --- reintegrar -------------------------------------------------------------------

    @Test
    void reintegrar_sinCapturar_cancelaLaAutorizacion() {
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.cancelarAutorizacion(TOKEN, ORDEN, "idem-1:cancel"))
                .thenReturn(new ResultadoPago(ORDEN, "canceled", "canceled"));

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Cancelado);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway, never()).reembolsar(any(), any(), any());
        verify(pagoRepository).save(pago);
    }

    @Test
    void reintegrar_capturado_reembolsaYAudita() {
        pago.setCapturadoAt(AHORA.minus(Duration.ofHours(3)));
        pago.setAutorizadoHasta(null);
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.reembolsar(TOKEN, ORDEN, "idem-1:refund"))
                .thenReturn(new ResultadoPago(ORDEN, "refunded", "refunded"));

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Reembolsado);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Reembolsado);
        verify(paymentGateway, never()).cancelarAutorizacion(any(), any(), any());
        verify(auditService).registrar(eq(actorId), eq(AuditAccion.PAGO_REEMBOLSADO), eq("Pago"), eq(pago.getId()), isNull());
    }

    @Test
    void reintegrar_bloqueaLaFilaDelPagoAntesDeLlamarAMercadoPago() {
        // KAN-66: el bloqueo tiene que estar tomado cuando MP manda el webhook, o sea antes del pedido.
        pago.setCapturadoAt(AHORA.minus(Duration.ofHours(3)));
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.reembolsar(TOKEN, ORDEN, "idem-1:refund"))
                .thenReturn(new ResultadoPago(ORDEN, "refunded", "refunded"));

        when(entityManager.contains(pago)).thenReturn(true);

        operaciones.reintegrar(pago, instructorId, actorId);

        var orden = org.mockito.Mockito.inOrder(entityManager, paymentGateway);
        orden.verify(entityManager).refresh(pago, LockModeType.PESSIMISTIC_WRITE);
        orden.verify(paymentGateway).reembolsar(TOKEN, ORDEN, "idem-1:refund");
    }

    @Test
    void reintegrar_siAlReleerYaEstabaReembolsado_noLlamaAMercadoPagoNiAuditaDeNuevo() {
        // KAN-66: el webhook confirmo el reembolso antes de que esta transaccion tomara el bloqueo.
        pago.setCapturadoAt(AHORA.minus(Duration.ofHours(3)));
        when(entityManager.contains(pago)).thenReturn(true);
        org.mockito.Mockito.doAnswer(inv -> {
            pago.setEstado(EstadoPago.Reembolsado);
            return null;
        }).when(entityManager).refresh(pago, LockModeType.PESSIMISTIC_WRITE);

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Reembolsado);
        verifyNoInteractions(paymentGateway, cuentaGuard, auditService);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void reintegrar_pagoDesconectado_bloqueaBuscandoLaFilaPorIdEnVezDeRefrescar() {
        // KAN-66: cancelarinscripcion llama a liberarCupo (clearAutomatically) antes de reintegrar,
        // y refresh sobre una entidad desconectada lanza IllegalArgumentException.
        when(entityManager.contains(pago)).thenReturn(false);
        Pago enBase = copiaEnBase(pago);
        when(entityManager.find(Pago.class, pago.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(enBase);
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.cancelarAutorizacion(TOKEN, ORDEN, "idem-1:cancel"))
                .thenReturn(new ResultadoPago(ORDEN, "canceled", "canceled"));

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Cancelado);
        verify(entityManager, never()).refresh(any(), any(LockModeType.class));
        // Se guarda la instancia gestionada y el objeto del llamador queda con el mismo estado.
        verify(pagoRepository).save(enBase);
        assertThat(enBase.getEstado()).isEqualTo(EstadoPago.Cancelado);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        assertThat(pago.getAutorizadoHasta()).isNull();
    }

    @Test
    void reintegrar_pagoDesconectadoConDatosViejos_decideConLaFilaReleidaYNoLaPisa() {
        // El objeto del llamador se leyo antes de que el pago se capturara: en la base ya figura
        // capturado, asi que hay que reembolsar (cancelar la autorizacion daria error en MP) y
        // no hay que guardar el objeto viejo, que borraria capturado_at.
        when(entityManager.contains(pago)).thenReturn(false);
        Pago enBase = copiaEnBase(pago);
        enBase.setCapturadoAt(AHORA.minus(Duration.ofHours(3)));
        when(entityManager.find(Pago.class, pago.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(enBase);
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.reembolsar(TOKEN, ORDEN, "idem-1:refund"))
                .thenReturn(new ResultadoPago(ORDEN, "refunded", "refunded"));

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Reembolsado);
        verify(paymentGateway, never()).cancelarAutorizacion(any(), any(), any());
        verify(pagoRepository).save(enBase);
        verify(pagoRepository, never()).save(pago);
        assertThat(enBase.getCapturadoAt()).isNotNull();
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Reembolsado);
        verify(auditService).registrar(actorId, AuditAccion.PAGO_REEMBOLSADO, "Pago", pago.getId(), null);
    }

    /** La misma fila leida de nuevo por el EntityManager: otra instancia con los mismos datos. */
    private static Pago copiaEnBase(Pago original) {
        Pago copia = new Pago();
        ReflectionTestUtils.setField(copia, "id", original.getId());
        copia.setEstado(original.getEstado());
        copia.setMpOrderId(original.getMpOrderId());
        copia.setIdempotencyKey(original.getIdempotencyKey());
        copia.setAutorizadoHasta(original.getAutorizadoHasta());
        copia.setCapturadoAt(original.getCapturadoAt());
        return copia;
    }

    @Test
    void reintegrar_pagoDesconectadoQueEnLaBaseYaFiguraReembolsado_noLlamaAMercadoPago() {
        when(entityManager.contains(pago)).thenReturn(false);
        Pago enBase = new Pago();
        enBase.setEstado(EstadoPago.Reembolsado);
        when(entityManager.find(Pago.class, pago.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(enBase);

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Reembolsado);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Reembolsado);
        verifyNoInteractions(paymentGateway, auditService);
    }

    @Test
    void reintegrar_autorizacionYaVencida_quedaCanceladoSinLlamarAMercadoPago() {
        // MP la cancelo sola: pedirlo daria error y frenaria, por ejemplo, la cancelacion de la clase.
        pago.setAutorizadoHasta(AHORA.minus(Duration.ofHours(1)));

        EstadoPago estado = operaciones.reintegrar(pago, instructorId, actorId);

        assertThat(estado).isEqualTo(EstadoPago.Cancelado);
        verifyNoInteractions(paymentGateway, cuentaGuard);
    }

    @Test
    void reintegrar_mercadoPagoFalla_propagaYNoCambiaElEstado() {
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.cancelarAutorizacion(any(), any(), any())).thenThrow(new PagoException("caido"));

        assertThatThrownBy(() -> operaciones.reintegrar(pago, instructorId, actorId)).isInstanceOf(PagoException.class);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void reintegrar_instructorSinCuentaVinculada_propaga() {
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenThrow(new CuentaMercadoPagoNoVinculadaException());

        assertThatThrownBy(() -> operaciones.reintegrar(pago, instructorId, actorId))
                .isInstanceOf(CuentaMercadoPagoNoVinculadaException.class);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
    }

    @Test
    void reintegrar_pagoEnRevision_cancelaLaOrdenEnMercadoPago() {
        // KAN-58: la orden existe en MP aunque todavia no este autorizada; si la inscripcion se
        // cae (clase cancelada, baja del alumno), hay que cancelarla para que no se apruebe sola.
        pago.setEstado(EstadoPago.EnRevision);
        pago.setAutorizadoHasta(null);
        when(cuentaGuard.exigirCuentaVinculada(instructorId)).thenReturn(TOKEN);
        when(paymentGateway.cancelarAutorizacion(TOKEN, ORDEN, "idem-1:cancel"))
                .thenReturn(new ResultadoPago(ORDEN, "canceled", "canceled"));

        assertThat(operaciones.reintegrar(pago, instructorId, actorId)).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarAutorizacion(TOKEN, ORDEN, "idem-1:cancel");
        verify(paymentGateway, never()).reembolsar(any(), any(), any());
    }

    @Test
    void reservadoEnMercadoPago_soloRetenidoYEnRevision() {
        assertThat(java.util.Arrays.stream(EstadoPago.values()).filter(EstadoPago::reservadoEnMercadoPago))
                .containsExactlyInAnyOrder(EstadoPago.Retenido, EstadoPago.EnRevision);
    }

    @Test
    void reintegrar_pagoQueNoEstaRetenido_esUnErrorDeProgramacion() {
        pago.setEstado(EstadoPago.Liberado);

        assertThatThrownBy(() -> operaciones.reintegrar(pago, instructorId, actorId))
                .isInstanceOf(IllegalStateException.class);
    }
}
