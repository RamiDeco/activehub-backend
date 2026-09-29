package com.activehub.usecases.subirdocumento;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubirDocumentoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("application/pdf", "image/jpeg", "image/png");

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final DocumentoInstructorRepository documentoInstructorRepository;
    private final AuditService auditService;
    private final AlmacenamientoArchivos almacenamiento;

    public SubirDocumentoService(
            PerfilInstructorRepository perfilInstructorRepository,
            DocumentoInstructorRepository documentoInstructorRepository,
            AuditService auditService,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.documentoInstructorRepository = documentoInstructorRepository;
        this.auditService = auditService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public SubirDocumentoResponse subir(MultipartFile archivo, UUID actorId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Seleccioná un archivo para subir.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidacionException("Formato no permitido. Subí un PDF, JPG o PNG.");
        }

        PerfilInstructor perfil = perfilInstructorRepository.findByUsuarioId(actorId)
                .orElseThrow(() -> new NoEncontradoException("No encontramos tu perfil de instructor."));

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "documento";
        String nombreGuardado = UUID.randomUUID() + extension(nombreOriginal);

        try {
            almacenamiento.guardar(
                    CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, nombreGuardado, archivo.getBytes(), contentType);
        } catch (java.io.IOException | AlmacenamientoException e) {
            throw new ValidacionException("No pudimos guardar el archivo. Intentá de nuevo.");
        }

        DocumentoInstructor documento = new DocumentoInstructor();
        documento.setPerfilInstructor(perfil);
        documento.setTipoDocumento(contentType);
        documento.setRutaArchivo(nombreGuardado);
        documento.setNombreArchivo(nombreOriginal);
        documento.setTamanioBytes(archivo.getSize());
        documento = documentoInstructorRepository.save(documento);

        auditService.registrar(
                actorId, AuditAccion.DOCUMENTO_INSTRUCTOR_SUBIDO, "DocumentoInstructor", documento.getId(), null);

        return new SubirDocumentoResponse(
                documento.getId(), documento.getNombreArchivo(), documento.getTipoDocumento(), documento.getTamanioBytes());
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
