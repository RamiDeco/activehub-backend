package com.activehub.usecases.obtenermiperfilinstructor;

import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerMiPerfilInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;

    public ObtenerMiPerfilInstructorService(PerfilInstructorRepository perfilInstructorRepository) {
        this.perfilInstructorRepository = perfilInstructorRepository;
    }

    @Transactional(readOnly = true)
    public ObtenerMiPerfilInstructorResponse obtener(UUID usuarioId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Perfil de instructor no encontrado."));

        return new ObtenerMiPerfilInstructorResponse(
                perfil.getEspecialidad(),
                perfil.getAniosExperiencia(),
                perfil.getDescripcion(),
                perfil.getEstadoVerificacion().name(),
                perfil.getMotivoRechazo()
        );
    }
}
