package com.activehub.shared.ia;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige qué modelo de lenguaje queda activo: Groq si hay clave, nada si no.
 *
 * <p><b>La selección es por credencial presente, no por un flag</b>, igual que en
 * {@code AlmacenamientoConfig}: un {@code app.ia.habilitado} sería una tercera cosa que puede
 * quedar desincronizada de la clave (encendido sin clave = todas las consultas fallan; apagado con
 * clave = la IA no se usa y nadie sabe por qué). Si la clave está cargada, es porque alguien la
 * cargó para usarla.
 *
 * <p>El modo elegido se loguea al arrancar: es la única forma de darse cuenta de que un despliegue
 * quedó sin IA por una variable de entorno que no llegó.
 */
@Configuration
public class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    @Bean
    public ModeloLenguaje modeloLenguaje(
            @Value("${app.ia.groq.api-key:}") String apiKey,
            @Value("${app.ia.groq.url}") String url,
            @Value("${app.ia.groq.modelo}") String modelo,
            @Value("${app.ia.groq.reasoning-effort:}") String razonamiento,
            @Value("${app.ia.groq.timeout-seg}") int timeoutSeg
    ) {
        ModeloLenguaje modeloLenguaje = apiKey.isBlank()
                ? new ModeloLenguajeNoConfigurado()
                : new ModeloLenguajeGroq(url, apiKey, modelo, razonamiento, Duration.ofSeconds(timeoutSeg));
        log.info("Modelo de lenguaje: {}", modeloLenguaje.descripcion());
        return modeloLenguaje;
    }
}
