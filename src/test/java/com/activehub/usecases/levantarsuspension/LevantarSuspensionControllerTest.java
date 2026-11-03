package com.activehub.usecases.levantarsuspension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = LevantarSuspensionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LevantarSuspensionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LevantarSuspensionService service;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    @Test
    void levantar_conMotivo_devuelve200() {
        when(service.levantar(any(), any(), any()))
                .thenReturn(new LevantarSuspensionResponse(UUID.randomUUID(), Instant.parse("2026-11-02T15:00:00Z"), false));

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/penalizaciones/" + UUID.randomUUID() + "/levantar")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Se aclaró la situación\"}")))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.multaPendiente").isEqualTo(false);
    }

    @Test
    void levantar_sinMotivo_devuelve400() {
        assertThat(mvc.perform(MockMvcRequestBuilders
                        .post("/api/admin/penalizaciones/" + UUID.randomUUID() + "/levantar")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")))
                .hasStatus(400);
    }
}
