package com.activehub.usecases.invalidarcomprobantepenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = InvalidarComprobantePenalizacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InvalidarComprobantePenalizacionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private InvalidarComprobantePenalizacionService service;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    @Test
    void invalidar_conMotivo_devuelve200() {
        when(service.invalidar(any(), any(), any()))
                .thenReturn(new InvalidarComprobantePenalizacionResponse(UUID.randomUUID(), "PENDIENTE_PAGO"));

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/penalizaciones/" + UUID.randomUUID() + "/invalidar-comprobante")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"El monto no coincide\"}")))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estadoPago").isEqualTo("PENDIENTE_PAGO");
    }

    @Test
    void invalidar_sinMotivo_devuelve400() {
        assertThat(mvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/penalizaciones/" + UUID.randomUUID() + "/invalidar-comprobante")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"\"}")))
                .hasStatus(400);
    }
}
