package com.activehub.usecases.actualizarusuarioadmin;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU02 criterios 4 y 5: el admin edita los datos de un usuario y el cambio queda auditado.
 *
 * <p>No toca el rol ni el estado: el rol es un enum fijo y el estado se maneja desde
 * {@code actualizarestadousuario}, que tiene su propia guarda anti-auto-suspension.
 */
@Service
public class ActualizarUsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public ActualizarUsuarioAdminService(UsuarioRepository usuarioRepository, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarUsuarioAdminResponse actualizar(
            UUID usuarioId, ActualizarUsuarioAdminRequest request, UUID actorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        String emailNuevo = request.email().trim().toLowerCase();
        if (!emailNuevo.equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse(emailNuevo)) {
            throw new EmailEnUsoException();
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setEmail(emailNuevo);
        usuario.setTelefono(request.telefono() != null ? request.telefono().trim() : null);
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuarioRepository.save(usuario);

        auditService.registrar(actorId, AuditAccion.USUARIO_ACTUALIZADO, "Usuario", usuarioId, null);

        return new ActualizarUsuarioAdminResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento());
    }
}
