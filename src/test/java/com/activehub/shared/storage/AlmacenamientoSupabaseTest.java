package com.activehub.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El cliente HTTP se inyecta justamente para poder probar esto sin un proyecto de Supabase:
 * lo que se verifica es el contrato que se arma (método, URL, headers) y cómo se interpreta
 * cada respuesta, que es donde están las decisiones.
 */
@ExtendWith(MockitoExtension.class)
class AlmacenamientoSupabaseTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> respuestaTexto;
    @Mock
    private HttpResponse<byte[]> respuestaBytes;

    private AlmacenamientoSupabase almacenamiento;

    @BeforeEach
    void setUp() {
        almacenamiento = new AlmacenamientoSupabase(
                httpClient, "https://proyecto.supabase.co", "activehub", "service-key-de-prueba");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<HttpRequest> capturarPedido() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        return captor;
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardar_armaElPutConLaRutaDeLaCarpetaYElTokenDeServicio() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(respuestaTexto);
        when(respuestaTexto.statusCode()).thenReturn(200);

        almacenamiento.guardar(CarpetaArchivos.FOTOS_PERFIL, "foto.jpg", "contenido".getBytes(), "image/jpeg");

        HttpRequest pedido = capturarPedido().getValue();
        assertThat(pedido.method()).isEqualTo("PUT");
        assertThat(pedido.uri().toString())
                .isEqualTo("https://proyecto.supabase.co/storage/v1/object/activehub/fotos-perfil/foto.jpg");
        assertThat(pedido.headers().firstValue("Authorization")).contains("Bearer service-key-de-prueba");
        assertThat(pedido.headers().firstValue("Content-Type")).contains("image/jpeg");
        // Sin x-upsert, un reintento despues de un timeout falla con 409 por el archivo que la
        // llamada anterior si llego a escribir.
        assertThat(pedido.headers().firstValue("x-upsert")).contains("true");
    }

    /** La URL del proyecto se copia tanto con barra final como sin ella. */
    @Test
    @SuppressWarnings("unchecked")
    void guardar_conBarraFinalEnLaUrl_noDuplicaLaBarra() throws Exception {
        almacenamiento = new AlmacenamientoSupabase(
                httpClient, "https://proyecto.supabase.co/", "activehub", "service-key-de-prueba");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(respuestaTexto);
        when(respuestaTexto.statusCode()).thenReturn(200);

        almacenamiento.guardar(CarpetaArchivos.FOTOS_PERFIL, "foto.jpg", "contenido".getBytes(), "image/jpeg");

        assertThat(capturarPedido().getValue().uri().toString())
                .isEqualTo("https://proyecto.supabase.co/storage/v1/object/activehub/fotos-perfil/foto.jpg");
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardar_respuestaDeError_lanzaAlmacenamientoException() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(respuestaTexto);
        when(respuestaTexto.statusCode()).thenReturn(403);
        when(respuestaTexto.body()).thenReturn("{\"message\":\"new row violates row-level security policy\"}");

        assertThatThrownBy(() -> almacenamiento.guardar(
                CarpetaArchivos.FOTOS_PERFIL, "foto.jpg", "contenido".getBytes(), "image/jpeg"))
                .isInstanceOf(AlmacenamientoException.class)
                .hasMessageContaining("403");
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardar_sinConexion_lanzaAlmacenamientoException() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("connection reset"));

        assertThatThrownBy(() -> almacenamiento.guardar(
                CarpetaArchivos.FOTOS_PERFIL, "foto.jpg", "contenido".getBytes(), "image/jpeg"))
                .isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void leer_devuelveElContenidoDelBucket() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(respuestaBytes);
        when(respuestaBytes.statusCode()).thenReturn(200);
        when(respuestaBytes.body()).thenReturn("contenido".getBytes());

        byte[] contenido = almacenamiento.leer(CarpetaArchivos.FOTOS_ACTIVIDAD, "foto.jpg");

        assertThat(contenido).isEqualTo("contenido".getBytes());
        assertThat(capturarPedido().getValue().method()).isEqualTo("GET");
    }

    @Test
    @SuppressWarnings("unchecked")
    void leer_archivoInexistente_lanzaAlmacenamientoException() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(respuestaBytes);
        when(respuestaBytes.statusCode()).thenReturn(404);

        assertThatThrownBy(() -> almacenamiento.leer(CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, "dni.pdf"))
                .isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void borrarSiExiste_mandaDelete() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(respuestaTexto);
        when(respuestaTexto.statusCode()).thenReturn(200);

        almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_ACTIVIDAD, "vieja.jpg");

        HttpRequest pedido = capturarPedido().getValue();
        assertThat(pedido.method()).isEqualTo("DELETE");
        assertThat(pedido.uri().toString())
                .isEqualTo("https://proyecto.supabase.co/storage/v1/object/activehub/fotos-actividad/vieja.jpg");
    }

    /** El contrato de la interfaz: borrar es best-effort y no puede tumbar la operación. */
    @Test
    @SuppressWarnings("unchecked")
    void borrarSiExiste_sinConexion_noLanza() throws Exception {
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("connection reset"));

        assertThatCode(() -> almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_PERFIL, "vieja.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void descripcion_nombraElBucketYNoLaClave() {
        assertThat(almacenamiento.descripcion())
                .contains("activehub")
                .doesNotContain("service-key-de-prueba");
    }
}
