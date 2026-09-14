package com.activehub.usecases.actualizarmiperfil;

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
 * E3A-HU12 criterio 2 (y E2I-HU12 criterio 4): el usuario edita sus propios datos.
 *
 * <p>Hasta ahora no existia ningun endpoint para esto: la pantalla de Perfil llamaba a una
 * funcion del frontend que solo mutaba un array mock, asi que "Guardar cambios" no persistia
 * nada y al recargar volvia todo atras.
 *
 * <p>Sirve para los tres roles — el instructor no verificado tambien puede editar sus datos
 * (es lo unico que la spec le habilita mientras espera la validacion).
 */
@Service
public class ActualizarMiPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public ActualizarMiPerfilService(UsuarioRepository usuarioRepository, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarMiPerfilResponse actualizar(UUID usuarioId, ActualizarMiPerfilRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        String emailNuevo = request.email().trim().toLowerCase();
        // Criterio 5: si el correo cambia, no puede pisar el de otra cuenta activa.
        if (!emailNuevo.equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse(emailNuevo)) {
            throw new EmailEnUsoException();
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setEmail(emailNuevo);
        usuario.setTelefono(request.telefono().trim());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuarioRepository.save(usuario);

        auditService.registrar(usuarioId, AuditAccion.PERFIL_ACTUALIZADO, "Usuario", usuarioId, null);

        return new ActualizarMiPerfilResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento());
    }
}
