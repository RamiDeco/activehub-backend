package com.activehub.usecases.desbloquearinstructorpenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.ValidacionException;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = DesbloquearInstructorPenalizacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DesbloquearInstructorPenalizacionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DesbloquearInstructorPenalizacionService service;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    @Test
    void desbloquear_devuelve200ConLaMultaPagada() {
        when(service.desbloquear(any(), any()))
                .thenReturn(new DesbloquearInstructorPenalizacionResponse(UUID.randomUUID(), "PAGADA", null));

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/penalizaciones/" + UUID.randomUUID() + "/desbloquear")
                        .principal(authentication)))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estadoPago").isEqualTo("PAGADA");
    }

    @Test
    void desbloquear_sinComprobante_devuelve400() {
        when(service.desbloquear(any(), any()))
                .thenThrow(new ValidacionException("Esta multa no tiene un comprobante esperando revisión."));

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/penalizaciones/" + UUID.randomUUID() + "/desbloquear")
                        .principal(authentication)))
                .hasStatus(400);
    }
}
