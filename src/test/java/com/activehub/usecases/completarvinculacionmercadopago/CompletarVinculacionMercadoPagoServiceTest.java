package com.activehub.usecases.completarvinculacionmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructor;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.payments.MercadoPagoOAuthClient;
import com.activehub.shared.payments.MercadoPagoOAuthClient.CredencialesOAuth;
import com.activehub.shared.payments.MercadoPagoOAuthState;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompletarVinculacionMercadoPagoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-09-29T12:00:00Z");
    private static final String FRONT = "http://localhost:5173";
    private static final CredencialesOAuth CREDENCIALES =
            new CredencialesOAuth("APP_USR-at", "TG-rt", "APP_USR-pk", 987654L, 15552000L, false);

    @Mock
    private MercadoPagoOAuthState oauthState;
    @Mock
    private MercadoPagoOAuthClient oauthClient;
    @Mock
    private CuentaMercadoPagoInstructorRepository cuentaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    private CompletarVinculacionMercadoPagoService service;
    private UUID instructorId;
    private Usuario instructor;

    @BeforeEach
    void setUp() {
        service = new CompletarVinculacionMercadoPagoService(oauthState, oauthClient, cuentaRepository,
                usuarioRepository, auditService, Clock.fixed(AHORA, ZoneOffset.UTC), FRONT);
        instructorId = UUID.randomUUID();
        instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);
    }

    private void stubGuardar() {
        when(cuentaRepository.save(any())).thenAnswer(inv -> {
            CuentaMercadoPagoInstructor c = inv.getArgument(0);
            if (c.getId() == null) {
                ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            }
            return c;
        });
    }

    @Test
    void primeraVez_creaLaCuentaVinculadaYRedirigeAlPanel() {
        when(oauthState.verificar("st")).thenReturn(instructorId);
        when(usuarioRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
        when(oauthClient.canjearCodigo("TG-1")).thenReturn(CREDENCIALES);
        when(cuentaRepository.findByInstructorId(instructorId)).thenReturn(Optional.empty());
        stubGuardar();

        String destino = service.completar(new CompletarVinculacionMercadoPagoRequest("TG-1", "st", null));

        assertThat(destino).isEqualTo(FRONT + "/instructor?mercadopago=vinculada");
        var captor = ArgumentCaptor.forClass(CuentaMercadoPagoInstructor.class);
        verify(cuentaRepository).save(captor.capture());
        CuentaMercadoPagoInstructor c = captor.getValue();
        assertThat(c.getInstructor()).isSameAs(instructor);
        assertThat(c.getEstado()).isEqualTo(EstadoCuentaMercadoPago.VINCULADA);
        assertThat(c.getMpUserId()).isEqualTo("987654");
        assertThat(c.getAccessToken()).isEqualTo("APP_USR-at");
        assertThat(c.getRefreshToken()).isEqualTo("TG-rt");
        assertThat(c.getPublicKey()).isEqualTo("APP_USR-pk");
        assertThat(c.getExpiresAt()).isEqualTo(AHORA.plusSeconds(15552000L));
        var metadata = ArgumentCaptor.forClass(String.class);
        verify(auditService).registrar(eq(instructorId), eq(AuditAccion.MERCADOPAGO_VINCULADA),
                eq("CuentaMercadoPagoInstructor"), eq(c.getId()), metadata.capture());
        assertThat(metadata.getValue()).isEqualTo("mp_user_id=987654").doesNotContain("APP_USR");
    }

    @Test
    void vincularDeNuevo_actualizaLaMismaFila() {
        CuentaMercadoPagoInstructor existente = new CuentaMercadoPagoInstructor();
        UUID idFila = UUID.randomUUID();
        ReflectionTestUtils.setField(existente, "id", idFila);
        existente.setInstructor(instructor);
        existente.setMpUserId("111");
        existente.setAccessToken("APP_USR-viejo");
        existente.setEstado(EstadoCuentaMercadoPago.TOKEN_VENCIDO);
        when(oauthState.verificar("st")).thenReturn(instructorId);
        when(usuarioRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
        when(oauthClient.canjearCodigo("TG-2")).thenReturn(CREDENCIALES);
        when(cuentaRepository.findByInstructorId(instructorId)).thenReturn(Optional.of(existente));
        stubGuardar();

        service.completar(new CompletarVinculacionMercadoPagoRequest("TG-2", "st", null));

        verify(cuentaRepository).save(existente);
        assertThat(existente.getId()).isEqualTo(idFila);
        assertThat(existente.getEstado()).isEqualTo(EstadoCuentaMercadoPago.VINCULADA);
        assertThat(existente.getMpUserId()).isEqualTo("987654");
        assertThat(existente.getAccessToken()).isEqualTo("APP_USR-at");
    }

    @Test
    void stateInvalido_lanza400YNoGuardaNada() {
        when(oauthState.verificar("roto")).thenThrow(new ValidacionException("La vinculación con Mercado Pago no es válida."));

        assertThatThrownBy(() -> service.completar(new CompletarVinculacionMercadoPagoRequest("TG-1", "roto", null)))
                .isInstanceOf(ValidacionException.class);
        verifyNoInteractions(oauthClient, cuentaRepository, auditService);
    }

    @Test
    void codigoRechazadoPorMP_redirigeConErrorYNoGuardaNada() {
        when(oauthState.verificar("st")).thenReturn(instructorId);
        when(usuarioRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
        when(oauthClient.canjearCodigo("TG-viejo")).thenThrow(new PagoException("Mercado Pago no aceptó la vinculación."));

        String destino = service.completar(new CompletarVinculacionMercadoPagoRequest("TG-viejo", "st", null));

        assertThat(destino).isEqualTo(FRONT + "/instructor?mercadopago=error");
        verify(cuentaRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), anyString(), any(), any());
    }

    @Test
    void instructorCanceloEnMP_redirigeSinCanjear() {
        when(oauthState.verificar("st")).thenReturn(instructorId);

        String destino = service.completar(new CompletarVinculacionMercadoPagoRequest(null, "st", "access_denied"));

        assertThat(destino).isEqualTo(FRONT + "/instructor?mercadopago=cancelada");
        verifyNoInteractions(oauthClient, cuentaRepository, auditService);
    }
}
