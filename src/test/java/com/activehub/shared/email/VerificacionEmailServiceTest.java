package com.activehub.shared.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.PropositoVerificacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.VerificacionEmail;
import com.activehub.domain.usuario.VerificacionEmailRepository;
import com.activehub.shared.error.DemasiadosIntentosException;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerificacionEmailServiceTest {

    private static final Instant AHORA = Instant.parse("2026-03-10T12:00:00Z");

    @Mock
    private VerificacionEmailRepository verificacionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailSender emailSender;

    private VerificacionEmailService service;
    private Usuario usuario;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new VerificacionEmailService(
                verificacionRepository, passwordEncoder, emailSender,
                Clock.fixed(AHORA, ZoneOffset.UTC), 15, 60, 5);

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setEmail("martina@email.com");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private VerificacionEmail pendiente(String hash) {
        VerificacionEmail v = new VerificacionEmail(
                usuario, "martina@email.com", hash, PropositoVerificacion.REGISTRO, AHORA.plusSeconds(900));
        ReflectionTestUtils.setField(v, "createdAt", AHORA);
        return v;
    }

    @Test
    void emitir_guardaElCodigoHasheadoYMandaElMail() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(emailSender.enviar(any(), any(), any())).thenReturn(true);

        assertThat(service.emitir(usuario, "martina@email.com", PropositoVerificacion.REGISTRO)).isTrue();

        // Los pendientes se invalidan ANTES: con dos códigos vivos a la vez, el tope de
        // intentos de uno no protege al otro.
        verify(verificacionRepository).invalidarPendientes(usuarioId, AHORA);

        ArgumentCaptor<VerificacionEmail> captor = ArgumentCaptor.forClass(VerificacionEmail.class);
        verify(verificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigoHash()).isEqualTo("hash");
        assertThat(captor.getValue().getExpiraAt()).isEqualTo(AHORA.plusSeconds(15 * 60));

        // El código en claro no se guarda en ningún lado: sólo viaja al mail.
        ArgumentCaptor<String> codigoEncodeado = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(codigoEncodeado.capture());
        assertThat(codigoEncodeado.getValue()).matches("[0-9]{6}");
    }

    @Test
    void emitir_conSmtpCaido_noLanza_soloDevuelveFalse() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(emailSender.enviar(any(), any(), any())).thenReturn(false);

        // Es la garantía que sostiene al alta: la cuenta ya está guardada cuando esto corre.
        assertThat(service.emitir(usuario, "martina@email.com", PropositoVerificacion.REGISTRO)).isFalse();
        verify(verificacionRepository).save(any());
    }

    @Test
    void reemitir_antesDeLaEsperaMinima_lanza429() {
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId))
                .thenReturn(Optional.of(pendiente("hash")));

        assertThatThrownBy(() -> service.reemitir(usuario, "martina@email.com", PropositoVerificacion.REGISTRO))
                .isInstanceOf(DemasiadosIntentosException.class);

        verify(verificacionRepository, never()).save(any());
    }

    @Test
    void validar_codigoCorrecto_marcaUsado() {
        VerificacionEmail v = pendiente("hash");
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(Optional.of(v));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertThat(service.validar(usuarioId, "123456").getUsadoAt()).isEqualTo(AHORA);
        verify(verificacionRepository).save(v);
    }

    @Test
    void validar_codigoIncorrecto_sumaIntentoYLoPERSISTE() {
        VerificacionEmail v = pendiente("hash");
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(Optional.of(v));
        when(passwordEncoder.matches(eq("000000"), eq("hash"))).thenReturn(false);

        assertThatThrownBy(() -> service.validar(usuarioId, "000000"))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("4 intentos");

        // saveAndFlush, no save: si el incremento quedara pendiente en la transacción, el
        // rollback de la excepción se lo llevaría y el tope no contaría nunca.
        verify(verificacionRepository).saveAndFlush(v);
        assertThat(v.getIntentos()).isEqualTo(1);
    }

    @Test
    void validar_codigoVencido_lanza() {
        VerificacionEmail v = pendiente("hash");
        v.setExpiraAt(AHORA.minusSeconds(1));
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.validar(usuarioId, "123456"))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("venció");
    }

    @Test
    void validar_codigoYaUsado_noSirveDeNuevo() {
        VerificacionEmail v = pendiente("hash");
        v.setUsadoAt(AHORA.minusSeconds(10));
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.validar(usuarioId, "123456"))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya se usó");
    }

    @Test
    void validar_intentosAgotados_noSigueProbando() {
        VerificacionEmail v = pendiente("hash");
        v.setIntentos(5);
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.validar(usuarioId, "123456"))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("intentos permitidos");
        // Ni siquiera se compara: 6 dígitos son 10^6 y el tope es lo único que lo hace caro.
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void validar_sinCodigoPendiente_lanza() {
        when(verificacionRepository.findFirstByUsuarioIdOrderByCreatedAtDesc(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(usuarioId, "123456"))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ningún código pendiente");
    }
}
