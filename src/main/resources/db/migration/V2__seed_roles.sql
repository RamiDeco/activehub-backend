INSERT INTO rol (id, nombre) VALUES
    (gen_random_uuid(), 'ALUMNO'),
    (gen_random_uuid(), 'INSTRUCTOR'),
    (gen_random_uuid(), 'ADMIN')
ON CONFLICT (nombre) DO NOTHING;
