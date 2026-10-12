package com.activehub.usecases.registraradmin;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.DniEnUsoException;
import com.activehub.shared.error.EmailEnUsoException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarAdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public RegistrarAdminService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public RegistrarAdminResponse registrar(RegistrarAdminRequest request, UUID actorId) {
        if (usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse(request.email())) {
            throw new EmailEnUsoException();
        }

        String dni = request.dni() != null && !request.dni().isBlank() ? request.dni().trim() : null;
        if (dni != null && usuarioRepository.existsByDniAndDeletedFalse(dni)) {
            throw new DniEnUsoException();
        }

        var rolAdmin = rolRepository.findByNombre(RolNombre.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado, revisar la migracion V2."));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setDni(dni);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setRol(rolAdmin);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.saveAndFlush(usuario);

        auditService.registrar(actorId, AuditAccion.ADMIN_CREADO, "Usuario", usuario.getId(), null);

        return new RegistrarAdminResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                rolAdmin.getNombre(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt()
        );
    }
}
