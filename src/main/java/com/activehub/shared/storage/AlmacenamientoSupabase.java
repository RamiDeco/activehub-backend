package com.activehub.shared.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supabase Storage por su API REST.
 *
 * <h2>Por qué a mano con {@link HttpClient} y no con un SDK</h2>
 *
 * El cliente oficial de Supabase para Java no existe; los que hay son de terceros y arrastran
 * su propio cliente HTTP y su propio JSON. Acá se usan <b>tres</b> llamadas (subir, bajar,
 * borrar) que son un PUT, un GET y un DELETE con un header de autorización, y ninguna devuelve
 * un cuerpo que haya que parsear. Es el mismo criterio con el que {@code GoogleIdTokenVerifier}
 * valida el token a mano en vez de sumar {@code google-api-client}.
 *
 * <h2>El bucket es PRIVADO y los archivos se sirven por nuestra API</h2>
 *
 * No se generan URLs públicas ni firmadas, y en la base no se guarda ninguna URL: el frontend
 * sigue pidiendo {@code /api/fotos/perfil/{id}} y el backend baja el archivo y lo devuelve.
 *
 * <ul>
 *   <li><b>La documentación del instructor no puede ser pública.</b> Son DNI y certificados de
 *       terceros; en un bucket público, adivinar o filtrar la URL alcanza para leerlos, sin
 *       pasar por el permiso que hoy los protege.</li>
 *   <li>Para las fotos, mantener el mismo endpoint significa que el cambio de destino <b>no
 *       toca ni una línea del frontend</b> ni ninguna fila de la base.</li>
 * </ul>
 *
 * <p>El costo es que cada imagen viaja dos veces (Supabase → backend → navegador). Con el
 * {@code Cache-Control} de una hora que ya mandan esos endpoints y el tamaño del catálogo
 * actual es despreciable; si algún día deja de serlo, el paso siguiente son URLs firmadas de
 * corta duración para las fotos —nunca para los documentos—, y eso sí cambia el contrato con
 * el frontend.
 *
 * <p>La clave que usa es la <b>service key</b> del proyecto, que saltea las políticas RLS y
 * por eso sólo puede vivir en el backend: nunca en el frontend ni en el repositorio.
 */
public class AlmacenamientoSupabase implements AlmacenamientoArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoSupabase.class);

    private final HttpClient httpClient;
    private final String urlBase;
    private final String bucket;
    private final String serviceKey;

    public AlmacenamientoSupabase(HttpClient httpClient, String url, String bucket, String serviceKey) {
        this.httpClient = httpClient;
        // Sin la barra final duplicada: la URL del proyecto se suele copiar con y sin ella.
        this.urlBase = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.bucket = bucket;
        this.serviceKey = serviceKey;
    }

    @Override
    public void guardar(CarpetaArchivos carpeta, String nombre, byte[] contenido, String tipoContenido) {
        HttpRequest request = autorizada(carpeta, nombre)
                // x-upsert: el nombre es un UUID nuevo, así que no debería haber nada que
                // pisar; sin el header, un reintento después de un timeout fallaría con 409
                // por un archivo que la llamada anterior sí llegó a escribir.
                .header("x-upsert", "true")
                .header("Content-Type", tipoContenido != null ? tipoContenido : "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(contenido))
                .build();

        HttpResponse<String> response = enviar(request, HttpResponse.BodyHandlers.ofString(),
                "guardar " + nombre);
        if (!esExitosa(response.statusCode())) {
            throw new AlmacenamientoException(
                    "Supabase Storage respondió " + response.statusCode() + " al guardar " + nombre
                            + ": " + response.body());
        }
    }

    @Override
    public byte[] leer(CarpetaArchivos carpeta, String nombre) {
        HttpRequest request = autorizada(carpeta, nombre).GET().build();

        HttpResponse<byte[]> response = enviar(request, HttpResponse.BodyHandlers.ofByteArray(),
                "leer " + nombre);
        if (!esExitosa(response.statusCode())) {
            throw new AlmacenamientoException(
                    "Supabase Storage respondió " + response.statusCode() + " al leer " + nombre);
        }
        return response.body();
    }

    @Override
    public void borrarSiExiste(CarpetaArchivos carpeta, String nombre) {
        try {
            HttpRequest request = autorizada(carpeta, nombre).DELETE().build();
            HttpResponse<String> response = enviar(request, HttpResponse.BodyHandlers.ofString(),
                    "borrar " + nombre);
            // Un 404 es el caso "ya no estaba", que para este método es éxito.
            if (!esExitosa(response.statusCode()) && response.statusCode() != 404) {
                log.warn("Supabase Storage respondió {} al borrar {}", response.statusCode(), nombre);
            }
        } catch (RuntimeException e) {
            // Nunca lanza: ver el contrato de la interfaz.
            log.warn("No se pudo borrar {} de Supabase Storage", nombre, e);
        }
    }

    @Override
    public String descripcion() {
        return "Supabase Storage (bucket " + bucket + ")";
    }

    private HttpRequest.Builder autorizada(CarpetaArchivos carpeta, String nombre) {
        return HttpRequest.newBuilder(URI.create(objetoUrl(carpeta, nombre)))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + serviceKey);
    }

    /**
     * {@code /storage/v1/object/{bucket}/{carpeta}/{nombre}}. El nombre siempre es
     * {@code <uuid>.<ext>}, generado por la aplicación: no hay nada que escapar, y por eso no
     * se codifica — un nombre de archivo que viniera del usuario sí habría que tratarlo.
     */
    private String objetoUrl(CarpetaArchivos carpeta, String nombre) {
        return urlBase + "/storage/v1/object/" + bucket + "/" + carpeta.prefijo() + "/" + nombre;
    }

    private <T> HttpResponse<T> enviar(HttpRequest request, HttpResponse.BodyHandler<T> handler, String queHacia) {
        try {
            return httpClient.send(request, handler);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo contactar a Supabase Storage para " + queHacia, e);
        } catch (InterruptedException e) {
            // Restaurar el flag es obligatorio: tragarlo deja al hilo sin saber que lo
            // interrumpieron y el pool nunca lo recicla.
            Thread.currentThread().interrupt();
            throw new AlmacenamientoException("Interrumpido al " + queHacia, e);
        }
    }

    private boolean esExitosa(int status) {
        return status >= 200 && status < 300;
    }

}
