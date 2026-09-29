package com.activehub.usecases.listarreportessoporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.soporte.EstadoReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporteRepository;
import com.activehub.domain.usuario.Usuario;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarReportesSoporteServiceTest {

    @Mock
    private ReporteSoporteRepository reporteSoporteRepository;

    private ListarReportesSoporteService service;

    @BeforeEach
    void setUp() {
        service = new ListarReportesSoporteService(reporteSoporteRepository);
    }

    private static Usuario usuario(String nombre, String apellido) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        u.setNombre(nombre);
        u.setApellido(apellido);
        return u;
    }

    private static ReporteSoporte reporte(Usuario autor) {
        ReporteSoporte r = new ReporteSoporte();
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        r.setEmail("ana@mail.com");
        r.setAsunto("No puedo pagar");
        r.setDetalle("Me tira error.");
        r.setUsuario(autor);
        return r;
    }

    @Test
    void listar_resuelveElNombreDelAutor() {
        when(reporteSoporteRepository.findAllConDetalle())
                .thenReturn(List.of(reporte(usuario("Ana", "Lopez"))));

        var r = service.listar().get(0);

        assertThat(r.autorNombre()).isEqualTo("Ana Lopez");
        assertThat(r.autorId()).isNotNull();
        assertThat(r.estado()).isEqualTo("Abierto");
    }

    /**
     * Un reporte anonimo devuelve {@code autorNombre} en null, no la cadena "Anónimo": la
     * etiqueta que ve el administrador la elige la pantalla, no el backend.
     */
    @Test
    void listar_reporteAnonimo_devuelveAutorEnNull() {
        when(reporteSoporteRepository.findAllConDetalle()).thenReturn(List.of(reporte(null)));

        var r = service.listar().get(0);

        assertThat(r.autorNombre()).isNull();
        assertThat(r.autorId()).isNull();
        assertThat(r.email()).isEqualTo("ana@mail.com");
    }

    @Test
    void listar_reporteCerrado_devuelveQuienLoCerroYLaRespuesta() {
        ReporteSoporte cerrado = reporte(null);
        cerrado.setEstado(EstadoReporteSoporte.CERRADO);
        cerrado.setRespuesta("Ya está resuelto.");
        cerrado.setCerradoPor(usuario("Admin", "Root"));
        when(reporteSoporteRepository.findAllConDetalle()).thenReturn(List.of(cerrado));

        var r = service.listar().get(0);

        assertThat(r.estado()).isEqualTo("Cerrado");
        assertThat(r.respuesta()).isEqualTo("Ya está resuelto.");
        assertThat(r.cerradoPorNombre()).isEqualTo("Admin Root");
    }
}
