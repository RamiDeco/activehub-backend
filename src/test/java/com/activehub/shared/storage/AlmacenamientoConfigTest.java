package com.activehub.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * La elección de destino es la parte que se rompe en silencio: un despliegue al que no le
 * llegó una variable de entorno sigue arrancando y escribe en un disco que se borra en el
 * próximo reinicio. Estos casos fijan exactamente cuándo se usa cada implementación.
 */
class AlmacenamientoConfigTest {

    private final AlmacenamientoConfig config = new AlmacenamientoConfig();

    private AlmacenamientoArchivos construir(String url, String serviceKey) {
        return config.almacenamientoArchivos(
                "./uploads/fotos-perfil",
                "./uploads/fotos-actividad",
                "./uploads/documentos-instructor",
                url,
                serviceKey,
                "activehub");
    }

    @Test
    void conUrlYServiceKey_usaSupabaseStorage() {
        assertThat(construir("https://proyecto.supabase.co", "service-key-de-prueba"))
                .isInstanceOf(AlmacenamientoSupabase.class);
    }

    @Test
    void sinCredenciales_usaElDisco() {
        assertThat(construir("", "")).isInstanceOf(AlmacenamientoDisco.class);
    }

    /**
     * Media configuración es disco, no Supabase a medias: con la URL cargada y sin clave todas
     * las subidas fallarían, y es peor que seguir guardando donde se venía guardando.
     */
    @Test
    void conUrlPeroSinServiceKey_usaElDisco() {
        assertThat(construir("https://proyecto.supabase.co", "")).isInstanceOf(AlmacenamientoDisco.class);
    }

    @Test
    void conServiceKeyPeroSinUrl_usaElDisco() {
        assertThat(construir("", "service-key-de-prueba")).isInstanceOf(AlmacenamientoDisco.class);
    }
}
