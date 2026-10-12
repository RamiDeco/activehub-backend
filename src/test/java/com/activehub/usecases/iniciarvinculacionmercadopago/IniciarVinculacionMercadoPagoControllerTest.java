package com.activehub.usecases.iniciarvinculacionmercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = IniciarVinculacionMercadoPagoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class IniciarVinculacionMercadoPagoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IniciarVinculacionMercadoPagoService service;

    @Test
    void vinculo_devuelveLaUrlDelInstructorLogueado() {
        UUID instructorId = UUID.randomUUID();
        when(service.iniciar(instructorId)).thenReturn(new IniciarVinculacionMercadoPagoResponse("https://auth.mercadopago.com/x"));

        assertThat(mvc.get().uri("/api/instructor/mercadopago/vinculo")
                .principal(new UsernamePasswordAuthenticationToken(instructorId, null, List.of()))
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.url").isEqualTo("https://auth.mercadopago.com/x");
    }
}
