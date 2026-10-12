package com.activehub.usecases.darmedebaja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DarmeDeBajaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditService auditService;

    @InjectMocks private DarmeDeBajaService service;

    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setRol(rol(RolNombre.ALUMNO.name()));
    }

    private Rol rol(String nombre) {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "id", UUID.randomUUID());
        rol.setNombre(nombre);
        rol.setSistema(true);
        return rol;
    }

    @Test
    void darDeBaja_cuentaActiva_haceBajaLogicaYAudita() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.darDeBaja(usuarioId);

        // RN-13: baja lógica, el registro se conserva para el histórico.
        assertThat(usuario.isDeleted()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.CUENTA_DADA_DE_BAJA), eq("Usuario"), eq(usuarioId), any());
    }

    @Test
    void darDeBaja_cuentaYaDadaDeBaja_lanzaValidacion() {
        usuario.marcarBorrado();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.darDeBaja(usuarioId)).isInstanceOf(ValidacionException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void darDeBaja_ultimoAdmin_lanzaValidacionYNoBorra() {
        // Sin esta guarda, el ultimo admin podia borrarse a si mismo y dejar la plataforma sin
        // ninguna cuenta capaz de gestionar usuarios ni configurar roles: no queda pantalla
        // desde la cual devolver el rol y la unica salida es tocar la base a mano.
        usuario.setRol(rol(RolNombre.ADMIN.name()));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByRolIdAndDeletedFalse(usuario.getRol().getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.darDeBaja(usuarioId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("único administrador");
        assertThat(usuario.isDeleted()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void darDeBaja_adminConOtroAdmin_daDeBaja() {
        usuario.setRol(rol(RolNombre.ADMIN.name()));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByRolIdAndDeletedFalse(usuario.getRol().getId())).thenReturn(2L);

        service.darDeBaja(usuarioId);

        assertThat(usuario.isDeleted()).isTrue();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void darDeBaja_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.darDeBaja(usuarioId)).isInstanceOf(NoEncontradoException.class);
    }
}
