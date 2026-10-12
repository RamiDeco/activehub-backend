package com.activehub.shared.security;

import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-19: el permiso se resuelve consultando {@code ConfiguracionRol} en la base, no con
 * condicionales hardcodeados. Se usa desde los controllers como
 * {@code @PreAuthorize("@permisos.puede('usuarios.gestionar')")}.
 *
 * <p><b>Sin caché a propósito.</b> El criterio 2 de E4Ad-HU08 pide que el cambio impacte
 * "de inmediato": una caché en memoria dejaría a los usuarios ya logueados operando con la
 * configuración vieja hasta que expirara. La consulta es un solo índice sobre
 * {@code (rol_id, permiso_id)}, así que el costo por request es despreciable.
 *
 * <p>Ojo con el nombre del bean: la expresión SpEL de los {@code @PreAuthorize} lo busca
 * como <b>{@code permisos}</b>. Si se renombra la clase sin mantener ese nombre, las guardas
 * fallan en runtime, no en compilación.
 */
@Component("permisos")
public class PermisosService {

    private final ConfiguracionRolRepository configuracionRolRepository;
    private final UsuarioRepository usuarioRepository;

    public PermisosService(
            ConfiguracionRolRepository configuracionRolRepository, UsuarioRepository usuarioRepository) {
        this.configuracionRolRepository = configuracionRolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** ¿El usuario logueado tiene este permiso, según la configuración de su rol? */
    @Transactional(readOnly = true)
    public boolean puede(String clave) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID usuarioId)) {
            return false;
        }
        return puede(usuarioId, clave);
    }

    @Transactional(readOnly = true)
    public boolean puede(UUID usuarioId, String clave) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        // Sin fila para ese (rol, permiso) el permiso no está concedido: negar es lo seguro
        // cuando alguien agrega una clave nueva al código y se olvida de la migración.
        return configuracionRolRepository.estaHabilitado(usuario.getRol().getId(), clave).orElse(false);
    }
}
