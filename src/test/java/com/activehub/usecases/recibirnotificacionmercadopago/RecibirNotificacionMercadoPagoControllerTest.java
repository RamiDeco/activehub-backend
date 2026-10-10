package com.activehub.usecases.recibirnotificacionmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.usecases.recibirnotificacionmercadopago.RecibirNotificacionMercadoPagoService.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = RecibirNotificacionMercadoPagoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RecibirNotificacionMercadoPagoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RecibirNotificacionMercadoPagoService service;

    private static final String BODY = "{\"action\":\"order.processed\",\"type\":\"order\",\"data\":{\"id\":\"ORDTST01ABC\"}}";

    @Test
    void notificacionValida_respondeOkYPasaQueryParamsYHeaders() {
        when(service.procesar(any())).thenReturn(Resultado.PROCESADA);

        assertThat(mvc.post().uri("/api/instructor/mercadopago/webhook?data.id=ORDTST01ABC&type=order")
                .header("x-signature", "ts=1,v1=abc").header("x-request-id", "req-1")
                .contentType(MediaType.APPLICATION_JSON).content(BODY).exchange())
                .hasStatus(200);
        verify(service).procesar(new RecibirNotificacionMercadoPagoRequest("ORDTST01ABC", "order", "ts=1,v1=abc", "req-1"));
    }

    @Test
    void ignorada_tambienRespondeOk() {
        // Si respondieramos error, MP reintentaria para siempre algo que no nos importa.
        when(service.procesar(any())).thenReturn(Resultado.IGNORADA);

        assertThat(mvc.post().uri("/api/instructor/mercadopago/webhook?type=payment").contentType(MediaType.APPLICATION_JSON)
                .content("{}").exchange()).hasStatus(200);
    }

    @Test
    void firmaInvalida_responde401() {
        when(service.procesar(any())).thenReturn(Resultado.FIRMA_INVALIDA);

        assertThat(mvc.post().uri("/api/instructor/mercadopago/webhook?data.id=X&type=order").contentType(MediaType.APPLICATION_JSON)
                .content(BODY).exchange()).hasStatus(401);
    }
}
