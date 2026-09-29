package com.activehub.usecases.completarvinculacionmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.ValidacionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = CompletarVinculacionMercadoPagoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CompletarVinculacionMercadoPagoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CompletarVinculacionMercadoPagoService service;

    @Test
    void callbackOk_redirigeAlFrontendCon302() {
        when(service.completar(new CompletarVinculacionMercadoPagoRequest("TG-1", "st", null)))
                .thenReturn("http://localhost:5173/instructor?mercadopago=vinculada");

        assertThat(mvc.get().uri("/api/instructor/mercadopago/callback?code=TG-1&state=st").exchange())
                .hasStatus(302)
                .hasHeader("Location", "http://localhost:5173/instructor?mercadopago=vinculada");
    }

    @Test
    void stateInvalido_devuelve400ConElFormatoDeError() {
        when(service.completar(any())).thenThrow(new ValidacionException("La vinculación con Mercado Pago no es válida."));

        assertThat(mvc.get().uri("/api/instructor/mercadopago/callback?code=TG-1&state=roto").exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");
    }
}
