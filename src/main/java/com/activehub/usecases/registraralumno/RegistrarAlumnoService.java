package com.activehub.usecases.registraralumno;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
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
public class RegistrarAlumnoService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public RegistrarAlumnoService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilAlumnoRepository perfilAlumnoRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public RegistrarAlumnoResponse registrar(RegistrarAlumnoRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse(request.email())) {
            throw new EmailEnUsoException();
        }

        var rolAlumno = rolRepository.findByNombre(RolNombre.ALUMNO)
                .orElseThrow(() -> new IllegalStateException("Rol ALUMNO no encontrado, revisar la migracion V2."));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setRol(rolAlumno);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.saveAndFlush(usuario);

        PerfilAlumno perfilAlumno = new PerfilAlumno(usuario, request.intereses());
        perfilAlumno.setCondicionSalud(request.condicionSalud());
        perfilAlumnoRepository.save(perfilAlumno);

        auditService.registrar(usuario.getId(), AuditAccion.REGISTRO_ALUMNO, "Usuario", usuario.getId(), null);

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), RolNombre.ALUMNO);

        return new RegistrarAlumnoResponse(token, new RegistrarAlumnoResponse.Usuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                rolAlumno.getNombre().name(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt()
        ));
    }
}
