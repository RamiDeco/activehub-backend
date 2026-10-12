package com.activehub.usecases.obtenercuentamercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.time.Instant;
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

@WebMvcTest(controllers = ObtenerCuentaMercadoPagoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ObtenerCuentaMercadoPagoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ObtenerCuentaMercadoPagoService service;

    @Test
    void estado_devuelveLaVinculacionSinTokens() throws Exception {
        UUID instructorId = UUID.randomUUID();
        when(service.obtener(instructorId)).thenReturn(new ObtenerCuentaMercadoPagoResponse(
                true, "VINCULADA", "987654", Instant.parse("2026-09-28T12:00:00Z"), Instant.parse("2027-03-27T12:00:00Z")));

        var respuesta = mvc.get().uri("/api/instructor/mercadopago")
                .principal(new UsernamePasswordAuthenticationToken(instructorId, null, List.of()))
                .exchange();

        assertThat(respuesta).hasStatus(200).bodyJson().extractingPath("$.vinculada").isEqualTo(true);
        assertThat(respuesta).bodyJson().extractingPath("$.mpUserId").isEqualTo("987654");
        assertThat(respuesta.getResponse().getContentAsString())
                .doesNotContain("accessToken", "refreshToken", "publicKey", "access_token");
    }
}
