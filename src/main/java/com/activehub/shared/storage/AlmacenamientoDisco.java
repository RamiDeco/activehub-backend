package com.activehub.shared.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * El filesystem del backend: lo que hubo desde siempre, y el modo por defecto.
 *
 * <p>Cada carpeta tiene su ruta propia configurada ({@code app.storage.*-dir}) en vez de
 * derivarse de una raíz común, porque así estaba y hay instalaciones con los tres directorios
 * en lugares distintos.
 *
 * <p><b>Es efímero en la nube.</b> En Render el disco del contenedor se pierde en cada
 * despliegue y en cada reinicio: las fotos subidas desaparecen aunque las filas de la base
 * sigan apuntándolas. Es exactamente el motivo por el que existe {@link AlmacenamientoSupabase}
 * y por el que el despliegue tiene que usarlo.
 */
public class AlmacenamientoDisco implements AlmacenamientoArchivos {

    private final Map<CarpetaArchivos, Path> directorios = new EnumMap<>(CarpetaArchivos.class);

    public AlmacenamientoDisco(String fotosPerfilDir, String fotosActividadDir, String documentosInstructorDir) {
        directorios.put(CarpetaArchivos.FOTOS_PERFIL, Path.of(fotosPerfilDir));
        directorios.put(CarpetaArchivos.FOTOS_ACTIVIDAD, Path.of(fotosActividadDir));
        directorios.put(CarpetaArchivos.DOCUMENTOS_INSTRUCTOR, Path.of(documentosInstructorDir));
    }

    @Override
    public void guardar(CarpetaArchivos carpeta, String nombre, byte[] contenido, String tipoContenido) {
        // El tipo de contenido no se usa acá: en disco el que manda es la extensión, y es de
        // donde lo deducen los endpoints que sirven el archivo.
        Path directorio = directorios.get(carpeta);
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), contenido);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo escribir " + nombre + " en " + directorio, e);
        }
    }

    @Override
    public byte[] leer(CarpetaArchivos carpeta, String nombre) {
        try {
            return Files.readAllBytes(directorios.get(carpeta).resolve(nombre));
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo leer " + nombre, e);
        }
    }

    @Override
    public void borrarSiExiste(CarpetaArchivos carpeta, String nombre) {
        try {
            Files.deleteIfExists(directorios.get(carpeta).resolve(nombre));
        } catch (IOException e) {
            // best-effort a propósito: ver el contrato de la interfaz.
        }
    }

    @Override
    public String descripcion() {
        return "disco local (" + directorios.get(CarpetaArchivos.FOTOS_PERFIL).toAbsolutePath() + ", …)";
    }
}
