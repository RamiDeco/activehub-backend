package com.activehub.usecases.resolverdenuncia;

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

@WebMvcTest(controllers = ResolverDenunciaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResolverDenunciaControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ResolverDenunciaService resolverDenunciaService;

    private static final UUID DENUNCIA_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void resolver_bodyValido_devuelve200() {
        when(resolverDenunciaService.resolver(any(), any(), any()))
                .thenReturn(new ResolverDenunciaResponse(DENUNCIA_ID, "Resuelta", "DESESTIMAR"));

        assertThat(mvc.post().uri("/api/admin/denuncias/{id}/resolver", DENUNCIA_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accion\": \"DESESTIMAR\"}")
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("Resuelta");
    }

    @Test
    void resolver_sinAccion_devuelve400() {
        assertThat(mvc.post().uri("/api/admin/denuncias/{id}/resolver", DENUNCIA_ID)
                .principal(principal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accion\": \"\"}")
                .exchange())
                .hasStatus(400);
    }
}
