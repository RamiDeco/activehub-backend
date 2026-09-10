package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RN-19: la respuesta sale de ConfiguracionRol. Lo importante es que la ausencia de fila
 * signifique "no", no "sí por defecto".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermisosServiceTest {

    @Mock private ConfiguracionRolRepository configuracionRolRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private PermisosService service;
    private UUID usuarioId;
    private UUID rolId;

    @BeforeEach
    void setUp() {
        service = new PermisosService(configuracionRolRepository, usuarioRepository);

        usuarioId = UUID.randomUUID();
        rolId = UUID.randomUUID();

        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        ReflectionTestUtils.setField(rol, "id", rolId);

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void puede_permisoHabilitado_devuelveTrue() {
        when(configuracionRolRepository.estaHabilitado(rolId, "usuarios.gestionar")).thenReturn(Optional.of(true));

        assertThat(service.puede(usuarioId, "usuarios.gestionar")).isTrue();
    }

    @Test
    void puede_permisoDeshabilitado_devuelveFalse() {
        when(configuracionRolRepository.estaHabilitado(rolId, "usuarios.gestionar")).thenReturn(Optional.of(false));

        assertThat(service.puede(usuarioId, "usuarios.gestionar")).isFalse();
    }

    @Test
    void puede_sinFilaParaEseRol_devuelveFalse() {
        // Clave nueva en el código sin migración: negar es lo seguro.
        when(configuracionRolRepository.estaHabilitado(rolId, "modulo.nuevo")).thenReturn(Optional.empty());

        assertThat(service.puede(usuarioId, "modulo.nuevo")).isFalse();
    }

    @Test
    void puede_usuarioInexistente_devuelveFalse() {
        UUID fantasma = UUID.randomUUID();
        when(usuarioRepository.findById(fantasma)).thenReturn(Optional.empty());

        assertThat(service.puede(fantasma, "usuarios.gestionar")).isFalse();
    }

    @Test
    void puede_sinSesion_devuelveFalse() {
        SecurityContextHolder.clearContext();

        assertThat(service.puede("usuarios.gestionar")).isFalse();
    }

    @Test
    void puede_leeElUsuarioDelSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId, null, java.util.List.of()));
        when(configuracionRolRepository.estaHabilitado(rolId, "auditoria.ver")).thenReturn(Optional.of(true));

        assertThat(service.puede("auditoria.ver")).isTrue();
    }
}
