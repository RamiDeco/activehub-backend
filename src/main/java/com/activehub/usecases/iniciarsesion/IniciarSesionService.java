package com.activehub.usecases.iniciarsesion;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.UsuarioSuspendidoException;
import com.activehub.shared.security.JwtService;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IniciarSesionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public IniciarSesionService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public IniciarSesionResponse login(IniciarSesionRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email())
                .orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            UUID actorId = usuario != null ? usuario.getId() : null;
            auditService.registrar(actorId, AuditAccion.LOGIN_FALLIDO, "Usuario", actorId, request.email());
            throw new CredencialesInvalidasException();
        }

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
            auditService.registrar(usuario.getId(), AuditAccion.LOGIN_FALLIDO, "Usuario", usuario.getId(), "usuario suspendido");
            throw new UsuarioSuspendidoException();
        }

        auditService.registrar(usuario.getId(), AuditAccion.LOGIN_OK, "Usuario", usuario.getId(), null);

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), usuario.getRol().getNombre());

        return new IniciarSesionResponse(token, new IniciarSesionResponse.Usuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                usuario.getRol().getNombre().name(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt()
        ));
    }
}
