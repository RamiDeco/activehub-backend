package com.activehub.usecases.listarusuariosadmin;

import com.activehub.domain.usuario.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarUsuariosAdminService {

    private final UsuarioRepository usuarioRepository;

    public ListarUsuariosAdminService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarUsuariosAdminResponse> listar() {
        return usuarioRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new ListarUsuariosAdminResponse(
                        u.getId(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getEmail(),
                        u.getTelefono(),
                        u.getRol().getNombre().name(),
                        u.getEstado().name(),
                        u.getCantidadPenalizaciones(),
                        u.getCreatedAt()))
                .toList();
    }
}
