package com.activehub.usecases.listarinstructores;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarInstructoresService {

    private final PerfilInstructorRepository perfilInstructorRepository;

    public ListarInstructoresService(PerfilInstructorRepository perfilInstructorRepository) {
        this.perfilInstructorRepository = perfilInstructorRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarInstructoresResponse> listar(EstadoVerificacion estadoVerificacion) {
        List<PerfilInstructor> perfiles = estadoVerificacion != null
                ? perfilInstructorRepository.findByEstadoVerificacion(estadoVerificacion)
                : perfilInstructorRepository.findAll();

        return perfiles.stream()
                .sorted((a, b) -> a.getUsuario().getNombre().compareToIgnoreCase(b.getUsuario().getNombre()))
                .map(this::mapear)
                .toList();
    }

    private ListarInstructoresResponse mapear(PerfilInstructor perfil) {
        var usuario = perfil.getUsuario();
        return new ListarInstructoresResponse(
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
