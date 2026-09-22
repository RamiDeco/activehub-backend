package com.activehub.usecases.restablecerpassword;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.domain.usuario.VerificacionEmail;
import com.activehub.domain.usuario.VerificacionEmailRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Segundo paso de "¿Olvidaste tu contraseña?": el código habilita escribir una contraseña
 * nueva. Es el único camino que cambia una credencial <b>sin</b> pedir la anterior, así que
 * todo lo que sigue existe para que el código sea lo único que haga falta y sea suficiente.
 *
 * <h2>El propósito se verifica ANTES de consumir el código</h2>
 *
 * {@code VerificacionEmailService.validar} valida el <b>último</b> código del usuario, sea
 * cual sea su propósito, y al validarlo lo marca como usado. Si el propósito se mirara
 * después, un código de alta o de cambio de correo se consumiría acá antes de ser rechazado:
 * el usuario perdería un código que no tenía nada que ver con esto y tendría que pedir otro.
 * Por eso se mira primero y, si no es de recuperación, este pedido no toca nada.
 *
 * <h2>Un solo mensaje de error para todo lo que no sea "la contraseña nueva no sirve"</h2>
 *
 * Correo inexistente, cuenta de Google, correo sin verificar, código de otro propósito: todos
 * responden el mismo texto que un código equivocado. Es la contracara de la respuesta
 * indistinguible de {@code solicitarrecuperacionpassword} — si acá el error delatara que la
 * cuenta existe, no habría servido de nada ocultarlo en el paso anterior.
 *
 * <h2>Las sesiones abiertas siguen abiertas</h2>
 *
 * El JWT es stateless y no hay tabla de sesiones que invalidar, así que un token emitido antes
 * del cambio sigue siendo válido hasta que venza (30 min, {@code app.jwt.expiration-min}).
 * Está asumido: la contraseña vieja ya no sirve para sacar uno nuevo, que es lo que cierra el
 * acceso. Si alguna vez hiciera falta cortar en el acto, el camino es un claim de versión de
 * credencial en el token, no una tabla de sesiones.
 */
@Service
public class RestablecerPasswordService {

    /**
     * El mismo texto para todos los caminos de fallo que podrían delatar si la cuenta existe.
     */
    private static final String ERROR_GENERICO =
            "El código no es correcto o ya venció. Pedí uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final VerificacionEmailRepository verificacionRepository;
    private final VerificacionEmailService verificacionEmailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public RestablecerPasswordService(
            UsuarioRepository usuarioRepository,
            VerificacionEmailRepository verificacionRepository,
            VerificacionEmailService verificacionEmailService,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionRepository = verificacionRepository;
        this.verificacionEmailService = verificacionEmailService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public RestablecerPasswordResponse restablecer(RestablecerPasswordRequest request) {
        Usuario usuario = buscarCuentaRecuperable(request.email())
                .orElseThrow(() -> new ValidacionException(ERROR_GENERICO));

        boolean esDeRecuperacion = verificacionRepository
                .findFirstByUsuarioIdOrderByCreatedAtDesc(usuario.getId())
                .filter(v -> v.getProposito() == PropositoVerificacion.RECUPERACION_PASSWORD)
                .isPresent();
        if (!esDeRecuperacion) {
            throw new ValidacionException(ERROR_GENERICO);
        }

        // Recién acá se consume: valida vencimiento, intentos y el hash del código.
        VerificacionEmail verificacion = verificacionEmailService.validar(usuario.getId(), request.codigo());
        if (verificacion.getProposito() != PropositoVerificacion.RECUPERACION_PASSWORD) {
            // Defensa en profundidad: entre la lectura de arriba y esta línea nadie debería
            // poder emitir otro código, pero si pasara, un código de otro propósito no puede
            // terminar cambiando una contraseña.
            throw new ValidacionException(ERROR_GENERICO);
        }

        if (passwordEncoder.matches(request.contraseniaNueva(), usuario.getPasswordHash())) {
            throw new ValidacionException("La contraseña nueva tiene que ser distinta de la actual.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.contraseniaNueva()));
        usuarioRepository.save(usuario);

        auditService.registrar(
                usuario.getId(), AuditAccion.PASSWORD_RESTABLECIDA, "Usuario", usuario.getId(), null);

        return new RestablecerPasswordResponse(usuario.getEmail());
    }

    /** La misma cuenta que eligió {@code solicitarrecuperacionpassword}: viva, verificada y LOCAL. */
    private Optional<Usuario> buscarCuentaRecuperable(String email) {
        return usuarioRepository.findAllByEmailConRol(email.trim()).stream()
                .filter(Usuario::isEmailVerificado)
                .filter(u -> u.getAuthProveedor() == AuthProveedor.LOCAL)
                .findFirst();
    }
}
