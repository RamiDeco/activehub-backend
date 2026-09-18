package com.activehub.usecases.reenviarcodigoemail;

import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.domain.usuario.VerificacionEmailRepository;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reenvía el código. Existe porque el mail se pierde: cae en spam, se cierra la pestaña, el
 * SMTP falla justo en el alta. Sin esto, la única salida era registrarse de nuevo.
 *
 * <p><b>El destino no viene en el request, se deduce.</b> Si hubiera un cambio de correo
 * pendiente se manda al correo nuevo; si no, al de la cuenta. Que el cliente eligiera la
 * dirección convertiría este endpoint en un "mandale un código a quien yo diga".
 */
@Service
public class ReenviarCodigoEmailService {

    private final UsuarioRepository usuarioRepository;
    private final VerificacionEmailRepository verificacionRepository;
    private final VerificacionEmailService verificacionEmailService;

    public ReenviarCodigoEmailService(
            UsuarioRepository usuarioRepository,
            VerificacionEmailRepository verificacionRepository,
            VerificacionEmailService verificacionEmailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionRepository = verificacionRepository;
        this.verificacionEmailService = verificacionEmailService;
    }

    @Transactional
    public ReenviarCodigoEmailResponse reenviar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        var ultima = verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId);
        boolean cambioPendiente = ultima
                .filter(v -> v.getUsadoAt() == null)
                .filter(v -> v.getProposito() == PropositoVerificacion.CAMBIO_EMAIL)
                .isPresent();

        if (!cambioPendiente && usuario.isEmailVerificado()) {
            throw new ValidacionException("Tu correo ya está confirmado.");
        }

        String destino = cambioPendiente ? ultima.get().getEmail() : usuario.getEmail();
        PropositoVerificacion proposito =
                cambioPendiente ? PropositoVerificacion.CAMBIO_EMAIL : PropositoVerificacion.REGISTRO;

        boolean enviado = verificacionEmailService.reemitir(usuario, destino, proposito);
        return new ReenviarCodigoEmailResponse(destino, enviado, verificacionEmailService.ttlMin());
    }
}
