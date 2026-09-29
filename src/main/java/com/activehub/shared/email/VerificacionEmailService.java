package com.activehub.shared.email;

import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.VerificacionEmail;
import com.activehub.domain.usuario.VerificacionEmailRepository;
import com.activehub.shared.error.DemasiadosIntentosException;
import com.activehub.shared.error.ValidacionException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Emite y valida los códigos de 6 dígitos. Cross-cutting, como {@code NotificacionService}:
 * lo usan el alta de alumno, el alta de instructor, el reenvío y el cambio de correo.
 *
 * <h2>Reglas que hace cumplir</h2>
 *
 * <ul>
 *   <li><b>Un solo código vigente por usuario.</b> Antes de emitir se invalidan los
 *       pendientes: si el anterior siguiera sirviendo, "reenviar" dejaría dos códigos válidos
 *       y el contador de intentos de uno no protegería al otro.</li>
 *   <li><b>Espera mínima entre envíos</b> ({@code app.verificacion-email.espera-reenvio-seg}).
 *       Sin esto el endpoint de reenvío es un generador de spam contra un correo ajeno — y el
 *       correo puede ser ajeno, porque hasta que se confirma cualquiera puede pedirlo.</li>
 *   <li><b>Tope de intentos fallidos</b> ({@code max-intentos}). Seis dígitos son 10^6
 *       combinaciones: un script las prueba en minutos si no se invalida el código.</li>
 *   <li><b>El código se guarda hasheado</b> y se compara con el {@code PasswordEncoder}.</li>
 * </ul>
 */
@Service
public class VerificacionEmailService {

    /**
     * {@link SecureRandom} y no {@code Math.random()} ni {@code Random}: un código adivinable
     * a partir de otro código hace inútil todo lo demás.
     */
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final VerificacionEmailRepository verificacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final Clock clock;
    private final int ttlMin;
    private final int esperaReenvioSeg;
    private final int maxIntentos;

    public VerificacionEmailService(
            VerificacionEmailRepository verificacionRepository,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            Clock clock,
            @Value("${app.verificacion-email.ttl-min:15}") int ttlMin,
            @Value("${app.verificacion-email.espera-reenvio-seg:60}") int esperaReenvioSeg,
            @Value("${app.verificacion-email.max-intentos:5}") int maxIntentos
    ) {
        this.verificacionRepository = verificacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.clock = clock;
        this.ttlMin = ttlMin;
        this.esperaReenvioSeg = esperaReenvioSeg;
        this.maxIntentos = maxIntentos;
    }

    /**
     * Emite un código, lo guarda hasheado y manda el mail.
     *
     * <p><b>No falla si el mail no se pudo enviar</b>: el alta ya está guardada y perderla por
     * un SMTP caído sería peor que dejar al usuario con el botón de reenviar. Devuelve si se
     * entregó al servidor para que la respuesta se lo pueda decir.
     *
     * @param destino a qué dirección se manda. En un cambio de correo es el NUEVO.
     */
    public boolean emitir(Usuario usuario, String destino, PropositoVerificacion proposito) {
        Instant ahora = clock.instant();
        verificacionRepository.invalidarPendientes(usuario.getId(), ahora);

        String codigo = generarCodigo();
        VerificacionEmail verificacion = new VerificacionEmail(
                usuario,
                destino.trim().toLowerCase(),
                passwordEncoder.encode(codigo),
                proposito,
                ahora.plus(Duration.ofMinutes(ttlMin)));
        verificacionRepository.save(verificacion);

        return emailSender.enviar(
                destino,
                PlantillaEmail.asunto(proposito, codigo),
                PlantillaEmail.cuerpo(proposito, usuario.getNombre(), codigo, ttlMin));
    }

    /**
     * Vuelve a emitir, respetando la espera mínima entre envíos.
     *
     * @throws DemasiadosIntentosException si todavía no pasó la espera.
     */
    public boolean reemitir(Usuario usuario, String destino, PropositoVerificacion proposito) {
        verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuario.getId())
                .ifPresent(ultima -> {
                    Instant puedeDesde = ultima.getCreatedAt().plusSeconds(esperaReenvioSeg);
                    if (clock.instant().isBefore(puedeDesde)) {
                        throw new DemasiadosIntentosException(
                                "Esperá unos segundos antes de pedir otro código.");
                    }
                });
        return emitir(usuario, destino, proposito);
    }

    /**
     * Valida el código del último pedido del usuario.
     *
     * @return la verificación consumida — quien llama necesita su {@code email} (el destino) y
     *     su {@code proposito} para decidir qué actualizar.
     * @throws ValidacionException si no hay código pendiente, venció o no coincide.
     */
    public VerificacionEmail validar(java.util.UUID usuarioId, String codigo) {
        VerificacionEmail verificacion = verificacionRepository
                .findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)
                .orElseThrow(() -> new ValidacionException(
                        "No tenés ningún código pendiente. Pedí uno nuevo."));

        Instant ahora = clock.instant();
        if (verificacion.getUsadoAt() != null) {
            throw new ValidacionException("Ese código ya se usó. Pedí uno nuevo.");
        }
        if (!ahora.isBefore(verificacion.getExpiraAt())) {
            throw new ValidacionException("El código venció. Pedí uno nuevo.");
        }
        if (verificacion.getIntentos() >= maxIntentos) {
            throw new ValidacionException(
                    "Superaste los intentos permitidos para este código. Pedí uno nuevo.");
        }

        if (!passwordEncoder.matches(codigo.trim(), verificacion.getCodigoHash())) {
            // El intento fallido se persiste ANTES de lanzar. Es la misma trampa que ya
            // documenta `iniciarsesion`: si esto corriera dentro de una transacción, el
            // rollback de la excepción se llevaría el incremento y el tope no contaría nunca.
            verificacion.setIntentos(verificacion.getIntentos() + 1);
            verificacionRepository.saveAndFlush(verificacion);
            int restantes = Math.max(0, maxIntentos - verificacion.getIntentos());
            throw new ValidacionException(restantes > 0
                    ? "El código no es correcto. Te quedan " + restantes
                            + (restantes == 1 ? " intento." : " intentos.")
                    : "El código no es correcto y se agotaron los intentos. Pedí uno nuevo.");
        }

        verificacion.setUsadoAt(ahora);
        verificacionRepository.save(verificacion);
        return verificacion;
    }

    /** Si hay credenciales de SMTP. El frontend lo muestra para no prometer un mail que no sale. */
    public boolean envioHabilitado() {
        return emailSender.habilitado();
    }

    public int ttlMin() {
        return ttlMin;
    }

    /** Seis dígitos, con los ceros a la izquierda incluidos: 000123 es un código válido. */
    private String generarCodigo() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }
}
