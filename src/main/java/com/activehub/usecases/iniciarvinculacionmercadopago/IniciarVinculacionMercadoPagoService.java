package com.activehub.usecases.iniciarvinculacionmercadopago;

import com.activehub.shared.payments.MercadoPagoOAuthClient;
import com.activehub.shared.payments.MercadoPagoOAuthState;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Primer paso del OAuth: arma la URL de Mercado Pago con un {@code state} firmado que dice
 * QUIEN pidio vincular. El callback no trae JWT (lo llama el navegador desde MP), asi que ese
 * state es la unica forma de saber a que instructor asignarle la cuenta.
 */
@Service
public class IniciarVinculacionMercadoPagoService {

    private final MercadoPagoOAuthState oauthState;
    private final MercadoPagoOAuthClient oauthClient;

    public IniciarVinculacionMercadoPagoService(MercadoPagoOAuthState oauthState, MercadoPagoOAuthClient oauthClient) {
        this.oauthState = oauthState;
        this.oauthClient = oauthClient;
    }

    public IniciarVinculacionMercadoPagoResponse iniciar(UUID instructorId) {
        return new IniciarVinculacionMercadoPagoResponse(oauthClient.urlDeAutorizacion(oauthState.firmar(instructorId)));
    }
}
