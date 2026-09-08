package com.activehub.usecases.registrarinstructor;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
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
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.security.JwtService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final String directorioAlmacenamiento;

    public RegistrarInstructorService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            @Value("${app.storage.documentos-instructor-dir}") String directorioAlmacenamiento
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
    }

    @Transactional
    public RegistrarInstructorResponse registrar(RegistrarInstructorRequest request, List<MultipartFile> documentos) {
        validarDocumentos(documentos);

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

        guardarDocumentos(documentos, perfilInstructor, usuario.getId());

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
        List<Path> escritos = new ArrayList<>();
        registrarLimpiezaSiHayRollback(escritos);

        Path directorio = Path.of(directorioAlmacenamiento);
        for (MultipartFile archivo : documentos) {
            String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "documento";
            String nombreEnDisco = UUID.randomUUID() + extension(nombreOriginal);
            Path destino = directorio.resolve(nombreEnDisco);

            try {
                Files.createDirectories(directorio);
                Files.write(destino, archivo.getBytes());
                escritos.add(destino);
            } catch (IOException e) {
                // Rompe la transaccion: no queda ni Usuario ni PerfilInstructor. Se devuelve
                // como ValidacionException para que el mensaje llegue tal cual a la pantalla.
                log.error("Fallo al guardar la documentacion del alta de instructor", e);
                throw new ValidacionException("No se pudo crear la cuenta. Intentá de nuevo.");
            }

            DocumentoInstructor documento = new DocumentoInstructor();
            documento.setPerfilInstructor(perfil);
            documento.setTipoDocumento(archivo.getContentType());
            documento.setRutaArchivo(nombreEnDisco);
            documento.setNombreArchivo(nombreOriginal);
            documento.setTamanioBytes(archivo.getSize());
            documentoInstructorRepository.save(documento);

            auditService.registrar(
                    actorId, AuditAccion.DOCUMENTO_INSTRUCTOR_SUBIDO, "DocumentoInstructor", documento.getId(), null);
        }
    }

    /**
     * El filesystem no participa de la transaccion: si la base hace rollback despues de
     * haber escrito archivos, hay que borrarlos para no dejar huerfanos en disco.
     */
    private void registrarLimpiezaSiHayRollback(List<Path> escritos) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                for (Path path : escritos) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("No se pudo borrar el documento huerfano {} tras el rollback del alta", path, e);
                    }
                }
            }
        });
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
