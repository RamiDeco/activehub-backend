package com.activehub.usecases.listarmissanciones;

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

@WebMvcTest(controllers = ListarMisSancionesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarMisSancionesControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarMisSancionesService service;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    @Test
    void listar_devuelve200ConLasSanciones() {
        when(service.listar(any())).thenReturn(new ListarMisSancionesResponse(
                true, null, new ListarMisSancionesResponse.Transferencia("activehub.multas", null, null), List.of()));

        assertThat(mvc.perform(MockMvcRequestBuilders.get("/api/instructor/sanciones").principal(authentication)))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.multaPendiente").isEqualTo(true);
    }
}
