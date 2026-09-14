package com.activehub.usecases.obtenermiperfilinstructor;

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

/**
 * E2I-HU12 ("Visualizar estado de solicitud"). Es el endpoint de "Mis datos" del que habla
 * RN-16: no lo guarda un permiso, lo acota la identidad — quien no tenga PerfilInstructor
 * recibe 404, sea cual sea su rol.
 */
@ExtendWith(MockitoExtension.class)
class ObtenerMiPerfilInstructorServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;

    private ObtenerMiPerfilInstructorService service;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new ObtenerMiPerfilInstructorService(perfilInstructorRepository);
        usuarioId = UUID.randomUUID();
    }

    private static PerfilInstructor perfil(EstadoVerificacion estado, String motivoRechazo) {
        PerfilInstructor p = new PerfilInstructor(new Usuario(), "Running", 6, "Entreno grupos desde 2019.");
        p.setEstadoVerificacion(estado);
        p.setMotivoRechazo(motivoRechazo);
        return p;
    }

    @Test
    void obtener_perfilAprobado_devuelveLosDatosDeLaSolicitud() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(perfil(EstadoVerificacion.APROBADO, null)));

        ObtenerMiPerfilInstructorResponse response = service.obtener(usuarioId);

        assertThat(response.especialidad()).isEqualTo("Running");
        assertThat(response.aniosExperiencia()).isEqualTo(6);
        assertThat(response.estadoVerificacion()).isEqualTo("APROBADO");
        assertThat(response.motivoRechazo()).isNull();
    }

    @Test
    void obtener_perfilRechazado_devuelveElMotivo() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(perfil(EstadoVerificacion.RECHAZADO, "La documentación está vencida.")));

        ObtenerMiPerfilInstructorResponse response = service.obtener(usuarioId);

        assertThat(response.estadoVerificacion()).isEqualTo("RECHAZADO");
        assertThat(response.motivoRechazo()).isEqualTo("La documentación está vencida.");
    }

    @Test
    void obtener_sinPerfilDeInstructor_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(usuarioId))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessageContaining("Perfil de instructor no encontrado");
    }
}
