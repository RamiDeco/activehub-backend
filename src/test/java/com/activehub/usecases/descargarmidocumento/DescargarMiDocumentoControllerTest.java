package com.activehub.usecases.descargarmidocumento;

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = DescargarMiDocumentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DescargarMiDocumentoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DescargarMiDocumentoService descargarMiDocumentoService;

    @Test
    void descargar_devuelveElArchivoConSuTipo() {
        when(descargarMiDocumentoService.descargar(any(), any()))
                .thenReturn(new DescargarMiDocumentoResponse("cert.pdf", "application/pdf", "x".getBytes()));
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .get("/api/instructor/documentos/" + UUID.randomUUID() + "/archivo")
                        .principal(authentication)))
                .hasStatus(200)
                .hasHeader("Content-Type", "application/pdf");
    }
}
