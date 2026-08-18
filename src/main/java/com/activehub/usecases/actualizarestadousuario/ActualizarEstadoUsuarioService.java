package com.activehub.usecases.actualizarestadousuario;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarEstadoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public ActualizarEstadoUsuarioService(UsuarioRepository usuarioRepository, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarEstadoUsuarioResponse actualizar(UUID id, ActualizarEstadoUsuarioRequest request, UUID actorId) {
        if (id.equals(actorId)) {
            throw new ValidacionException("No podés cambiar tu propio estado.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        EstadoUsuario estado;
        try {
            estado = EstadoUsuario.valueOf(request.estado());
        } catch (IllegalArgumentException ex) {
            throw new ValidacionException("Estado inválido: " + request.estado());
        }

        usuario.setEstado(estado);
        usuarioRepository.save(usuario);

        auditService.registrar(actorId, AuditAccion.USUARIO_ESTADO_ACTUALIZADO, "Usuario", id, estado.name());

        return new ActualizarEstadoUsuarioResponse(usuario.getId(), usuario.getEstado().name());
    }
}
