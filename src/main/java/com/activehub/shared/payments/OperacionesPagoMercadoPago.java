package com.activehub.shared.payments;

import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * KAN-65: lo que se le pide a Mercado Pago sobre un {@link Pago} ya autorizado. Estilo
 * {@link CuentaMercadoPagoGuard}: la regla vive en un solo lugar y la usan los casos de uso que
 * capturan o reintegran, sin inyectarse Services de otros slices.
 *
 * <p>Ciclo (decision del usuario, 2026-10-05): se autoriza al inscribirse, se <b>captura al
 * finalizar la clase</b> (siempre dentro de los 5 dias de {@code VentanaPagos.VIGENCIA_AUTORIZACION},
 * porque la inscripcion abre 4 dias antes) y {@code Liberado} es un estado interno, sin llamada a
 * MP. Capturar al liberar —24 h despues de la clase— dejaba vencer la autorizacion de quien se
 * anotaba con mas de ~3 dias de anticipacion, y el instructor no cobraba nunca.
 *
 * <p>Un pago sin {@code mpOrderId} es un pago simulado de antes de MP-04: no tiene nada del lado
 * de MP y solo cambia de estado.
 */
@Component
public class OperacionesPagoMercadoPago {

    private static final Logger log = LoggerFactory.getLogger(OperacionesPagoMercadoPago.class);

    private final PaymentGateway paymentGateway;
    private final CuentaMercadoPagoGuard cuentaGuard;
    private final PagoRepository pagoRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final EntityManager entityManager;

