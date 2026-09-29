package com.activehub.usecases.registrarinstructor;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.DniEnUsoException;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.security.GoogleIdTokenVerifier;
import com.activehub.shared.security.JwtService;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Alta de instructor. E1A-HU04 criterio 9 (RN-12): la operacion es consistente — o se
 * crea el Usuario CON su documentacion, o no se crea nada.
 *
 * <p>Por eso los archivos viajan en el mismo request que los datos y se validan ANTES de
 * tocar la base. Si algo falla (formato, tamanio, escritura en disco) se lanza la excepcion,
 * la transaccion hace rollback y los archivos que ya se habian escrito se borran mediante un
 * hook de sincronizacion: el disco no es transaccional, asi que hay que limpiarlo a mano.
 *
 * <p>La logica de guardado de archivos esta duplicada respecto de {@code subirdocumento} a
 * proposito: la convencion del repo es no inyectar el Service de un usecase dentro de otro.
 */
@Service
public class RegistrarInstructorService {

    private static final Logger log = LoggerFactory.getLogger(RegistrarInstructorService.class);

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_BYTES_POR_ARCHIVO = 5L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final VerificacionEmailService verificacionEmailService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final AlmacenamientoArchivos almacenamiento;

    public RegistrarInstructorService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            VerificacionEmailService verificacionEmailService,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.verificacionEmailService = verificacionEmailService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public RegistrarInstructorResponse registrar(RegistrarInstructorRequest request, List<MultipartFile> documentos) {
        validarDocumentos(documentos);

        // Igual que en el alta de alumno: bloquea el correo CONFIRMADO, no el tipeado (V26).
        if (usuarioRepository.existsVerificadoConEmail(request.email(), null)) {
            throw new EmailEnUsoException();
        }

        // Precondición de E1A-HU04: mismo correo *o DNI*.
        String dni = request.dni() != null && !request.dni().isBlank() ? request.dni().trim() : null;
        if (dni != null && usuarioRepository.existsByDniAndDeletedFalse(dni)) {
            throw new DniEnUsoException();
        }

        var rolInstructor = rolRepository.findByNombre(RolNombre.INSTRUCTOR)
                .orElseThrow(() -> new IllegalStateException("Rol INSTRUCTOR no encontrado, revisar la migracion V2."));

        // "Continuar con Google" para instructor: Google prueba la identidad, pero la cuenta
        // se crea recién acá, cuando llega la documentación (RN-12). El token se vuelve a
        // verificar: entre la precarga del formulario y este request no se guardó estado, y
        // dar por bueno un "ya lo validamos" del cliente sería confiar en el cliente.
        boolean conGoogle = request.googleIdToken() != null && !request.googleIdToken().isBlank();
        if (conGoogle) {
            var datos = googleIdTokenVerifier.verificar(request.googleIdToken());
            // El correo lo fija Google, no el formulario: si no, el token de una casilla
            // serviría para registrar cualquier otra ya verificada.
            if (!datos.email().equalsIgnoreCase(request.email().trim())) {
                throw new ValidacionException(
                        "El correo no coincide con el de tu cuenta de Google.",
                        java.util.Map.of("email", "Tiene que ser el mismo correo de tu cuenta de Google."));
            }
        } else if (request.password() == null || request.password().isBlank()) {
            throw new ValidacionException("La contraseña es obligatoria.",
                    java.util.Map.of("password", "La contraseña es obligatoria."));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setDni(dni);
        // Sin contraseña utilizable cuando entra por Google (ver `IniciarSesionGoogleService`).
        usuario.setPasswordHash(passwordEncoder.encode(
                conGoogle ? passwordInutilizable() : request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setRol(rolInstructor);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        if (conGoogle) {
            usuario.setAuthProveedor(AuthProveedor.GOOGLE);
            usuario.setEmailVerificado(true);
            usuario.setEmailVerificadoAt(java.time.Instant.now());
        }
        usuario = usuarioRepository.saveAndFlush(usuario);

        PerfilInstructor perfilInstructor = new PerfilInstructor(
                usuario, request.especialidad(), request.aniosExperiencia(), request.descripcion());
        perfilInstructorRepository.save(perfilInstructor);

        guardarDocumentos(documentos, perfilInstructor, usuario.getId());

        auditService.registrar(usuario.getId(), AuditAccion.REGISTRO_INSTRUCTOR, "Usuario", usuario.getId(), null);

        // El codigo de 6 digitos. Si el SMTP falla el alta NO se pierde (ver RegistrarAlumnoService).
        // Con Google no hace falta: esa direccion ya la verifico Google.
        boolean mailEnviado = !conGoogle
                && verificacionEmailService.emitir(usuario, usuario.getEmail(), PropositoVerificacion.REGISTRO);

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), RolNombre.INSTRUCTOR.name(), usuario.isEmailVerificado());

        return new RegistrarInstructorResponse(token, mailEnviado, new RegistrarInstructorResponse.Usuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                rolInstructor.getNombre(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt(),
                usuario.isEmailVerificado(),
                usuario.getAuthProveedor().name()
        ));
    }

    /** Contraseña aleatoria para una cuenta de Google: `password_hash` es NOT NULL. */
    private String passwordInutilizable() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validarDocumentos(List<MultipartFile> documentos) {
        if (documentos == null || documentos.isEmpty() || documentos.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ValidacionException("Adjuntá al menos un documento de certificación para crear tu cuenta.");
        }
        for (MultipartFile archivo : documentos) {
            if (archivo.isEmpty()) {
                throw new ValidacionException("Uno de los archivos adjuntos está vacío.");
            }
            if (archivo.getContentType() == null || !TIPOS_PERMITIDOS.contains(archivo.getContentType())) {
                throw new ValidacionException("Formato no admitido. Usá PDF, JPG o PNG.");
            }
            if (archivo.getSize() > MAX_BYTES_POR_ARCHIVO) {
                throw new ValidacionException("El archivo supera el tamaño máximo permitido.");
            }
        }
    }

    private void guardarDocumentos(List<MultipartFile> documentos, PerfilInstructor perfil, UUID actorId) {
        List<String> escritos = new ArrayList<>();
        registrarLimpiezaSiHayRollback(escritos);

        for (MultipartFile archivo : documentos) {
            String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "documento";
            String nombreGuardado = UUID.randomUUID() + extension(nombreOriginal);

            try {
                almacenamiento.guardar(
                        CarpetaArchivos.DOCUMENTOS_INSTRUCTOR,
                        nombreGuardado,
                        archivo.getBytes(),
                        archivo.getContentType());
                escritos.add(nombreGuardado);
            } catch (IOException | AlmacenamientoException e) {
                // Rompe la transaccion: no queda ni Usuario ni PerfilInstructor. Se devuelve
                // como ValidacionException para que el mensaje llegue tal cual a la pantalla.
                log.error("Fallo al guardar la documentacion del alta de instructor", e);
                throw new ValidacionException("No se pudo crear la cuenta. Intentá de nuevo.");
            }

            DocumentoInstructor documento = new DocumentoInstructor();
            documento.setPerfilInstructor(perfil);
            documento.setTipoDocumento(archivo.getContentType());
            documento.setRutaArchivo(nombreGuardado);
            documento.setNombreArchivo(nombreOriginal);
            documento.setTamanioBytes(archivo.getSize());
            documentoInstructorRepository.save(documento);

            auditService.registrar(
                    actorId, AuditAccion.DOCUMENTO_INSTRUCTOR_SUBIDO, "DocumentoInstructor", documento.getId(), null);
        }
    }

    /**
     * El almacenamiento no participa de la transaccion —ni el disco ni Supabase Storage—: si
     * la base hace rollback despues de haber escrito archivos, hay que borrarlos para no dejar
     * huerfanos.
     */
    private void registrarLimpiezaSiHayRollback(List<String> escritos) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                for (String nombre : escritos) {
                    // `borrarSiExiste` es best-effort y no lanza: despues del rollback ya no
                    // hay a quien reportarle que quedo un archivo colgado.
                    almacenamiento.borrarSiExiste(CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, nombre);
                }
            }
        });
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
