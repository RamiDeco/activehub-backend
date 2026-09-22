package com.activehub.usecases.solicitarrecuperacionpassword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = SolicitarRecuperacionPasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SolicitarRecuperacionPasswordControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SolicitarRecuperacionPasswordService solicitarRecuperacionPasswordService;

    @Test
    void solicitar_correoValido_devuelve200ConElTtl() {
        when(solicitarRecuperacionPasswordService.solicitar(any()))
                .thenReturn(new SolicitarRecuperacionPasswordResponse(true, 15));

        assertThat(mvc.post().uri("/api/auth/recuperar-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"martina@email.com\"}"))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.ttlMin").isEqualTo(15);
    }

    @Test
    void solicitar_correoConFormatoInvalido_devuelve400() {
        assertThat(mvc.post().uri("/api/auth/recuperar-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-correo\"}"))
                .hasStatus(400);
    }
}
