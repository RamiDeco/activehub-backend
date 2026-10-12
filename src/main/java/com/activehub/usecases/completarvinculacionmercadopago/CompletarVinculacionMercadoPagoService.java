package com.activehub.usecases.completarvinculacionmercadopago;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructor;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.payments.MercadoPagoOAuthClient;
import com.activehub.shared.payments.MercadoPagoOAuthState;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Callback del OAuth: Mercado Pago redirige el NAVEGADOR del instructor aca con un
 * {@code code} de un solo uso (10 minutos) y el {@code state} que armamos al iniciar.
 *
 * <p>El resultado no es JSON sino la URL del frontend a la que se redirige: quien esta del
 * otro lado es una persona mirando el navegador, no el frontend haciendo un fetch.
 *
 * <ul>
 *   <li>State alterado o vencido: 400 y no se guarda nada. No se redirige: si el state no es
 *       nuestro, el pedido no salio de ActiveHub y no hay a quien mostrarle nada.</li>
 *   <li>El instructor cancelo en MP, o MP rechazo el codigo: se vuelve al panel con
 *       {@code ?mercadopago=error} y no se toca la fila existente.</li>
 *   <li>Ok: una sola fila por instructor. Vincular de nuevo (otra cuenta, o la misma tras
 *       desvincular/vencer) actualiza esa fila.</li>
 * </ul>
 */
@Service
public class CompletarVinculacionMercadoPagoService {

    static final String RUTA_PANEL = "/instructor";

    private final MercadoPagoOAuthState oauthState;
    private final MercadoPagoOAuthClient oauthClient;
    private final CuentaMercadoPagoInstructorRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final String frontendUrl;

    public CompletarVinculacionMercadoPagoService(
            MercadoPagoOAuthState oauthState,
            MercadoPagoOAuthClient oauthClient,
            CuentaMercadoPagoInstructorRepository cuentaRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            Clock clock,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.oauthState = oauthState;
        this.oauthClient = oauthClient;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    /** @return URL del frontend a la que redirigir al navegador */
    @Transactional
    public String completar(CompletarVinculacionMercadoPagoRequest request) {
        UUID instructorId = oauthState.verificar(request.state());

        if (request.code() == null || request.code().isBlank()) {
            return redireccion("cancelada");
        }

        var instructor = usuarioRepository.findById(instructorId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        MercadoPagoOAuthClient.CredencialesOAuth credenciales;
        try {
            credenciales = oauthClient.canjearCodigo(request.code());
        } catch (PagoException e) {
            return redireccion("error");
        }

        CuentaMercadoPagoInstructor cuenta = cuentaRepository.findByInstructorId(instructorId)
                .orElseGet(CuentaMercadoPagoInstructor::new);
        cuenta.setInstructor(instructor);
        cuenta.setMpUserId(String.valueOf(credenciales.userId()));
        cuenta.setAccessToken(credenciales.accessToken());
        cuenta.setRefreshToken(credenciales.refreshToken());
        cuenta.setPublicKey(credenciales.publicKey());
        cuenta.setExpiresAt(credenciales.expiresIn() == null
                ? null
                : clock.instant().plusSeconds(credenciales.expiresIn()));
        cuenta.setEstado(EstadoCuentaMercadoPago.VINCULADA);
        cuenta = cuentaRepository.save(cuenta);

        // Solo el id de la cuenta de MP: los tokens nunca van a la auditoria.
        auditService.registrar(instructorId, AuditAccion.MERCADOPAGO_VINCULADA, "CuentaMercadoPagoInstructor",
                cuenta.getId(), "mp_user_id=" + cuenta.getMpUserId());

        return redireccion("vinculada");
    }

    private String redireccion(String resultado) {
        return frontendUrl + RUTA_PANEL + "?mercadopago=" + resultado;
    }
}
