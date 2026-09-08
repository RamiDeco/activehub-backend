package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.SinPermisoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstructorVerificadoGuardTest {

    @Mock private PerfilInstructorRepository perfilInstructorRepository;

    @InjectMocks private InstructorVerificadoGuard guard;

    private PerfilInstructor perfilCon(EstadoVerificacion estado) {
        PerfilInstructor perfil = new PerfilInstructor();
        ReflectionTestUtils.setField(perfil, "estadoVerificacion", estado);
        return perfil;
    }

    @Test
    void aprobado_dejaPasar() {
        UUID id = UUID.randomUUID();
        when(perfilInstructorRepository.findByUsuarioId(id))
                .thenReturn(Optional.of(perfilCon(EstadoVerificacion.APROBADO)));

        assertThatCode(() -> guard.exigirVerificado(id, "cancelar clases")).doesNotThrowAnyException();
    }

    @Test
    void pendiente_lanzaSinPermiso() {
        UUID id = UUID.randomUUID();
        when(perfilInstructorRepository.findByUsuarioId(id))
                .thenReturn(Optional.of(perfilCon(EstadoVerificacion.PENDIENTE)));

        assertThatThrownBy(() -> guard.exigirVerificado(id, "cancelar clases"))
                .isInstanceOf(SinPermisoException.class)
                .hasMessageContaining("cancelar clases");
    }

    @Test
    void rechazado_lanzaSinPermiso() {
        UUID id = UUID.randomUUID();
        when(perfilInstructorRepository.findByUsuarioId(id))
                .thenReturn(Optional.of(perfilCon(EstadoVerificacion.RECHAZADO)));

        assertThatThrownBy(() -> guard.exigirVerificado(id, "confirmar cobros"))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void sinPerfilDeInstructor_lanzaSinPermiso() {
        UUID id = UUID.randomUUID();
        when(perfilInstructorRepository.findByUsuarioId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.exigirVerificado(id, "eliminar clases"))
                .isInstanceOf(SinPermisoException.class);
    }
}
