package com.activehub.usecases.cambiarmicontrasenia;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E3A-HU12 criterio 6: cambio de contraseña del usuario logueado.
 *
 * <p>Pide la contraseña actual a proposito: sin eso, cualquiera con la sesion abierta en una
 * maquina prestada podria quedarse con la cuenta.
 */
@Service
public class CambiarMiContraseniaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public CambiarMiContraseniaService(
            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public void cambiar(UUID usuarioId, CambiarMiContraseniaRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (!passwordEncoder.matches(request.contraseniaActual(), usuario.getPasswordHash())) {
            throw new ValidacionException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(request.contraseniaNueva(), usuario.getPasswordHash())) {
            throw new ValidacionException("La contraseña nueva tiene que ser distinta de la actual.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.contraseniaNueva()));
        usuarioRepository.save(usuario);

        auditService.registrar(usuarioId, AuditAccion.PASSWORD_CAMBIADA, "Usuario", usuarioId, null);
    }
}
