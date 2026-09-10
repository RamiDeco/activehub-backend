package com.activehub.usecases.asignarrolusuario;

import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia el rol de una cuenta, incluidos los roles que el admin creó en "Roles y permisos".
 * Hasta acá un rol nuevo no servía para nada: se podía crear y configurar, pero no asignar.
 *
 * <p>Dos guardas:
 * <ul>
 *   <li><b>No podés cambiarte el rol a vos mismo.</b> Misma idea que la guarda
 *       anti-auto-suspensión de {@code actualizarestadousuario}: un admin que se saca el
 *       rol se deja afuera de la plataforma sin nadie que lo devuelva.</li>
 *   <li><b>No se puede dejar la plataforma sin ADMIN</b> del sistema: si el usuario es el
 *       último ADMIN, el cambio se rechaza.</li>
 * </ul>
 *
 * <p>El rol solo define <em>qué permisos</em> trae (RN-19). El perfil (alumno / instructor)
 * no se toca: un instructor que pasa a un rol nuevo conserva su {@code PerfilInstructor}, y
 * los usecases que dependen de él siguen exigiéndolo.
 */
@Service
public class AsignarRolUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditService auditService;

    public AsignarRolUsuarioService(
            UsuarioRepository usuarioRepository, RolRepository rolRepository, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AsignarRolUsuarioResponse asignar(UUID usuarioId, AsignarRolUsuarioRequest request, UUID actorId) {
        if (usuarioId.equals(actorId)) {
            throw new ValidacionException("No podés cambiarte el rol a vos mismo.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));
        Rol nuevo = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new NoEncontradoException("Rol no encontrado."));

        boolean eraAdmin = RolNombre.ADMIN.name().equals(usuario.getRol().getNombre());
        boolean sigueSiendoAdmin = RolNombre.ADMIN.name().equals(nuevo.getNombre());
        if (eraAdmin && !sigueSiendoAdmin
                && usuarioRepository.countByRolIdAndDeletedFalse(usuario.getRol().getId()) <= 1) {
            throw new ValidacionException(
                    "Es el único administrador de la plataforma: asigná otro antes de cambiarle el rol.");
        }

        usuario.setRol(nuevo);
        usuarioRepository.save(usuario);

        auditService.registrar(actorId, AuditAccion.ROL_ASIGNADO, "Usuario", usuarioId, nuevo.getNombre());

        return new AsignarRolUsuarioResponse(usuarioId, nuevo.getId(), nuevo.getNombre());
    }
}
