package com.activehub.usecases.registrarinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.ValidacionException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * El alta de instructor es multipart: parte "datos" (JSON) + parte "documentos" (archivos),
 * en el mismo request (E1A-HU04 criterio 9 / RN-12).
 */
@WebMvcTest(controllers = RegistrarInstructorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegistrarInstructorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegistrarInstructorService registrarInstructorService;

    private static final String DATOS_VALIDOS = """
            {
              "nombre": "Mateo",
              "apellido": "Ríos",
              "email": "mateo@email.com",
              "telefono": "2611234567",
              "password": "Password1",
              "fechaNacimiento": "1990-04-02",
              "especialidad": "Running",
              "aniosExperiencia": 6,
              "descripcion": "Entrenador de running",
              "aceptaTerminos": true
            }
            """;

    private MockMultipartFile datos(String json) {
        return new MockMultipartFile(
                "datos", "datos.json", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile documento() {
        return new MockMultipartFile("documentos", "cert.pdf", "application/pdf", "contenido".getBytes());
    }

    private RegistrarInstructorResponse respuestaOk() {
        var usuario = new RegistrarInstructorResponse.Usuario(
                UUID.randomUUID(), "Mateo", "Ríos", "mateo@email.com", "2611234567",
                LocalDate.of(1990, 4, 2), "INSTRUCTOR", "ACTIVO", 0, Instant.now(), false, "LOCAL");
        return new RegistrarInstructorResponse("token-jwt", true, usuario);
    }

    @Test
    void registrar_multipartValido_devuelve201ConTokenYUsuario() {
        when(registrarInstructorService.registrar(any(), any())).thenReturn(respuestaOk());

        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/auth/registro/instructor")
                        .file(datos(DATOS_VALIDOS))
                        .file(documento())))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.token").isEqualTo("token-jwt");
    }

    @Test
    void registrar_multipartValido_devuelveElUsuarioConRolInstructor() {
        when(registrarInstructorService.registrar(any(), any())).thenReturn(respuestaOk());

        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/auth/registro/instructor")
                        .file(datos(DATOS_VALIDOS))
                        .file(documento())))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.usuario.rol").isEqualTo("INSTRUCTOR");
    }

    @Test
    void registrar_passwordSinMayuscula_devuelve400SinLlamarAlService() {
        // RN-20: 8 caracteres, una mayúscula y un número. "password1" pasaba antes.
        String flojo = DATOS_VALIDOS.replace("Password1", "password1");

        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/auth/registro/instructor")
                        .file(datos(flojo))
                        .file(documento())))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");

        verify(registrarInstructorService, never()).registrar(any(), any());
    }

    @Test
    void registrar_sinAceptarTerminos_devuelve400() {
        String sinAceptar = DATOS_VALIDOS.replace("\"aceptaTerminos\": true", "\"aceptaTerminos\": false");

        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/auth/registro/instructor")
                        .file(datos(sinAceptar))
                        .file(documento())))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");
    }

    @Test
    void registrar_sinLaParteDocumentos_devuelve400YNoCreaNada() {
        // Sin handler propio, una parte faltante caía en el catch-all y devolvía 500:
        // un pedido incompleto del cliente se leía como falla del servidor.
        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/auth/registro/instructor")
                        .file(datos(DATOS_VALIDOS))))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("VALIDACION");

        verify(registrarInstructorService, never()).registrar(any(), any());
    }

    @Test
    void registrar_documentacionRechazadaPorElService_devuelve400ConElMensaje() {
        when(registrarInstructorService.registrar(any(), any()))
                .thenThrow(new ValidacionException("Formato no admitido. Usá PDF, JPG o PNG."));

        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/auth/registro/instructor")
                        .file(datos(DATOS_VALIDOS))
                        .file(new MockMultipartFile("documentos", "virus.exe", "application/x-msdownload", new byte[]{1}))))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message").asString().contains("PDF, JPG o PNG");
    }
}
