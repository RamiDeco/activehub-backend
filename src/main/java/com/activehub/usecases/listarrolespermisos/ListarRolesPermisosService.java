package com.activehub.usecases.listarrolespermisos;

import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarRolesPermisosService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ConfiguracionRolRepository configuracionRolRepository;
    private final UsuarioRepository usuarioRepository;

    public ListarRolesPermisosService(
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            ConfiguracionRolRepository configuracionRolRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.configuracionRolRepository = configuracionRolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public ListarRolesPermisosResponse listar() {
        // Una sola consulta con JOIN FETCH para toda la matriz: son 3 roles x 15 permisos,
        // no hace falta paginar nada (misma convencion que el resto de los listados admin).
        Map<UUID, List<String>> habilitadosPorRol = configuracionRolRepository.findAllConDetalle().stream()
                .filter(ConfiguracionRol::isHabilitado)
                .collect(Collectors.groupingBy(
                        c -> c.getRol().getId(),
                        Collectors.mapping(c -> c.getPermiso().getClave(), Collectors.toList())));

        List<ListarRolesPermisosResponse.Rol> roles = rolRepository.findAllByOrderBySistemaDescNombreAsc().stream()
                .map(rol -> new ListarRolesPermisosResponse.Rol(
                        rol.getId(),
                        rol.getNombre(),
                        rol.getDescripcion(),
                        rol.isSistema(),
                        usuarioRepository.countByRolIdAndDeletedFalse(rol.getId()),
                        habilitadosPorRol.getOrDefault(rol.getId(), List.of())))
                .toList();

        List<ListarRolesPermisosResponse.Permiso> permisos = permisoRepository.findAllByOrderByOrdenAsc().stream()
                .map(p -> new ListarRolesPermisosResponse.Permiso(
                        p.getId(), p.getClave(), p.getModulo(), p.getAccion(), p.isCritico(), p.esConfigurable()))
                .toList();

        return new ListarRolesPermisosResponse(roles, permisos);
    }
}
