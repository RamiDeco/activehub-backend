package com.activehub.usecases.obtenerinstructor;

import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;

    public ObtenerInstructorService(PerfilInstructorRepository perfilInstructorRepository) {
        this.perfilInstructorRepository = perfilInstructorRepository;
    }

    @Transactional(readOnly = true)
    public ObtenerInstructorResponse obtener(UUID usuarioId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));
        var usuario = perfil.getUsuario();

        return new ObtenerInstructorResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                usuario.getCreatedAt(),
                perfil.getEspecialidad(),
                perfil.getAniosExperiencia(),
                perfil.getEstadoVerificacion().name(),
                perfil.getMotivoRechazo()
        );
    }
}
