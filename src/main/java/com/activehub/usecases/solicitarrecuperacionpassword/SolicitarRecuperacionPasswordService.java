package com.activehub.usecases.solicitarrecuperacionpassword;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.DemasiadosIntentosException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "¿Olvidaste tu contraseña?": emite el código de 6 dígitos contra el correo de la cuenta.
 *
 * <h2>La respuesta NO dice si la cuenta existe</h2>
 *
 * Este endpoint es público y recibe un correo cualquiera. Si contestara distinto según la
 * cuenta existiera o no, sería un <b>oráculo de enumeración</b>: cualquiera podría averiguar
 * quién está registrado en la plataforma probando direcciones, que es el primer paso de un
 * ataque dirigido. Por eso los tres desenlaces —no hay cuenta, la cuenta es de Google, el
 * correo no está verificado— devuelven exactamente lo mismo que el caso feliz, y lo único que
 * cambia es que no se emite nada.
 *
 * <p>Por la misma razón <b>se traga la espera mínima entre reenvíos</b>: dejar escapar el
 * {@link DemasiadosIntentosException} respondería 429 sólo para los correos registrados, que
 * es justamente la diferencia observable que esto viene a evitar. El pedido no emite y listo.
 *
 * <h2>Quién puede recuperar</h2>
 *
 * <ul>
 *   <li><b>Sólo cuentas con el correo verificado.</b> Es la regla de V26 llevada hasta el
 *       final: si un correo sin confirmar sirviera para recuperar, alguien podría registrarse
 *       con la dirección de otro y usar este flujo para hacerse de un código a esa casilla.
 *       Quien nunca confirmó no tiene ninguna cuenta que perder — puede registrarse de nuevo.</li>
 *   <li><b>Sólo cuentas {@code LOCAL}.</b> Una cuenta de Google no tiene contraseña utilizable
 *       (nace con un hash aleatorio), así que "recuperarla" no le devolvería el acceso:
 *       entra por el botón de Google, que es donde está su credencial.</li>
 * </ul>
 */
@Service
public class SolicitarRecuperacionPasswordService {

    private final UsuarioRepository usuarioRepository;
    private final VerificacionEmailService verificacionEmailService;

    public SolicitarRecuperacionPasswordService(
            UsuarioRepository usuarioRepository, VerificacionEmailService verificacionEmailService) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionEmailService = verificacionEmailService;
    }

    @Transactional
    public SolicitarRecuperacionPasswordResponse solicitar(SolicitarRecuperacionPasswordRequest request) {
        buscarCuentaRecuperable(request.email()).ifPresent(usuario -> {
            try {
                verificacionEmailService.reemitir(
                        usuario, usuario.getEmail(), PropositoVerificacion.RECUPERACION_PASSWORD);
            } catch (DemasiadosIntentosException e) {
                // A propósito: ver la nota de enumeración. El usuario todavía tiene el código
                // anterior, que sigue siendo el único válido.
            }
        });

        return new SolicitarRecuperacionPasswordResponse(
                verificacionEmailService.envioHabilitado(), verificacionEmailService.ttlMin());
    }

    /**
     * La única cuenta que puede recibir el código: viva, con el correo verificado y con
     * contraseña propia. {@code findAllByEmailConRol} puede devolver varias (desde V26 un
     * correo sin confirmar no es único), y la verificada viene primero.
     */
    private Optional<Usuario> buscarCuentaRecuperable(String email) {
        return usuarioRepository.findAllByEmailConRol(email.trim()).stream()
                .filter(Usuario::isEmailVerificado)
                .filter(u -> u.getAuthProveedor() == AuthProveedor.LOCAL)
                .findFirst();
    }
}
