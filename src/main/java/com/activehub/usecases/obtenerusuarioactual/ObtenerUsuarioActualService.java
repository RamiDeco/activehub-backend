package com.activehub.usecases.obtenerusuarioactual;

import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerUsuarioActualService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final ConfiguracionRolRepository configuracionRolRepository;

    public ObtenerUsuarioActualService(
            UsuarioRepository usuarioRepository,
            PerfilAlumnoRepository perfilAlumnoRepository,
            ConfiguracionRolRepository configuracionRolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.configuracionRolRepository = configuracionRolRepository;
    }

    @Transactional(readOnly = true)
    public ObtenerUsuarioActualResponse obtener(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        // Los intereses viven en el perfil de alumno; para los otros dos roles no hay perfil
        // y la lista viaja vacia (no null: el frontend los recorre sin chequear).
        List<ObtenerUsuarioActualResponse.Interes> intereses = perfilAlumnoRepository.findByUsuarioId(usuarioId)
                .map(perfil -> perfil.getInteresesOrdenados().stream()
                        .map(t -> new ObtenerUsuarioActualResponse.Interes(
                                t.getId(), t.getNombre(), t.getCategoria().getId(), t.getCategoria().getNombre()))
                        .toList())
                .orElseGet(List::of);

        List<String> permisos = configuracionRolRepository.findByRolId(usuario.getRol().getId()).stream()
                .filter(ConfiguracionRol::isHabilitado)
                .map(c -> c.getPermiso().getClave())
                .toList();

        return new ObtenerUsuarioActualResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getDni(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                usuario.getRol().getNombre(),
                usuario.getEstado().name(),
                usuario.getCantidadPenalizaciones(),
                usuario.getCreatedAt(),
                usuario.isEmailVerificado(),
                usuario.getAuthProveedor().name(),
                intereses,
                permisos
        );
    }
}
