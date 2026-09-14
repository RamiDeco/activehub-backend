package com.activehub.usecases.subirdocumento;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = SubirDocumentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SubirDocumentoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SubirDocumentoService subirDocumentoService;

    @Test
    void subir_archivoValido_devuelve201() {
        when(subirDocumentoService.subir(any(), any()))
                .thenReturn(new SubirDocumentoResponse(UUID.randomUUID(), "cert.pdf", "application/pdf", 9L));

        MockMultipartFile archivo =
                new MockMultipartFile("archivo", "cert.pdf", "application/pdf", "contenido".getBytes());
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/instructor/documentos")
                        .file(archivo)
                        .principal(authentication)))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.nombreArchivo").isEqualTo("cert.pdf");
    }
}
