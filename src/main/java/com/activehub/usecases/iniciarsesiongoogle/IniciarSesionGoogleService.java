package com.activehub.usecases.iniciarsesiongoogle;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.UsuarioSuspendidoException;
import com.activehub.shared.security.GoogleIdTokenVerifier;
import com.activehub.shared.security.JwtService;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Continuar con Google": entra si la cuenta existe, y si no la crea.
 *
 * <h2>Es alta y login en el mismo endpoint, a propósito</h2>
 *
 * Nadie que aprieta ese botón sabe si "ya tiene cuenta": eligió una identidad, no un trámite.
 * Separarlo en dos endpoints obligaría a la pantalla a adivinar cuál llamar y a manejar un
 * "ya existe" que para el usuario no es un error.
 *
 * <h2>El correo de Google nace verificado, y eso lo reserva</h2>
 *
 * Google ya confirmó esa dirección (y {@code GoogleIdTokenVerifier} exige
 * {@code email_verified}), así que la cuenta se crea con {@code emailVerificado = true} y no
 * se le manda ningún código. Como es el flag que reserva el correo (V26), a partir de ahí esa
 * dirección queda tomada.
 *
 * <p><b>Si ya había una cuenta VERIFICADA con ese correo, se entra a esa</b>, aunque se haya
 * creado con contraseña: es la misma persona, ya demostró ser dueña del correo por los dos
 * caminos. Las cuentas <b>sin verificar</b> con esa dirección se ignoran — no probaron nada,
 * y justamente por eso no reservan el correo.
 */
@Service
public class IniciarSesionGoogleService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final Clock clock;

    public IniciarSesionGoogleService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilAlumnoRepository perfilAlumnoRepository,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.clock = clock;
    }

    public GoogleConfigResponse config() {
        return new GoogleConfigResponse(googleIdTokenVerifier.habilitado(), googleIdTokenVerifier.clientId());
    }

    @Transactional
    public IniciarSesionGoogleResponse ingresar(IniciarSesionGoogleRequest request) {
        GoogleIdTokenVerifier.DatosGoogle datos = googleIdTokenVerifier.verificar(request.idToken());

        Optional<Usuario> existente = usuarioRepository.findAllByEmailConRol(datos.email()).stream()
                .filter(Usuario::isEmailVerificado)
                .findFirst();

        if (existente.isEmpty()) {
            // Sin rol: viene del botón del LOGIN. No se crea nada — quien aprieta "Iniciar
            // sesión con Google" espera entrar a su cuenta, no que le aparezca una nueva a
            // medio llenar (Google no da teléfono ni fecha de nacimiento).
            if (request.rol() == null || request.rol().isBlank()) {
                return IniciarSesionGoogleResponse.sinCuenta(datos.email());
            }
            // Quiere ser instructor: tampoco se crea. Un instructor no puede darse de alta sin
            // documentación (RN-12) y Google no la trae, así que lo único que aporta es la
            // identidad — vuelve para precargar el formulario, y el alta la hace
            // `registrarinstructor` cuando llegan los archivos.
            if (RolNombre.INSTRUCTOR.name().equals(request.rol())) {
                return IniciarSesionGoogleResponse.completarInstructor(new IniciarSesionGoogleResponse.Identidad(
                        datos.email(), datos.nombre(), datos.apellido(), request.idToken()));
            }
        }

        boolean cuentaNueva = existente.isEmpty();
        Usuario usuario = existente.orElseGet(() -> crear(datos));

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
            auditService.registrar(
                    usuario.getId(), AuditAccion.LOGIN_FALLIDO, "Usuario", usuario.getId(), "usuario suspendido");
            throw new UsuarioSuspendidoException();
        }

        auditService.registrar(usuario.getId(), AuditAccion.LOGIN_OK, "Usuario", usuario.getId(), "google");

        String token = jwtService.emitir(
                usuario.getId(), usuario.getEmail(), usuario.getRol().getNombre(), usuario.isEmailVerificado());
        return IniciarSesionGoogleResponse.sesion(token, cuentaNueva, new IniciarSesionGoogleResponse.Usuario(
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

    /**
     * La cuenta nueva es siempre ALUMNO. El alta de instructor no pasa por acá: exige
     * documentación (RN-12), así que se resuelve en `registrarinstructor` con el ID token
     * adjunto.
     *
     * <p>Queda <b>incompleta a propósito</b>: Google no da teléfono ni fecha de nacimiento, y
     * las dos columnas son nullable. La respuesta marca {@code cuentaNueva} para que la
     * pantalla lo mande a completar el perfil.
     */
    private Usuario crear(GoogleIdTokenVerifier.DatosGoogle datos) {
        var rolAlumno = rolRepository.findByNombre(RolNombre.ALUMNO)
                .orElseThrow(() -> new IllegalStateException("Rol ALUMNO no encontrado, revisar la migracion V2."));

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().isBlank() ? "Usuario" : datos.nombre());
        usuario.setApellido(datos.apellido().isBlank() ? "de Google" : datos.apellido());
        usuario.setEmail(datos.email());
        // `password_hash` es NOT NULL y esta cuenta no tiene contraseña: se pone una aleatoria
        // que nadie conoce. Sin esto habría que dejar la columna nullable y todo el login por
        // contraseña tendría que empezar a contemplar el caso "sin hash".
        usuario.setPasswordHash(passwordEncoder.encode(passwordInutilizable()));
        usuario.setRol(rolAlumno);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setAuthProveedor(AuthProveedor.GOOGLE);
        usuario.setEmailVerificado(true);
        usuario.setEmailVerificadoAt(clock.instant());
        usuario = usuarioRepository.saveAndFlush(usuario);

        perfilAlumnoRepository.save(new PerfilAlumno(usuario, List.of()));

        auditService.registrar(usuario.getId(), AuditAccion.REGISTRO_GOOGLE, "Usuario", usuario.getId(), null);
        return usuario;
    }

    private String passwordInutilizable() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
