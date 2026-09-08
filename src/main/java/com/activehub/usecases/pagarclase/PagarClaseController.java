package com.activehub.usecases.pagarclase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activehub.shared.payments.MercadoPagoOrderRequest;
import com.activehub.shared.payments.MercadoPagoOrderResponse;
import com.activehub.shared.payments.MercadoPagoPaymentGateway;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PagarClaseController {

    private static final Logger log = LoggerFactory.getLogger(PagarClaseController.class);
    private final MercadoPagoPaymentGateway paymentGateway;

    public PagarClaseController(MercadoPagoPaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @PostMapping("/process")
    public ResponseEntity<MercadoPagoOrderResponse> processOrder(@Valid @RequestBody MercadoPagoOrderRequest request) {
        log.info("Recibida orden de pago Mercado Pago: {}", request);

        MercadoPagoOrderResponse response = paymentGateway.createOrder(request);
        return ResponseEntity.ok(response);
    }
}
