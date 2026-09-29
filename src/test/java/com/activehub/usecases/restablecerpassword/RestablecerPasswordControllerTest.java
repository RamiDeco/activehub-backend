package com.activehub.usecases.restablecerpassword;

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

@WebMvcTest(controllers = RestablecerPasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RestablecerPasswordControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RestablecerPasswordService restablecerPasswordService;

    @Test
    void restablecer_datosValidos_devuelve200ConElCorreo() {
        when(restablecerPasswordService.restablecer(any()))
                .thenReturn(new RestablecerPasswordResponse("martina@email.com"));

        assertThat(mvc.post().uri("/api/auth/recuperar-password/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"martina@email.com\",\"codigo\":\"123456\","
                                + "\"contraseniaNueva\":\"Segura123\"}"))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.email").isEqualTo("martina@email.com");
    }

    @Test
    void restablecer_codigoQueNoTiene6Digitos_devuelve400() {
        assertThat(mvc.post().uri("/api/auth/recuperar-password/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"martina@email.com\",\"codigo\":\"12\","
                                + "\"contraseniaNueva\":\"Segura123\"}"))
                .hasStatus(400);
    }

    /** RN-20: la misma política del registro y del cambio de contraseña con sesión. */
    @Test
    void restablecer_contraseniaDebil_devuelve400() {
        assertThat(mvc.post().uri("/api/auth/recuperar-password/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"martina@email.com\",\"codigo\":\"123456\","
                                + "\"contraseniaNueva\":\"password1\"}"))
                .hasStatus(400);
    }
}
