package com.activehub.usecases.iniciarsesion;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.DemasiadosIntentosException;
import com.activehub.shared.security.IntentosLoginService;
import com.activehub.shared.error.UsuarioSuspendidoException;
import com.activehub.shared.security.JwtService;
import java.util.List;
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
    private final IntentosLoginService intentosLoginService;

    public IniciarSesionService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            IntentosLoginService intentosLoginService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.intentosLoginService = intentosLoginService;
    }

    /**
     * OJO: este metodo NO puede ser @Transactional.
     *
     * <p>Los caminos de fallo escriben en audit_log y despues lanzan una excepcion. Como
     * ApiException extiende RuntimeException, Spring hacia rollback de toda la transaccion
     * y se borraba el propio INSERT de auditoria: en el registro solo quedaban los LOGIN_OK
     * y los intentos fallidos se perdian, justo el evento que la especificacion pide auditar.
     * Sin transaccion ambiente, cada save commitea por su cuenta y el registro sobrevive.
     */
    public IniciarSesionResponse login(IniciarSesionRequest request) {
        String identificador = request.identificador().trim();

        // El bloqueo se evalua antes que nada: no tiene sentido seguir verificando
        // credenciales de un identificador que ya esta frenado por fuerza bruta.
        if (intentosLoginService.estaBloqueado(identificador)) {
            auditService.registrar(null, AuditAccion.LOGIN_BLOQUEADO, "Usuario", null, identificador);
            throw new DemasiadosIntentosException();
        }

        // Con el Rol ya cargado: este método no es transaccional (ver javadoc de arriba) y
        // fuera de la sesión el proxy lazy de Rol no se puede inicializar.
        // Un DNI (solo dígitos) nunca puede ser un email, así que el discriminador es seguro.
        //
        // OJO: por email puede haber VARIAS cuentas. Desde V26 un correo sin confirmar no es
        // único — es la regla pedida: mientras nadie ingresó el código, cualquiera puede
        // registrarse con esa dirección. Así que "el usuario de este email" ya no tiene una
        // sola respuesta, y lo que desambigua es la CONTRASEÑA: dos personas que tipearon el
        // mismo correo tienen claves distintas. Se recorren las candidatas (la verificada
        // primero, que es la dueña del correo) y se toma la que coincide.
        List<Usuario> candidatas = esDni(identificador)
                ? usuarioRepository.findByDniConRol(identificador).map(List::of).orElseGet(List::of)
                : usuarioRepository.findAllByEmailConRol(identificador);

        Usuario usuario = candidatas.stream()
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .findFirst()
                .orElse(null);

        if (usuario == null) {
            // Con varias candidatas no hay un "actor" al que atribuir el fallo; se registra
            // sin actor, igual que un email inexistente.
            UUID actorId = candidatas.size() == 1 ? candidatas.get(0).getId() : null;
            auditService.registrar(actorId, AuditAccion.LOGIN_FALLIDO, "Usuario", actorId, identificador);
            intentosLoginService.registrarFallo(identificador);
            throw new CredencialesInvalidasException();
        }

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
            auditService.registrar(usuario.getId(), AuditAccion.LOGIN_FALLIDO, "Usuario", usuario.getId(), "usuario suspendido");
            throw new UsuarioSuspendidoException();
        }

        intentosLoginService.registrarExito(identificador);
        auditService.registrar(usuario.getId(), AuditAccion.LOGIN_OK, "Usuario", usuario.getId(), null);

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), usuario.getRol().getNombre(), usuario.isEmailVerificado());

        return new IniciarSesionResponse(token, new IniciarSesionResponse.Usuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                usuario.getRol().getNombre(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt(),
                usuario.isEmailVerificado(),
                usuario.getAuthProveedor().name()
        ));
    }

    private boolean esDni(String identificador) {
        return identificador.matches("[0-9]{7,8}");
    }
}
