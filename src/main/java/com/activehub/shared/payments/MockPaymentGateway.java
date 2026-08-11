package com.activehub.shared.payments;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public IniciarPagoResultado iniciarPago(String referenciaExterna, long montoEnCentavos) {
        return new IniciarPagoResultado(referenciaExterna, "https://mock-checkout.local/" + UUID.randomUUID());
    }

    @Override
    public void confirmarPago(String referenciaExterna) {
        // no-op: mock, sin efectos hasta que se enchufe Mercado Pago real
    }

    @Override
    public void cancelarPago(String referenciaExterna) {
        // no-op: mock, sin efectos hasta que se enchufe Mercado Pago real
    }
}
