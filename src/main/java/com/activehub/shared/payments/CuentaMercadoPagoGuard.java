package com.activehub.shared.payments;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructor;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.shared.error.CuentaMercadoPagoNoVinculadaException;
import com.activehub.shared.error.PagoException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * KAN-57: nunca se intenta cobrar con Mercado Pago una clase de un instructor que no tiene a
 * donde recibir la plata. Sin cuenta VINCULADA y vigente, su clase solo acepta Efectivo.
 *
 * <p>Estilo {@code InstructorVerificadoGuard}: la regla vive en un solo lugar y la usan los
 * casos de uso que la necesitan (hoy {@code inscribirse} para cobrar y {@code obteneractividad}
 * para avisarle al frontend si ofrecer la opcion).
 */
@Component
public class CuentaMercadoPagoGuard {

    private static final Logger log = LoggerFactory.getLogger(CuentaMercadoPagoGuard.class);

    private final CuentaMercadoPagoInstructorRepository cuentaRepository;
    private final Clock clock;

    public CuentaMercadoPagoGuard(CuentaMercadoPagoInstructorRepository cuentaRepository, Clock clock) {
        this.cuentaRepository = cuentaRepository;
        this.clock = clock;
    }

    /**
     * La public key del instructor si hoy puede cobrar con Mercado Pago; vacio si no. Lee con una
     * proyeccion que NO carga los tokens: la respuesta del detalle es publica y no tiene por que
     * descifrar nada (ni fallar si falta la clave de cifrado).
     */
    public Optional<String> publicKeySiAceptaMercadoPago(UUID instructorId) {
        return cuentaRepository.findResumenByInstructorId(instructorId)
                .filter(c -> c.vigente(clock.instant()))
                .map(CuentaMercadoPagoInstructorRepository.ResumenCuenta::getPublicKey)
                .filter(pk -> !pk.isBlank());
    }

    /**
     * @return el access token del instructor, para crear la orden en SU cuenta
     * @throws CuentaMercadoPagoNoVinculadaException (409) si no tiene cuenta vigente
     */
    public String exigirCuentaVinculada(UUID instructorId) {
        Optional<CuentaMercadoPagoInstructor> encontrada;
        try {
            encontrada = cuentaRepository.findByInstructorId(instructorId);
        } catch (RuntimeException e) {
            // El token no se pudo descifrar (falta MP_TOKEN_ENCRYPTION_KEY o cambio la clave).
            // Hibernate puede envolver la excepcion del converter, por eso no se filtra por tipo.
            log.error("No se pudo leer la cuenta de Mercado Pago del instructor {}", instructorId, e);
            throw new PagoException("El pago con Mercado Pago no está disponible en este momento.");
        }
        var ahora = clock.instant();
        return encontrada
                .filter(c -> c.getEstado() == EstadoCuentaMercadoPago.VINCULADA)
                .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(ahora))
                .map(CuentaMercadoPagoInstructor::getAccessToken)
                .orElseThrow(CuentaMercadoPagoNoVinculadaException::new);
    }
}
