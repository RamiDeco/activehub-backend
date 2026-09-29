package com.activehub.usecases.iniciarvinculacionmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.PagoException;
import com.activehub.shared.payments.MercadoPagoOAuthClient;
import com.activehub.shared.payments.MercadoPagoOAuthState;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IniciarVinculacionMercadoPagoServiceTest {

    @Mock
    private MercadoPagoOAuthState oauthState;
    @Mock
    private MercadoPagoOAuthClient oauthClient;
    @InjectMocks
    private IniciarVinculacionMercadoPagoService service;

    @Test
    void iniciar_armaLaUrlConElStateDelInstructor() {
        UUID instructorId = UUID.randomUUID();
        when(oauthState.firmar(instructorId)).thenReturn("state-firmado");
        when(oauthClient.urlDeAutorizacion("state-firmado")).thenReturn("https://auth.mercadopago.com/authorization?state=state-firmado");

        assertThat(service.iniciar(instructorId).url()).contains("state=state-firmado");
    }

    @Test
    void oauthSinConfigurar_propagaPagoException() {
        when(oauthState.firmar(any())).thenReturn("s");
        when(oauthClient.urlDeAutorizacion("s")).thenThrow(new PagoException("no disponible"));

        assertThatThrownBy(() -> service.iniciar(UUID.randomUUID())).isInstanceOf(PagoException.class);
    }
}
