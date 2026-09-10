package com.activehub.usecases.obtenerinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Ficha del instructor que abre el admin desde Gestión (E4Ad-HU03). */
@ExtendWith(MockitoExtension.class)
class ObtenerInstructorServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;

    private ObtenerInstructorService service;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new ObtenerInstructorService(perfilInstructorRepository);
        usuarioId = UUID.randomUUID();
    }

    private PerfilInstructor perfilDe(EstadoVerificacion estado, String motivoRechazo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Mateo");
        usuario.setApellido("Herrera");
        usuario.setEmail("mateo@test.com");
        usuario.setTelefono("+54 261 5550000");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        PerfilInstructor perfil = new PerfilInstructor(usuario, "Running", 6, "desc");
        perfil.setEstadoVerificacion(estado);
        perfil.setMotivoRechazo(motivoRechazo);
        return perfil;
    }

    @Test
    void obtener_devuelveDatosDelUsuarioYDelPerfilJuntos() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(perfilDe(EstadoVerificacion.PENDIENTE, null)));

        ObtenerInstructorResponse response = service.obtener(usuarioId);

        assertThat(response.id()).isEqualTo(usuarioId);
        assertThat(response.nombre()).isEqualTo("Mateo");
        assertThat(response.email()).isEqualTo("mateo@test.com");
        assertThat(response.telefono()).isEqualTo("+54 261 5550000");
        assertThat(response.especialidad()).isEqualTo("Running");
        assertThat(response.aniosExperiencia()).isEqualTo(6);
        assertThat(response.estadoVerificacion()).isEqualTo("PENDIENTE");
    }

    @Test
    void obtener_instructorRechazado_incluyeElMotivo() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(perfilDe(EstadoVerificacion.RECHAZADO, "Falta el título habilitante.")));

        assertThat(service.obtener(usuarioId).motivoRechazo()).isEqualTo("Falta el título habilitante.");
    }

    @Test
    void obtener_usuarioQueNoEsInstructor_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(usuarioId))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessageContaining("Instructor no encontrado");
    }
}
