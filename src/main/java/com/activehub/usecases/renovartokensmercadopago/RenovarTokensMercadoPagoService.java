package com.activehub.usecases.renovartokensmercadopago;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructor;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.MercadoPagoOAuthClient;
import com.activehub.shared.payments.MercadoPagoOAuthClient.CredencialesOAuth;
import com.activehub.shared.payments.MercadoPagoOAuthClient.RenovacionRechazadaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * KAN-55: que la vinculacion con Mercado Pago no se muera sola. El access token de MP vence
 * (hoy, a los 180 dias) y, vencido, {@code CuentaMercadoPagoGuard} deja de considerar
 * vinculada la cuenta: el instructor pasaba a cobrar solo en efectivo sin enterarse.
 *
 * <p>Este job renueva con el {@code refresh_token} los tokens de cuentas {@code VINCULADA} que
 * vencen dentro de {@link #VENTANA_RENOVACION} (incluidos los ya vencidos, que MP todavia
 * puede renovar). Con margen de dias y no de horas: si MP esta caido una corrida, quedan
 * muchas mas antes del vencimiento.
 *
 * <ul>
 *   <li>MP renueva: se guardan el access y el refresh token NUEVOS (el viejo deja de servir).</li>
 *   <li>MP rechaza (4xx: el instructor revoco el permiso, el refresh token vencio): la cuenta
 *       queda {@code TOKEN_VENCIDO}, sin tokens, y se le avisa al instructor. No se reintenta:
 *       la unica salida es volver a vincular.</li>
 *   <li>MP no responde o falla (5xx): no se toca nada, se reintenta en la proxima corrida.</li>
 * </ul>
 *
 * <p>Sin {@code @Transactional} a proposito, como la captura de {@code finalizarclasesvencidas}:
 * cada cuenta commitea sola y una que falla (incluso un token que no se puede descifrar) no
 * corta la corrida para las demas.
 */
@Service
public class RenovarTokensMercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(RenovarTokensMercadoPagoService.class);

    /** Se renuevan los tokens que vencen antes de ahora + esta ventana. */
    static final Duration VENTANA_RENOVACION = Duration.ofDays(7);

    static final String MENSAJE_TOKEN_VENCIDO = "Mercado Pago no aceptó renovar la vinculación de tu cuenta. "
            + "Hasta que la vuelvas a vincular desde tu Panel, tus clases solo aceptan pago en efectivo.";

    private final CuentaMercadoPagoInstructorRepository cuentaRepository;
    private final MercadoPagoOAuthClient oauthClient;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final Clock clock;

    public RenovarTokensMercadoPagoService(
            CuentaMercadoPagoInstructorRepository cuentaRepository,
            MercadoPagoOAuthClient oauthClient,
            NotificacionService notificacionService,
            AuditService auditService,
            Clock clock
    ) {
        this.cuentaRepository = cuentaRepository;
        this.oauthClient = oauthClient;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /** @return cuantas cuentas se renovaron en esta corrida */
    public int renovar() {
        Instant limite = clock.instant().plus(VENTANA_RENOVACION);
        List<UUID> ids = cuentaRepository.findIdsQueVencenAntesDe(EstadoCuentaMercadoPago.VINCULADA, limite);

        int renovadas = 0;
        for (UUID id : ids) {
            try {
                if (renovarCuenta(id)) {
                    renovadas++;
                }
            } catch (RuntimeException e) {
                // Token ilegible (cambio MP_TOKEN_ENCRYPTION_KEY), falla al guardar o al notificar.
                log.error("No se pudo renovar la cuenta de Mercado Pago {}: se reintenta en la proxima corrida", id, e);
            }
        }
        return renovadas;
    }

    private boolean renovarCuenta(UUID id) {
        CuentaMercadoPagoInstructor cuenta = cuentaRepository.findById(id).orElse(null);
        if (cuenta == null || cuenta.getEstado() != EstadoCuentaMercadoPago.VINCULADA) {
            return false;
        }
        // Sin refresh token no hay forma de renovar: es lo mismo que un rechazo de MP.
        if (cuenta.getRefreshToken() == null || cuenta.getRefreshToken().isBlank()) {
            marcarVencida(cuenta, "sin refresh token");
            return false;
        }

        CredencialesOAuth credenciales;
        try {
            credenciales = oauthClient.renovar(cuenta.getRefreshToken());
        } catch (RenovacionRechazadaException e) {
            marcarVencida(cuenta, "renovacion rechazada por Mercado Pago");
            return false;
        } catch (PagoException e) {
            log.warn("Mercado Pago no renovo la cuenta {} ({}): se reintenta en la proxima corrida",
                    id, e.getMessage());
            return false;
        }

        cuenta.setAccessToken(credenciales.accessToken());
        cuenta.setRefreshToken(credenciales.refreshToken());
        if (credenciales.publicKey() != null) {
            cuenta.setPublicKey(credenciales.publicKey());
        }
        cuenta.setExpiresAt(credenciales.expiresIn() == null
                ? null
                : clock.instant().plusSeconds(credenciales.expiresIn()));
        cuentaRepository.save(cuenta);

        // Los tokens nunca van a la auditoria: solo el nuevo vencimiento.
        auditService.registrar(null, AuditAccion.MERCADOPAGO_TOKEN_RENOVADO, "CuentaMercadoPagoInstructor",
                cuenta.getId(), "vence " + cuenta.getExpiresAt());
        return true;
    }

    private void marcarVencida(CuentaMercadoPagoInstructor cuenta, String motivo) {
        UUID instructorId = cuenta.getInstructor().getId();
        log.warn("La cuenta de Mercado Pago {} del instructor {} queda TOKEN_VENCIDO: {}",
                cuenta.getId(), instructorId, motivo);

        cuenta.setEstado(EstadoCuentaMercadoPago.TOKEN_VENCIDO);
        // Ya no sirven para nada: no hay por que seguir guardando secretos.
        cuenta.setAccessToken(null);
        cuenta.setRefreshToken(null);
        cuentaRepository.save(cuenta);

        auditService.registrar(null, AuditAccion.MERCADOPAGO_TOKEN_VENCIDO, "CuentaMercadoPagoInstructor",
                cuenta.getId(), motivo);
        // Sin destino: la tarjeta de Mercado Pago vive en el Panel del instructor, que no tiene
        // un DestinoNotificacion propio.
        notificacionService.notificar(instructorId, TipoNotificacion.MERCADOPAGO_TOKEN_VENCIDO,
                MENSAJE_TOKEN_VENCIDO, cuenta.getId(), Destino.ninguno());
    }
}
