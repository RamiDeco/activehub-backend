package com.activehub.usecases.solicitarrecuperacionpassword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.email.VerificacionEmailService;
import com.activehub.shared.error.DemasiadosIntentosException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SolicitarRecuperacionPasswordServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VerificacionEmailService verificacionEmailService;

    private SolicitarRecuperacionPasswordService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new SolicitarRecuperacionPasswordService(usuarioRepository, verificacionEmailService);

        usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setEmail("martina@email.com");
        usuario.setEmailVerificado(true);
        usuario.setAuthProveedor(AuthProveedor.LOCAL);
        Rol rol = new Rol();
        rol.setNombre("ALUMNO");
        usuario.setRol(rol);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
    }

    private SolicitarRecuperacionPasswordRequest pedido() {
        return new SolicitarRecuperacionPasswordRequest("martina@email.com");
    }

    @Test
    void solicitar_cuentaVerificadaYLocal_emiteElCodigoAlCorreoDeLaCuenta() {
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of(usuario));
        when(verificacionEmailService.envioHabilitado()).thenReturn(true);
        when(verificacionEmailService.ttlMin()).thenReturn(15);

        SolicitarRecuperacionPasswordResponse respuesta = service.solicitar(pedido());

        assertThat(respuesta.envioHabilitado()).isTrue();
        assertThat(respuesta.ttlMin()).isEqualTo(15);
        verify(verificacionEmailService).reemitir(
                eq(usuario), eq("martina@email.com"), eq(PropositoVerificacion.RECUPERACION_PASSWORD));
    }

    /**
     * El corazón de este usecase: sin cuenta, la respuesta tiene que ser indistinguible de la
     * del caso feliz. Si contestara distinto, el endpoint sería un oráculo para averiguar qué
     * correos están registrados en la plataforma.
     */
    @Test
    void solicitar_correoInexistente_respondeIgualYNoEmiteNada() {
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of());
        when(verificacionEmailService.envioHabilitado()).thenReturn(true);
        when(verificacionEmailService.ttlMin()).thenReturn(15);

        SolicitarRecuperacionPasswordResponse respuesta = service.solicitar(pedido());

        assertThat(respuesta.envioHabilitado()).isTrue();
        assertThat(respuesta.ttlMin()).isEqualTo(15);
        verify(verificacionEmailService, never()).reemitir(any(), any(), any());
    }

    /** Una cuenta de Google no tiene contraseña utilizable: recuperarla no le devolvería el acceso. */
    @Test
    void solicitar_cuentaDeGoogle_noEmiteNada() {
        usuario.setAuthProveedor(AuthProveedor.GOOGLE);
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of(usuario));

        service.solicitar(pedido());

        verify(verificacionEmailService, never()).reemitir(any(), any(), any());
    }

    /**
     * Un correo sin confirmar puede pertenecer a cualquiera (regla de V26): mandarle un código
     * de recuperación sería mandárselo a quien lo tipeó, no a quien lo tiene.
     */
    @Test
    void solicitar_correoSinVerificar_noEmiteNada() {
        usuario.setEmailVerificado(false);
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of(usuario));

        service.solicitar(pedido());

        verify(verificacionEmailService, never()).reemitir(any(), any(), any());
    }

    /**
     * La espera entre reenvíos se traga a propósito: dejar salir el 429 sólo para los correos
     * registrados vuelve a delatar cuáles existen, que es lo que este usecase oculta.
     */
    @Test
    void solicitar_dosVecesSeguidas_noPropagaElErrorDeEspera() {
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of(usuario));
        when(verificacionEmailService.reemitir(any(), any(), any()))
                .thenThrow(new DemasiadosIntentosException("Esperá unos segundos antes de pedir otro código."));
        when(verificacionEmailService.envioHabilitado()).thenReturn(true);
        when(verificacionEmailService.ttlMin()).thenReturn(15);

        SolicitarRecuperacionPasswordResponse respuesta = service.solicitar(pedido());

        assertThat(respuesta.envioHabilitado()).isTrue();
    }

    /** Sin SMTP configurado el código queda en el log del backend; la pantalla lo avisa. */
    @Test
    void solicitar_sinCredencialesDeMail_loInformaEnLaRespuesta() {
        when(usuarioRepository.findAllByEmailConRol("martina@email.com")).thenReturn(List.of(usuario));
        when(verificacionEmailService.envioHabilitado()).thenReturn(false);
        when(verificacionEmailService.ttlMin()).thenReturn(15);

        assertThat(service.solicitar(pedido()).envioHabilitado()).isFalse();
    }
}
