package com.activehub.usecases.verificaremail;

import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.domain.usuario.VerificacionEmail;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.security.JwtService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingresar el código: es el momento en que el correo queda <b>reservado</b>.
 *
 * <h2>La carrera por el mismo correo, y quién gana</h2>
 *
 * Mientras un correo está sin confirmar, varias cuentas pueden tenerlo (es la regla pedida y
 * lo que permite el índice único parcial de V26). Eso significa que dos personas pueden estar
 * sosteniendo un código para la misma dirección al mismo tiempo, y **gana la primera que lo
 * ingresa**: a partir de ahí el índice rechaza a cualquier otra.
 *
 * <p>Por eso el chequeo de "ya está tomado" se hace acá y no sólo en el alta: entre el alta y
 * la confirmación puede haber pasado cualquier cosa. La segunda persona recibe un 409 claro en
 * vez de un error de base de datos.
 */
@Service
public class VerificarEmailService {

    private final UsuarioRepository usuarioRepository;
    private final VerificacionEmailService verificacionEmailService;
    private final AuditService auditService;
    private final JwtService jwtService;
    private final Clock clock;

    public VerificarEmailService(
            UsuarioRepository usuarioRepository,
            VerificacionEmailService verificacionEmailService,
            AuditService auditService,
            JwtService jwtService,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionEmailService = verificacionEmailService;
        this.auditService = auditService;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public VerificarEmailResponse verificar(UUID usuarioId, VerificarEmailRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        VerificacionEmail verificacion = verificacionEmailService.validar(usuarioId, request.codigo());
        String email = verificacion.getEmail();
        boolean cambio = verificacion.getProposito() == PropositoVerificacion.CAMBIO_EMAIL;

        // El correo pudo haber sido verificado por otra cuenta mientras este código estaba en
        // el aire. Se pregunta de nuevo, justo antes de escribir.
        if (usuarioRepository.existsVerificadoConEmail(email, usuarioId)) {
            throw new EmailEnUsoException();
        }

        usuario.setEmail(email);
        usuario.setEmailVerificado(true);
        usuario.setEmailVerificadoAt(clock.instant());
        usuarioRepository.save(usuario);

        auditService.registrar(
                usuarioId,
                cambio ? AuditAccion.EMAIL_CAMBIADO : AuditAccion.EMAIL_VERIFICADO,
                "Usuario",
                usuarioId,
                email);

        // Token nuevo: el que tiene el cliente dice `emailVerificado: false` y lo seguiria
        // bloqueando el filtro hasta que venciera.
        String token = jwtService.emitir(usuarioId, email, usuario.getRol().getNombre(), true);
        return new VerificarEmailResponse(token, email, true, cambio);
    }
}
