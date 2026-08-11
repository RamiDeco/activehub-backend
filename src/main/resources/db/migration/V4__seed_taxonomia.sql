INSERT INTO categoria (id, nombre) VALUES
    (gen_random_uuid(), 'Bienestar'),
    (gen_random_uuid(), 'Aventura'),
    (gen_random_uuid(), 'Formación Técnica'),
    (gen_random_uuid(), 'Defensa Personal')
ON CONFLICT DO NOTHING;

INSERT INTO tipo_actividad (id, nombre, categoria_id)
SELECT gen_random_uuid(), v.nombre, c.id
FROM (VALUES
    ('Relajación', 'Bienestar'),
    ('Bienestar', 'Bienestar'),
    ('Aventura', 'Aventura'),
    ('Formativa', 'Formación Técnica'),
    ('Técnica', 'Formación Técnica'),
    ('Defensa Personal', 'Defensa Personal')
) AS v(nombre, categoria_nombre)
JOIN categoria c ON c.nombre = v.categoria_nombre
ON CONFLICT DO NOTHING;