    public OperacionesPagoMercadoPago(PaymentGateway paymentGateway, CuentaMercadoPagoGuard cuentaGuard,
                                      PagoRepository pagoRepository, AuditService auditService, Clock clock,
                                      EntityManager entityManager) {
        this.paymentGateway = paymentGateway;
        this.cuentaGuard = cuentaGuard;
        this.pagoRepository = pagoRepository;
        this.auditService = auditService;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    /**
     * Captura un pago {@code Retenido} todavia no capturado. Sigue {@code Retenido}: pasa a
     * {@code Liberado} recien despues del periodo de denuncias. Si MP falla, lanza y el pago queda
     * sin capturar, para que la proxima corrida lo reintente. Si la autorizacion ya vencio, no
     * hay nada que capturar y el pago pasa a {@code Cancelado}.
     */
    public void capturar(Pago pago, UUID instructorId) {
        if (pago.getEstado() != EstadoPago.Retenido || pago.getCapturadoAt() != null) {
            return;
        }
        Instant vence = pago.getAutorizadoHasta();
        if (pago.getMpOrderId() != null && vence != null && !vence.isAfter(clock.instant())) {
            // Ya no hay nada que capturar: MP cancelo la autorizacion y la plata volvio al alumno.
            // Sin esto el scheduler la reintentaria para siempre y el pago figuraria cobrado.
            log.error("La autorizacion de la orden {} vencio el {} sin capturarse: el instructor no cobra este pago",
                    pago.getMpOrderId(), vence);
            pago.setEstado(EstadoPago.Cancelado);
            pago.setStatusDetail("expired");
            pagoRepository.save(pago);
            return;
        }
        if (pago.getMpOrderId() != null) {
            String token = cuentaGuard.exigirCuentaVinculada(instructorId);
            var resultado = paymentGateway.capturar(token, pago.getMpOrderId(), clave(pago, "capture"));
            pago.setStatusDetail(resultado.statusDetail());
        }
        pago.setCapturadoAt(clock.instant());
        pago.setAutorizadoHasta(null);
        pagoRepository.save(pago);
        auditService.registrar(null, AuditAccion.PAGO_CAPTURADO, "Pago", pago.getId(), null);
    }

    /**
     * Devuelve al alumno un pago {@code Retenido} o {@code EnRevision} (KAN-58; nunca esta
     * capturado, asi que se cancela la orden): sin capturar se cancela la autorizacion
     * ({@code Cancelado}); capturado se reembolsa ({@code Reembolsado}). Si MP falla, lanza y la
     * operacion que reintegra se revierte entera: es preferible a marcar devuelto algo que sigue
     * cobrado o reservado en la tarjeta del alumno.
     *
     * <p>KAN-66: antes de llamar a MP relee el pago con bloqueo de fila, que se mantiene hasta que
     * confirma la transaccion del llamador (todos los llamadores son {@code @Transactional}). El
     * webhook que MP manda enseguida pide la misma fila y espera, asi no toma este reembolso por
     * uno hecho desde el panel. Si al releer el pago ya estaba devuelto (el webhook llego primero),
     * no se le pide nada a MP ni se audita de nuevo.
     *
     * @param actorId quien ordena el reintegro (null = el sistema), para la auditoria
     * @return el estado en que quedo el pago
     */
    public EstadoPago reintegrar(Pago pago, UUID instructorId, UUID actorId) {
        EstadoPago enBase = bloquearYReleer(pago).getEstado();
        if (enBase == EstadoPago.Cancelado || enBase == EstadoPago.Reembolsado) {
            log.info("El pago {} ya estaba {} al reintegrarlo: no se le pide nada a Mercado Pago", pago.getId(), enBase);
            pago.setEstado(enBase);
            return enBase;
        }
        if (!pago.getEstado().reservadoEnMercadoPago()) {
            throw new IllegalStateException("Solo se reintegra un pago Retenido o EnRevision, no " + pago.getEstado());
        }
        EstadoPago destino = pago.getCapturadoAt() == null ? EstadoPago.Cancelado : EstadoPago.Reembolsado;

        if (pago.getMpOrderId() != null) {
            if (destino == EstadoPago.Cancelado) {
                cancelarAutorizacion(pago, instructorId);
            } else {
                String token = cuentaGuard.exigirCuentaVinculada(instructorId);
                var resultado = paymentGateway.reembolsar(token, pago.getMpOrderId(), clave(pago, "refund"));
                pago.setStatusDetail(resultado.statusDetail());
            }
        }

        pago.setEstado(destino);
        pago.setAutorizadoHasta(null);
        pagoRepository.save(pago);
        if (destino == EstadoPago.Reembolsado) {
            auditService.registrar(actorId, AuditAccion.PAGO_REEMBOLSADO, "Pago", pago.getId(), null);
        }
        return destino;
    }

    /**
     * KAN-66: toma el bloqueo de la fila y devuelve el pago tal como esta en la base. Si el objeto
     * del llamador esta gestionado, se lo refresca. Si esta desconectado (pasa despues de un
     * {@code @Modifying(clearAutomatically = true)}: {@code cancelarinscripcion} llama a
     * {@code liberarCupo} justo antes), {@code refresh} lanzaria, asi que se busca la fila por id;
     * los cambios se siguen haciendo sobre el objeto del llamador y {@code save} los fusiona.
     */
    private Pago bloquearYReleer(Pago pago) {
        if (entityManager.contains(pago)) {
            entityManager.refresh(pago, LockModeType.PESSIMISTIC_WRITE);
            return pago;
        }
        Pago enBase = entityManager.find(Pago.class, pago.getId(), LockModeType.PESSIMISTIC_WRITE);
        return enBase != null ? enBase : pago;
    }

    private void cancelarAutorizacion(Pago pago, UUID instructorId) {
        Instant vence = pago.getAutorizadoHasta();
        if (vence != null && !vence.isAfter(clock.instant())) {
            // MP ya la cancelo sola al vencer y la plata volvio al alumno: no hay nada que pedir,
            // y pedirlo daria error y frenaria la cancelacion de la clase.
            log.info("Autorizacion de la orden {} vencida el {}: se da por cancelada", pago.getMpOrderId(), vence);
            return;
        }
        String token = cuentaGuard.exigirCuentaVinculada(instructorId);
        var resultado = paymentGateway.cancelarAutorizacion(token, pago.getMpOrderId(), clave(pago, "cancel"));
        pago.setStatusDetail(resultado.statusDetail());
    }

    /**
     * Cada operacion sobre la orden lleva su propia clave (MP rechaza reusar una con otro body),
     * derivada de la de la autorizacion: un reintento de la misma operacion da la misma clave.
     * El sufijo ":cancel" coincide a proposito con la compensacion de {@code inscribirse}.
     */
    private static String clave(Pago pago, String operacion) {
        String base = pago.getIdempotencyKey() != null ? pago.getIdempotencyKey() : "pago:" + pago.getId();
        return base + ":" + operacion;
    }
}
