package com.activehub.shared.storage;

/**
 * Los tres tipos de archivo que guarda la plataforma. Cada uno tiene su propio destino, y esa
 * separación es la que permite que las fotos sean públicas de hecho (cualquiera ve el catálogo)
 * y la documentación del instructor no lo sea nunca.
 *
 * <p>El valor del enum decide el directorio en disco y el prefijo de la ruta dentro del bucket
 * de Supabase Storage, así que cambiarle el prefijo a uno ya usado deja huérfanos los archivos
 * viejos: la ruta está guardada en la base sólo como nombre, y el prefijo se antepone al leer.
 */
public enum CarpetaArchivos {

    /** Foto de perfil del usuario ({@code usuario.foto_path}). */
    FOTOS_PERFIL("fotos-perfil"),

    /** Portada y galería de la actividad ({@code actividad.foto_path}, {@code actividad_imagen.path}). */
    FOTOS_ACTIVIDAD("fotos-actividad"),

    /** Documentación que respalda la solicitud del instructor ({@code documento_instructor.ruta_archivo}). */
    DOCUMENTOS_INSTRUCTOR("documentos-instructor");

    private final String prefijo;

    CarpetaArchivos(String prefijo) {
        this.prefijo = prefijo;
    }

    /** Carpeta dentro del bucket. En disco no se usa: ahí cada carpeta tiene su propia ruta configurada. */
    public String prefijo() {
        return prefijo;
    }
}
