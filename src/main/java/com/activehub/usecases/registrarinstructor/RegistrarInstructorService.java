package com.activehub.usecases.registrarinstructor;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarInstructorService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public RegistrarInstructorService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public RegistrarInstructorResponse registrar(RegistrarInstructorRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse(request.email())) {
            throw new EmailEnUsoException();
        }

        var rolInstructor = rolRepository.findByNombre(RolNombre.INSTRUCTOR)
                .orElseThrow(() -> new IllegalStateException("Rol INSTRUCTOR no encontrado, revisar la migracion V2."));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setRol(rolInstructor);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.saveAndFlush(usuario);

        PerfilInstructor perfilInstructor = new PerfilInstructor(
                usuario, request.especialidad(), request.aniosExperiencia(), request.descripcion());
        perfilInstructorRepository.save(perfilInstructor);

        auditService.registrar(usuario.getId(), AuditAccion.REGISTRO_INSTRUCTOR, "Usuario", usuario.getId(), null);

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), RolNombre.INSTRUCTOR);

        return new RegistrarInstructorResponse(token, new RegistrarInstructorResponse.Usuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                rolInstructor.getNombre().name(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt()
        ));
    }
}
