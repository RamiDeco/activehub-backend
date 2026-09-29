package com.activehub.shared.storage;

/**
 * Dónde viven los archivos subidos. Es el mismo tipo de costura que {@code PaymentGateway}
 * para los pagos: una interfaz estable detrás de la cual hay dos implementaciones, y ningún
 * caso de uso sabe cuál está activa.
 *
 * <ul>
 *   <li>{@link AlmacenamientoDisco} — el filesystem del backend, que es lo que había hasta
 *       ahora. Sigue siendo el modo por defecto y el de desarrollo.</li>
 *   <li>{@link AlmacenamientoSupabase} — un bucket de Supabase Storage. Se activa solo cuando
 *       hay credenciales cargadas ({@link AlmacenamientoConfig}).</li>
 * </ul>
 *
 * <h2>Lo que se guarda en la base es SÓLO el nombre del archivo</h2>
 *
 * {@code usuario.foto_path}, {@code actividad.foto_path}, {@code actividad_imagen.path} y
 * {@code documento_instructor.ruta_archivo} guardan un nombre suelto ({@code <uuid>.jpg}), no
 * una ruta ni una URL. Eso es lo que permite cambiar de destino sin migrar ni una fila, y es
 * la razón por la que estos métodos reciben la {@link CarpetaArchivos} aparte: el destino
 * completo lo arma la implementación. <b>No guardar URLs en la base</b> — una URL de Supabase
 * ata las filas a un proyecto concreto y deja de resolver el día que el bucket cambie.
 */
public interface AlmacenamientoArchivos {

    /**
     * Escribe el archivo, pisándolo si ya existiera. El nombre lo genera quien llama (siempre
     * un UUID + la extensión original), así que en la práctica nunca hay colisión.
     *
     * @throws AlmacenamientoException si no se pudo escribir.
     */
    void guardar(CarpetaArchivos carpeta, String nombre, byte[] contenido, String tipoContenido);

    /**
     * @return el contenido del archivo.
     * @throws AlmacenamientoException si no existe o no se pudo leer.
     */
    byte[] leer(CarpetaArchivos carpeta, String nombre);

    /**
     * Borra el archivo si está. <b>Nunca lanza</b>: se llama para limpiar una foto vieja que ya
     * fue reemplazada o un archivo huérfano de una transacción que hizo rollback, y en los dos
     * casos ya no hay a quién reportarle el problema — fallar ahí convertiría una operación
     * exitosa en un error para el usuario.
     */
    void borrarSiExiste(CarpetaArchivos carpeta, String nombre);

    /** Dónde está guardando, para el log de arranque. No incluye credenciales. */
    String descripcion();
}
