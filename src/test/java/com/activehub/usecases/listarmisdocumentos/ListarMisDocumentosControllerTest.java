package com.activehub.usecases.listarmisdocumentos;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = ListarMisDocumentosController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarMisDocumentosControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarMisDocumentosService listarMisDocumentosService;

    @Test
    void listar_devuelve200ConLosDocumentos() {
        when(listarMisDocumentosService.listar(any())).thenReturn(List.of(
                new ListarMisDocumentosResponse(UUID.randomUUID(), "cert.pdf", "application/pdf", 9L, Instant.now(), true)));
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.perform(MockMvcRequestBuilders.get("/api/instructor/documentos").principal(authentication)))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].nuevo").isEqualTo(true);
    }
}
