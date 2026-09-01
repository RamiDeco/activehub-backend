-- Alumno: breve descripción de enfermedad/lesión, opcional.
ALTER TABLE perfil_alumno ADD COLUMN condicion_salud TEXT;

-- documento_instructor: pasa de "url externa preparada a futuro" a archivo
-- real guardado por el backend. Se renombra la columna para que el nombre
-- refleje lo que realmente contiene (una ruta local, no una URL pública) y
-- se agregan nombre original + tamaño para poder listarlo bien en el admin.
ALTER TABLE documento_instructor RENAME COLUMN url_archivo TO ruta_archivo;
ALTER TABLE documento_instructor ADD COLUMN nombre_archivo VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE documento_instructor ADD COLUMN tamanio_bytes BIGINT NOT NULL DEFAULT 0;
