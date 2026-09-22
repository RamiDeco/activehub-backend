package com.activehub.usecases.subirfotoperfil;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
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
public class SubirFotoPerfilService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final AlmacenamientoArchivos almacenamiento;

    public SubirFotoPerfilService(
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public SubirFotoPerfilResponse subir(MultipartFile archivo, UUID actorId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Seleccioná una foto para subir.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidacionException("Formato no permitido. Subí una foto JPG o PNG.");
        }

        Usuario usuario = usuarioRepository.findById(actorId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "foto";
        String nombreGuardado = UUID.randomUUID() + extension(nombreOriginal);

        try {
            almacenamiento.guardar(CarpetaArchivos.FOTOS_PERFIL, nombreGuardado, archivo.getBytes(), contentType);
        } catch (java.io.IOException | AlmacenamientoException e) {
            throw new ValidacionException("No pudimos guardar la foto. Intentá de nuevo.");
        }

        String fotoAnterior = usuario.getFotoPath();
        usuario.setFotoPath(nombreGuardado);
        usuarioRepository.save(usuario);

        if (fotoAnterior != null) {
            // No bloquea la respuesta si no se pudo borrar la anterior: el contrato de
            // `borrarSiExiste` es best-effort justamente para esto.
            almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_PERFIL, fotoAnterior);
        }

        auditService.registrar(actorId, AuditAccion.FOTO_PERFIL_SUBIDA, "Usuario", usuario.getId(), null);

        return new SubirFotoPerfilResponse(usuario.getId());
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
