package com.activehub.shared.storage;

import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige dónde se guardan los archivos: Supabase Storage si hay credenciales, disco si no.
 *
 * <p><b>La selección es por configuración presente, no por un flag.</b> Un
 * {@code app.storage.modo=supabase} sería una tercera cosa que puede quedar desincronizada de
 * las credenciales — encendido sin clave (todas las subidas fallan) o apagado con clave (el
 * despliegue escribe en un disco que se borra en el próximo reinicio, en silencio). Si están
 * la URL y la service key, es porque alguien las cargó para usarlas.
 *
 * <p>El modo elegido se loguea al arrancar: es la forma de darse cuenta de que un despliegue
 * quedó escribiendo en disco por una variable de entorno que no llegó.
 */
@Configuration
public class AlmacenamientoConfig {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoConfig.class);

    @Bean
    public AlmacenamientoArchivos almacenamientoArchivos(
            @Value("${app.storage.fotos-perfil-dir}") String fotosPerfilDir,
            @Value("${app.storage.fotos-actividad-dir}") String fotosActividadDir,
            @Value("${app.storage.documentos-instructor-dir}") String documentosInstructorDir,
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.service-key:}") String supabaseServiceKey,
            @Value("${app.storage.supabase.bucket:activehub}") String supabaseBucket
    ) {
        AlmacenamientoArchivos almacenamiento;
        if (!supabaseUrl.isBlank() && !supabaseServiceKey.isBlank()) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            almacenamiento = new AlmacenamientoSupabase(
                    httpClient, supabaseUrl, supabaseBucket, supabaseServiceKey);
        } else {
            almacenamiento = new AlmacenamientoDisco(
                    fotosPerfilDir, fotosActividadDir, documentosInstructorDir);
        }
        log.info("Almacenamiento de archivos: {}", almacenamiento.descripcion());
        return almacenamiento;
    }
}
