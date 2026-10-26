package com.activehub.usecases.actualizarmicondicionsalud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Hasta este caso de uso, "¿Padecés alguna enfermedad o lesión?" se cargaba sólo en el alta y
 * no se podía cambiar nunca más, aunque el informe de beneficios y prevenciones lo usa.
 */
@ExtendWith(MockitoExtension.class)
class ActualizarMiCondicionSaludServiceTest {

    @Mock
    private PerfilAlumnoRepository perfilAlumnoRepository;
    @Mock
    private AuditService auditService;

    private ActualizarMiCondicionSaludService service;
    private UUID usuarioId;
    private PerfilAlumno perfil;

    @BeforeEach
    void setUp() {
        service = new ActualizarMiCondicionSaludService(perfilAlumnoRepository, auditService);
        usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        perfil = new PerfilAlumno(usuario, List.of());
        perfil.setCondicionSalud("Asma");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());
    }

    @Test
    void actualizar_guardaElTextoRecortadoYAuditaSinElContenido() {
        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        ActualizarMiCondicionSaludResponse response =
                service.actualizar(usuarioId, new ActualizarMiCondicionSaludRequest("  Lesión de rodilla  "));

        assertThat(response.condicionSalud()).isEqualTo("Lesión de rodilla");
        assertThat(perfil.getCondicionSalud()).isEqualTo("Lesión de rodilla");
        verify(perfilAlumnoRepository).save(perfil);
        // Es un dato de salud: en la auditoría queda que se actualizó, nunca qué dice.
        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.CONDICION_SALUD_ACTUALIZADA), eq("PerfilAlumno"), eq(perfil.getId()), isNull());
    }

    @Test
    void actualizar_enBlanco_borraLoQueHabia() {
        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        ActualizarMiCondicionSaludResponse response =
                service.actualizar(usuarioId, new ActualizarMiCondicionSaludRequest("   "));

        assertThat(response.condicionSalud()).isNull();
        assertThat(perfil.getCondicionSalud()).isNull();
    }

    @Test
    void actualizar_sinPerfilDeAlumno_lanzaNoEncontradoYNoAudita() {
        when(perfilAlumnoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(usuarioId, new ActualizarMiCondicionSaludRequest("Asma")))
                .isInstanceOf(NoEncontradoException.class);
        verify(perfilAlumnoRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), any(), any(), any());
    }
}
