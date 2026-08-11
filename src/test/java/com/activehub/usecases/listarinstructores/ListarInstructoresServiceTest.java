package com.activehub.usecases.listarinstructores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarInstructoresServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;

    private ListarInstructoresService service;

    private PerfilInstructor perfil(String nombre, EstadoVerificacion estado) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido("Apellido");
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        PerfilInstructor perfil = new PerfilInstructor(usuario, "Yoga", 5, "desc");
        perfil.setEstadoVerificacion(estado);
        return perfil;
    }

    @Test
    void listar_sinFiltro_devuelveTodosOrdenadosPorNombre() {
        service = new ListarInstructoresService(perfilInstructorRepository);
        when(perfilInstructorRepository.findAll()).thenReturn(List.of(
                perfil("Zoe", EstadoVerificacion.APROBADO),
                perfil("Ana", EstadoVerificacion.PENDIENTE)));

        List<ListarInstructoresResponse> resultado = service.listar(null);

        assertThat(resultado).extracting(ListarInstructoresResponse::nombre).containsExactly("Ana", "Zoe");
    }

    @Test
    void listar_conFiltroPendiente_soloDevuelvePendientes() {
        service = new ListarInstructoresService(perfilInstructorRepository);
        when(perfilInstructorRepository.findByEstadoVerificacion(EstadoVerificacion.PENDIENTE))
                .thenReturn(List.of(perfil("Ana", EstadoVerificacion.PENDIENTE)));

        List<ListarInstructoresResponse> resultado = service.listar(EstadoVerificacion.PENDIENTE);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estadoVerificacion()).isEqualTo("PENDIENTE");
    }
}
