package com.activehub.usecases.cancelarclase;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = CancelarClaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CancelarClaseControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CancelarClaseService cancelarClaseService;

    @Test
    void cancelar_claseExistente_devuelve200() {
        UUID claseId = UUID.randomUUID();
        when(cancelarClaseService.cancelar(any(), any()))
                .thenReturn(new CancelarClaseResponse(claseId, "Cancelada"));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/instructor/clases/{id}/cancelar", claseId)
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estado").isEqualTo("Cancelada");
    }
}
