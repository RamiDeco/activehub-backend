package com.activehub.usecases.asignarrolusuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsignarRolUsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private AuditService auditService;

    private AsignarRolUsuarioService service;

    private UUID actorId;
    private UUID usuarioId;
    private Rol rolAlumno;
    private Rol rolAdmin;
    private Rol rolSoporte;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new AsignarRolUsuarioService(usuarioRepository, rolRepository, auditService);

        actorId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();

        rolAlumno = rol(RolNombre.ALUMNO.name(), true);
        rolAdmin = rol(RolNombre.ADMIN.name(), true);
        rolSoporte = rol("Soporte", false);

        usuario = new Usuario();
        usuario.setRol(rolAlumno);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(rolSoporte.getId())).thenReturn(Optional.of(rolSoporte));
        when(rolRepository.findById(rolAdmin.getId())).thenReturn(Optional.of(rolAdmin));
        when(rolRepository.findById(rolAlumno.getId())).thenReturn(Optional.of(rolAlumno));
    }

    private Rol rol(String nombre, boolean sistema) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setSistema(sistema);
        ReflectionTestUtils.setField(rol, "id", UUID.randomUUID());
        return rol;
    }

    @Test
    void asignar_rolCreadoPorElAdmin_seGuardaYAudita() {
        var response = service.asignar(usuarioId, new AsignarRolUsuarioRequest(rolSoporte.getId()), actorId);

        assertThat(response.rol()).isEqualTo("Soporte");
        assertThat(usuario.getRol()).isEqualTo(rolSoporte);
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(
                eq(actorId), eq(AuditAccion.ROL_ASIGNADO), eq("Usuario"), eq(usuarioId), eq("Soporte"));
    }

    @Test
    void asignar_aSiMismo_lanzaValidacion() {
        // Misma idea que la guarda anti-auto-suspensión: si se saca el rol, no vuelve a entrar.
        assertThatThrownBy(() -> service.asignar(actorId, new AsignarRolUsuarioRequest(rolSoporte.getId()), actorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("a vos mismo");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void asignar_alUnicoAdmin_lanzaValidacion() {
        usuario.setRol(rolAdmin);
        when(usuarioRepository.countByRolIdAndDeletedFalse(rolAdmin.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.asignar(usuarioId, new AsignarRolUsuarioRequest(rolSoporte.getId()), actorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("único administrador");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void asignar_aUnAdminCuandoHayVarios_siCambia() {
        usuario.setRol(rolAdmin);
        when(usuarioRepository.countByRolIdAndDeletedFalse(rolAdmin.getId())).thenReturn(3L);

        var response = service.asignar(usuarioId, new AsignarRolUsuarioRequest(rolSoporte.getId()), actorId);

        assertThat(response.rol()).isEqualTo("Soporte");
    }

    @Test
    void asignar_rolInexistente_lanzaNoEncontrado() {
        UUID fantasma = UUID.randomUUID();
        when(rolRepository.findById(fantasma)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignar(usuarioId, new AsignarRolUsuarioRequest(fantasma), actorId))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void asignar_usuarioInexistente_lanzaNoEncontrado() {
        UUID fantasma = UUID.randomUUID();
        when(usuarioRepository.findById(fantasma)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignar(fantasma, new AsignarRolUsuarioRequest(rolSoporte.getId()), actorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
