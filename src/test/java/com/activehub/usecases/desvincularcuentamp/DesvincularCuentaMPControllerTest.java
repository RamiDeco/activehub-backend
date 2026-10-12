package com.activehub.usecases.desvincularcuentamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.activehub.shared.error.CuentaMercadoPagoConPagosPendientesException;
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

@WebMvcTest(controllers = DesvincularCuentaMPController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DesvincularCuentaMPControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DesvincularCuentaMPService service;

    @Test
    void desvincular_devuelve204ConElInstructorDelToken() {
        UUID instructorId = UUID.randomUUID();

        var respuesta = mvc.delete().uri("/api/instructor/mercadopago")
                .principal(new UsernamePasswordAuthenticationToken(instructorId, null, List.of()))
                .exchange();

        assertThat(respuesta).hasStatus(204);
        verify(service).desvincular(instructorId);
    }

    @Test
    void conPagosPendientes_devuelve409ConElCodigo() {
        UUID instructorId = UUID.randomUUID();
        doThrow(new CuentaMercadoPagoConPagosPendientesException(2)).when(service).desvincular(instructorId);

        var respuesta = mvc.delete().uri("/api/instructor/mercadopago")
                .principal(new UsernamePasswordAuthenticationToken(instructorId, null, List.of()))
                .exchange();

        assertThat(respuesta).hasStatus(409).bodyJson().extractingPath("$.code")
                .isEqualTo("MERCADOPAGO_CON_PAGOS_PENDIENTES");
    }
}
