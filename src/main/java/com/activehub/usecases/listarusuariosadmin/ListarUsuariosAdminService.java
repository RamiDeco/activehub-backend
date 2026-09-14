package com.activehub.usecases.listarusuariosadmin;

import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarUsuariosAdminService {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionRolRepository configuracionRolRepository;

    public ListarUsuariosAdminService(
            UsuarioRepository usuarioRepository, ConfiguracionRolRepository configuracionRolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.configuracionRolRepository = configuracionRolRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarUsuariosAdminResponse> listar() {
        // Una sola consulta para toda la lista en vez de una por usuario.
        Set<UUID> rolesQueDanClases = new HashSet<>(configuracionRolRepository.rolesConPermiso("clases.gestionar"));
        return usuarioRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new ListarUsuariosAdminResponse(
                        u.getId(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getEmail(),
                        u.getDni(),
                        u.getTelefono(),
                        u.getRol().getNombre(),
                        u.getEstado().name(),
                        u.getCantidadPenalizaciones(),
                        u.getCreatedAt(),
                        rolesQueDanClases.contains(u.getRol().getId())))
                .toList();
    }
}
