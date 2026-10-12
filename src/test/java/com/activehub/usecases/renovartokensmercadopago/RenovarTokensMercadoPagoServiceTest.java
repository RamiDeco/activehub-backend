package com.activehub.usecases.renovartokensmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.MercadoPagoOAuthClient;
import com.activehub.shared.payments.MercadoPagoOAuthClient.CredencialesOAuth;
import com.activehub.shared.payments.MercadoPagoOAuthClient.RenovacionRechazadaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RenovarTokensMercadoPagoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-10T12:00:00Z");
    private static final long SEIS_MESES = 15552000L;

    @Mock
    private CuentaMercadoPagoInstructorRepository cuentaRepository;
    @Mock
    private MercadoPagoOAuthClient oauthClient;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private AuditService auditService;

    private RenovarTokensMercadoPagoService service;

    @BeforeEach
    void setUp() {
        service = new RenovarTokensMercadoPagoService(cuentaRepository, oauthClient, notificacionService,
                auditService, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static CuentaMercadoPagoInstructor cuenta(Instant expira) {
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());
        CuentaMercadoPagoInstructor c = new CuentaMercadoPagoInstructor();
        c.setId(UUID.randomUUID());
        c.setInstructor(instructor);
        c.setMpUserId("987654");
        c.setAccessToken("APP_USR-viejo");
        c.setRefreshToken("TG-viejo");
        c.setPublicKey("APP_USR-pk");
        c.setExpiresAt(expira);
        c.setEstado(EstadoCuentaMercadoPago.VINCULADA);
        return c;
    }

    private void enVentana(CuentaMercadoPagoInstructor c) {
        when(cuentaRepository.findIdsQueVencenAntesDe(EstadoCuentaMercadoPago.VINCULADA,
                AHORA.plus(RenovarTokensMercadoPagoService.VENTANA_RENOVACION))).thenReturn(List.of(c.getId()));
        when(cuentaRepository.findById(c.getId())).thenReturn(Optional.of(c));
    }

    @Test
    void consultaConLaVentanaCalculadaDesdeElReloj() {
        when(cuentaRepository.findIdsQueVencenAntesDe(any(), any())).thenReturn(List.of());

        assertThat(service.renovar()).isZero();

        verify(cuentaRepository).findIdsQueVencenAntesDe(EstadoCuentaMercadoPago.VINCULADA,
                AHORA.plus(Duration.ofDays(7)));
        verifyNoInteractions(oauthClient);
    }

    @Test
    void tokenAPuntoDeVencer_seRenuevaYGuardaElParNuevo() {
        var c = cuenta(AHORA.plus(Duration.ofDays(2)));
        enVentana(c);
        when(oauthClient.renovar("TG-viejo"))
                .thenReturn(new CredencialesOAuth("APP_USR-nuevo", "TG-nuevo", "APP_USR-pk", 987654L, SEIS_MESES, false));

        assertThat(service.renovar()).isEqualTo(1);

        assertThat(c.getAccessToken()).isEqualTo("APP_USR-nuevo");
        assertThat(c.getRefreshToken()).isEqualTo("TG-nuevo");
        assertThat(c.getExpiresAt()).isEqualTo(AHORA.plusSeconds(SEIS_MESES));
        assertThat(c.getEstado()).isEqualTo(EstadoCuentaMercadoPago.VINCULADA);
        verify(cuentaRepository).save(c);
        verify(auditService).registrar(eq(null), eq(AuditAccion.MERCADOPAGO_TOKEN_RENOVADO),
                eq("CuentaMercadoPagoInstructor"), eq(c.getId()), anyString());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void tokenYaVencido_tambienSeIntentaRenovar() {
        var c = cuenta(AHORA.minus(Duration.ofDays(1)));
        enVentana(c);
        when(oauthClient.renovar("TG-viejo"))
                .thenReturn(new CredencialesOAuth("APP_USR-nuevo", "TG-nuevo", null, 987654L, SEIS_MESES, false));

        assertThat(service.renovar()).isEqualTo(1);

        assertThat(c.getExpiresAt()).isEqualTo(AHORA.plusSeconds(SEIS_MESES));
        // Si MP no manda public key, se conserva la que habia.
        assertThat(c.getPublicKey()).isEqualTo("APP_USR-pk");
    }

    @Test
    void tokenLejano_noSeToca() {
        // La consulta filtra por vencimiento: una cuenta que vence en 5 meses no vuelve.
        when(cuentaRepository.findIdsQueVencenAntesDe(any(), any())).thenReturn(List.of());

        service.renovar();

        verify(cuentaRepository, never()).findById(any());
        verify(cuentaRepository, never()).save(any());
        verifyNoInteractions(oauthClient, auditService, notificacionService);
    }

    @Test
    void renovacionRechazada_quedaTokenVencidoSinTokensYSeNotificaAlInstructor() {
        var c = cuenta(AHORA.plus(Duration.ofDays(1)));
        enVentana(c);
        when(oauthClient.renovar("TG-viejo")).thenThrow(new RenovacionRechazadaException());

        assertThat(service.renovar()).isZero();

        assertThat(c.getEstado()).isEqualTo(EstadoCuentaMercadoPago.TOKEN_VENCIDO);
        assertThat(c.getAccessToken()).isNull();
        assertThat(c.getRefreshToken()).isNull();
        verify(cuentaRepository).save(c);
        verify(auditService).registrar(eq(null), eq(AuditAccion.MERCADOPAGO_TOKEN_VENCIDO),
                eq("CuentaMercadoPagoInstructor"), eq(c.getId()), anyString());
        verify(notificacionService).notificar(c.getInstructor().getId(), TipoNotificacion.MERCADOPAGO_TOKEN_VENCIDO,
                RenovarTokensMercadoPagoService.MENSAJE_TOKEN_VENCIDO, c.getId(), Destino.ninguno());
    }

    @Test
    void sinRefreshToken_quedaTokenVencidoSinLlamarAMercadoPago() {
        var c = cuenta(AHORA.plus(Duration.ofDays(1)));
        c.setRefreshToken(null);
        enVentana(c);

        service.renovar();

        assertThat(c.getEstado()).isEqualTo(EstadoCuentaMercadoPago.TOKEN_VENCIDO);
        verifyNoInteractions(oauthClient);
        verify(notificacionService).notificar(eq(c.getInstructor().getId()),
                eq(TipoNotificacion.MERCADOPAGO_TOKEN_VENCIDO), anyString(), eq(c.getId()), eq(Destino.ninguno()));
    }

    @Test
    void mercadoPagoCaido_noTocaLaCuentaYSeReintentaDespues() {
        var c = cuenta(AHORA.plus(Duration.ofDays(1)));
        enVentana(c);
        when(oauthClient.renovar("TG-viejo")).thenThrow(new PagoException("No se pudo conectar con Mercado Pago."));

        assertThat(service.renovar()).isZero();

        assertThat(c.getEstado()).isEqualTo(EstadoCuentaMercadoPago.VINCULADA);
        assertThat(c.getRefreshToken()).isEqualTo("TG-viejo");
        verify(cuentaRepository, never()).save(any());
        verifyNoInteractions(auditService, notificacionService);
    }

    @Test
    void unaCuentaIlegible_noCortaLaCorridaParaLasDemas() {
        var ilegible = UUID.randomUUID();
        var c = cuenta(AHORA.plus(Duration.ofDays(1)));
        when(cuentaRepository.findIdsQueVencenAntesDe(any(), any())).thenReturn(List.of(ilegible, c.getId()));
        when(cuentaRepository.findById(ilegible)).thenThrow(new IllegalStateException("no se pudo descifrar"));
        when(cuentaRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(oauthClient.renovar("TG-viejo"))
                .thenReturn(new CredencialesOAuth("APP_USR-nuevo", "TG-nuevo", null, 987654L, SEIS_MESES, false));

        assertThat(service.renovar()).isEqualTo(1);
        assertThat(c.getAccessToken()).isEqualTo("APP_USR-nuevo");
    }
}
