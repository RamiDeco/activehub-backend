package com.activehub.usecases.registraralumno;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.DniEnUsoException;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.security.JwtService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarAlumnoService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final VerificacionEmailService verificacionEmailService;

    public RegistrarAlumnoService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilAlumnoRepository perfilAlumnoRepository,
            TipoActividadRepository tipoActividadRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            VerificacionEmailService verificacionEmailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.tipoActividadRepository = tipoActividadRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.verificacionEmailService = verificacionEmailService;
    }

    @Transactional
    public RegistrarAlumnoResponse registrar(RegistrarAlumnoRequest request) {
        // Lo que bloquea un correo es que alguien lo haya CONFIRMADO con el código, no que lo
        // haya tipeado (V26). Mientras esté sin verificar, esta misma dirección puede estar en
        // otras altas pendientes y todas son válidas: gana la primera que ingrese su código.
        if (usuarioRepository.existsVerificadoConEmail(request.email(), null)) {
            throw new EmailEnUsoException();
        }

        // Precondición de E1A-HU03: "no debe existir una cuenta activa con el mismo correo
        // electrónico (o DNI)".
        String dni = request.dni() != null && !request.dni().isBlank() ? request.dni().trim() : null;
        if (dni != null && usuarioRepository.existsByDniAndDeletedFalse(dni)) {
            throw new DniEnUsoException();
        }

        var rolAlumno = rolRepository.findByNombre(RolNombre.ALUMNO)
                .orElseThrow(() -> new IllegalStateException("Rol ALUMNO no encontrado, revisar la migracion V2."));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setDni(dni);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setRol(rolAlumno);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.saveAndFlush(usuario);

        // Los intereses son ids de TipoActividad (V19). Se ignora en silencio cualquier id que
        // ya no exista: no vale la pena abortar un alta por un interés que se dio de baja.
        List<TipoActividad> intereses = request.intereses() == null
                ? List.of()
                : request.intereses().stream()
                        .distinct()
                        .map(id -> tipoActividadRepository.findById(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .toList();

        PerfilAlumno perfilAlumno = new PerfilAlumno(usuario, intereses);
        perfilAlumno.setCondicionSalud(request.condicionSalud());
        perfilAlumnoRepository.save(perfilAlumno);

        auditService.registrar(usuario.getId(), AuditAccion.REGISTRO_ALUMNO, "Usuario", usuario.getId(), null);

        // El código de 6 dígitos. La cuenta ya está guardada: si el SMTP falla, `emitir`
        // devuelve false y el alta NO se pierde — el usuario tiene el botón de reenviar.
        boolean mailEnviado = verificacionEmailService.emitir(
                usuario, usuario.getEmail(), PropositoVerificacion.REGISTRO);

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), RolNombre.ALUMNO.name(), usuario.isEmailVerificado());

        return new RegistrarAlumnoResponse(token, mailEnviado, new RegistrarAlumnoResponse.Usuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                rolAlumno.getNombre(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt(),
                usuario.isEmailVerificado(),
                usuario.getAuthProveedor().name()
        ));
    }
}
