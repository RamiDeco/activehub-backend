package com.activehub.usecases.solicitarcambioemail;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedir el cambio de correo desde el perfil: manda el código al correo <b>nuevo</b>.
 *
 * <h2>La cuenta NO cambia acá</h2>
 *
 * Este usecase no toca {@code usuario.email}. Sólo emite un código contra la dirección nueva;
 * el cambio lo aplica {@code verificaremail} cuando el usuario ingresa el código. Es a
 * propósito, y es lo que evita el peor caso: tipear mal el correo nuevo y quedarse sin
 * credencial verificada. Hasta que se confirme, la cuenta sigue con el correo anterior — que
 * además sigue reservado, así que nadie lo puede tomar mientras el usuario duda.
 *
 * <p>Correlato: es la única forma de <b>liberar</b> un correo verificado. Al confirmarse el
 * cambio, el anterior deja de estar tomado y otra persona puede registrarse con él.
 */
@Service
public class SolicitarCambioEmailService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificacionEmailService verificacionEmailService;

    public SolicitarCambioEmailService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            VerificacionEmailService verificacionEmailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificacionEmailService = verificacionEmailService;
    }

    @Transactional
    public SolicitarCambioEmailResponse solicitar(UUID usuarioId, SolicitarCambioEmailRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        // Una cuenta de Google no tiene contraseña que el usuario conozca: su `passwordHash`
        // es aleatorio. Pedirle una sería pedirle algo que no existe, así que el cambio de
        // correo no aplica — su correo es el de Google y se cambia allá.
        if (usuario.getAuthProveedor() == AuthProveedor.GOOGLE) {
            throw new ValidacionException(
                    "Tu cuenta usa Google para iniciar sesión, así que el correo lo maneja Google. "
                            + "Cambialo en tu cuenta de Google.");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        String nuevo = request.email().trim().toLowerCase();
        if (nuevo.equalsIgnoreCase(usuario.getEmail()) && usuario.isEmailVerificado()) {
            throw new ValidacionException("Ese ya es tu correo actual.", java.util.Map.of(
                    "email", "Ese ya es tu correo actual."));
        }
        // Lo que bloquea no es que alguien lo haya tipeado, es que alguien lo haya CONFIRMADO.
        if (usuarioRepository.existsVerificadoConEmail(nuevo, usuarioId)) {
            throw new EmailEnUsoException();
        }

        boolean enviado = verificacionEmailService.reemitir(usuario, nuevo, PropositoVerificacion.CAMBIO_EMAIL);
        return new SolicitarCambioEmailResponse(nuevo, enviado, verificacionEmailService.ttlMin());
    }
}
