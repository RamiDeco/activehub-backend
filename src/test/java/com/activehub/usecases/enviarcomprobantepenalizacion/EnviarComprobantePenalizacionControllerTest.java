package com.activehub.usecases.enviarcomprobantepenalizacion;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = EnviarComprobantePenalizacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EnviarComprobantePenalizacionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private EnviarComprobantePenalizacionService service;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    private final MockMultipartFile archivo =
            new MockMultipartFile("archivo", "transferencia.pdf", "application/pdf", "contenido".getBytes());

    @Test
    void enviar_devuelve201ConLaMultaEnRevision() {
        when(service.enviar(any(), any(), any())).thenReturn(
                new EnviarComprobantePenalizacionResponse(UUID.randomUUID(), UUID.randomUUID(), "EN_REVISION"));

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/instructor/sanciones/" + UUID.randomUUID() + "/comprobante")
                        .file(archivo)
                        .principal(authentication)))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.estadoPago").isEqualTo("EN_REVISION");
    }

    @Test
    void enviar_yaEnRevision_devuelve400() {
        when(service.enviar(any(), any(), any()))
                .thenThrow(new ValidacionException("Ya enviaste un comprobante: esperá a que la administración lo revise."));

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/instructor/sanciones/" + UUID.randomUUID() + "/comprobante")
                        .file(archivo)
                        .principal(authentication)))
                .hasStatus(400);
    }

    @Test
    void enviar_sinArchivo_devuelve400() {
        assertThat(mvc.perform(MockMvcRequestBuilders
                        .multipart("/api/instructor/sanciones/" + UUID.randomUUID() + "/comprobante")
                        .principal(authentication)))
                .hasStatus(400);
    }
}
