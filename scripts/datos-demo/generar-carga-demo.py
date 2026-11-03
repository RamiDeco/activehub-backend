"""Genera 03-limpieza-y-carga-demo.sql (datos de demostracion, ver LEEME.txt).

Uso: python generar-carga-demo.py <hash_bcrypt> 03-limpieza-y-carga-demo.sql
El hash se recibe por argumento: la contrasenia en texto plano no pasa por este archivo.
Para obtenerlo (paquete bcrypt de Python; Spring acepta el prefijo $2a$):
    python -c "import bcrypt,getpass; print(bcrypt.hashpw(getpass.getpass().encode(), bcrypt.gensalt(10)).decode().replace('$2b$','$2a$',1))"
Los datos salen siempre iguales (azar con semilla fija).
"""
import random
import sys
import unicodedata
import uuid

HASH, SALIDA = sys.argv[1], sys.argv[2]
NS = uuid.UUID("6b3d2a40-8c1f-4b7e-9a51-2f0c6d1e9a01")
rnd = random.Random(20261028)
TZ = "America/Argentina/Buenos_Aires"


def uid(clave):
    return str(uuid.uuid5(NS, clave))


def q(s):
    return "NULL" if s is None else "'" + s.replace("'", "''") + "'"


def dia(d, hora="12:00"):
    """Fecha y hora en Argentina, d dias desde hoy (negativo = pasado)."""
    return f"pg_temp.dia({d}, '{hora}')"


def sin_tildes(s):
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


# --------------------------------------------------------------------------- taxonomia
CATEGORIAS = [
    ("Entrenamiento de Fuerza y Resistencia", ["Trabajo con pesas", "Trabajo con cargas livianas", "Ejercicios de resistencia corporal", "Funcional"]),
    ("Actividad Aeróbica", ["Running", "Nadar", "Andar en bicicleta", "Bailar"]),
    ("Actividades de Flexibilidad", ["Estiramientos", "Yoga", "Pilates"]),
    ("Artes Marciales", ["Tai Chi", "Defensa Personal"]),
    ("Aventura", ["Senderismo", "Trekking", "Mountain Bike", "Escalada"]),
    ("Bienestar", ["Relajación"]),
]
TIPO = {t: uid("tipo:" + t) for _, ts in CATEGORIAS for t in ts}

# --------------------------------------------------------------------------- personas
INSTRUCTORES = [
    ("lucia", "Lucía", "Fernández", "Entrenamiento funcional y de fuerza", 8,
     "Profesora de Educación Física. Trabajo la fuerza de forma progresiva y adaptada a cada persona, sin importar desde dónde arranque."),
    ("martin", "Martín", "Gómez", "Running recreativo y ciclismo", 6,
     "Entrenador de running y guía de salidas en bicicleta. Me gusta que cada uno vaya a su ritmo y disfrute del recorrido."),
    ("valentina", "Valentina", "Ruiz", "Yoga, pilates y bienestar", 10,
     "Instructora de yoga y pilates. Mis clases buscan movilidad, buena postura y un rato para desconectar."),
    ("diego", "Diego", "Morales", "Senderismo y actividades de montaña", 12,
     "Guía de montaña. Organizo salidas seguras por la precordillera mendocina, pensadas para disfrutar del paisaje."),
    ("carolina", "Carolina", "Sosa", "Natación, baile y tai chi", 7,
     "Profesora de natación y de danza. También doy tai chi al aire libre para quienes buscan algo tranquilo."),
]

ALUMNOS = [
    ("sofia", "Sofía", "Álvarez", ["Yoga", "Pilates", "Relajación"], None),
    ("juan", "Juan", "Pérez", ["Running", "Andar en bicicleta", "Funcional"], None),
    ("camila", "Camila", "Romero", ["Bailar", "Yoga", "Estiramientos"], None),
    ("tomas", "Tomás", "Herrera", ["Mountain Bike", "Escalada", "Trekking"], None),
    ("agustina", "Agustina", "Díaz", ["Senderismo", "Trekking", "Tai Chi"], None),
    ("nicolas", "Nicolás", "Castro", ["Trabajo con pesas", "Ejercicios de resistencia corporal", "Running"], None),
    ("florencia", "Florencia", "Molina", ["Pilates", "Nadar", "Trabajo con cargas livianas"], "Lesión de rodilla operada hace dos años."),
    ("mateo", "Mateo", "Torres", ["Defensa Personal", "Funcional", "Escalada"], None),
    ("julieta", "Julieta", "Medina", ["Nadar", "Bailar", "Relajación"], None),
    ("federico", "Federico", "Rojas", ["Andar en bicicleta", "Senderismo", "Mountain Bike"], None),
    ("micaela", "Micaela", "Suárez", ["Estiramientos", "Tai Chi", "Yoga"], "Asma leve, controlada."),
    ("lautaro", "Lautaro", "Benítez", ["Trabajo con pesas", "Defensa Personal", "Running"], None),
]

# --------------------------------------------------------------------------- actividades
# (clave, instructor, nombre, tipo, nivel, precio, duracion, cupo, hora, ubicacion, lat, lng, descripcion)
ACTIVIDADES = [
    ("pesas", "lucia", "Pesas para principiantes", "Trabajo con pesas", "Física media", 9000, 60, 10, "19:00",
     "Gimnasio al aire libre del Parque General San Martín, Mendoza", -32.8905, -68.8740,
     "Aprendé la técnica de los ejercicios básicos con mancuernas y barra, con cargas que se ajustan a tu nivel. Grupos chicos para poder corregir a cada uno."),
    ("cargas", "lucia", "Tonificación con cargas livianas", "Trabajo con cargas livianas", "Física baja", 7000, 50, 14, "10:00",
     "Plaza España, Ciudad de Mendoza", -32.8939, -68.8414,
     "Circuito suave con bandas elásticas y mancuernas livianas para ganar fuerza y mejorar la postura. Ideal para retomar la actividad física."),
    ("funcional", "lucia", "Funcional en el parque", "Funcional", "Física media", 8000, 60, 16, "08:00",
     "Rosedal del Parque General San Martín, Mendoza", -32.8925, -68.8680,
     "Entrenamiento funcional al aire libre: movimientos de la vida diaria, coordinación y equilibrio. Cada ejercicio tiene una versión más simple y una más exigente."),
    ("defensa", "lucia", "Defensa personal para el día a día", "Defensa Personal", "Física media", 8500, 75, 12, "20:00",
     "Polideportivo municipal, Godoy Cruz", -32.9250, -68.8440,
     "Técnicas simples de prevención y de respuesta ante situaciones de riesgo, practicadas con cuidado y sin contacto fuerte. No hace falta experiencia previa."),
    ("running", "martin", "Running recreativo en grupo", "Running", "Física media", 6000, 60, 20, "07:30",
     "Lago del Parque General San Martín, Mendoza", -32.8880, -68.8810,
     "Salidas a correr en grupo alrededor del lago, alternando trote y caminata. Cada persona va a su ritmo: lo importante es sumar kilómetros y pasarla bien."),
    ("bici", "martin", "Bicicleteada por los caminos de Maipú", "Andar en bicicleta", "Física media", 9500, 120, 15, "09:30",
     "Plaza departamental de Maipú, Mendoza", -32.9830, -68.7860,
     "Paseo en bicicleta por caminos rurales entre viñedos, a ritmo tranquilo y con paradas. Se puede ir con bicicleta propia; el recorrido es casi todo llano."),
    ("calistenia", "martin", "Resistencia con el propio cuerpo", "Ejercicios de resistencia corporal", "Física alta", 7500, 60, 14, "18:30",
     "Parque Central, Ciudad de Mendoza", -32.8790, -68.8380,
     "Sentadillas, flexiones, planchas y saltos en series cortas. Es la clase más intensa de la semana, con pausas para recuperar el aire."),
    ("yoga", "valentina", "Yoga al amanecer", "Yoga", "Física baja", 7000, 60, 15, "07:00",
     "Plaza de Chacras de Coria, Luján de Cuyo", -32.9890, -68.8740,
     "Secuencias suaves de yoga para empezar el día con energía: respiración, posturas básicas y un momento final de relajación. Traé tu mat."),
    ("pilates", "valentina", "Pilates en colchoneta", "Pilates", "Física baja", 8000, 55, 12, "18:00",
     "Sala de usos múltiples del Centro Cultural, Ciudad de Mendoza", -32.8870, -68.8450,
     "Pilates mat para fortalecer el centro del cuerpo, mejorar la postura y ganar flexibilidad. Ejercicios controlados, apto para todo público."),
    ("estiramientos", "valentina", "Estiramientos y movilidad", "Estiramientos", "Física baja", 5500, 45, 16, "19:30",
     "Sala de usos múltiples del Centro Cultural, Ciudad de Mendoza", -32.8870, -68.8450,
     "Una clase corta para soltar tensiones de la semana: estiramientos guiados y ejercicios de movilidad articular. Ideal para complementar otra actividad."),
    ("relajacion", "valentina", "Relajación y respiración consciente", "Relajación", "Física baja", 5000, 60, 15, "20:30",
     "Plaza de Chacras de Coria, Luján de Cuyo", -32.9890, -68.8740,
     "Técnicas de respiración y relajación guiada para bajar el estrés. Se practica sentado o acostado, sin exigencia física."),
    ("senderismo", "diego", "Senderismo al Cerro Arco", "Senderismo", "Física media", 10000, 180, 15, "08:30",
     "Base del Cerro Arco, El Challao, Las Heras", -32.8580, -68.8930,
     "Caminata por senderos marcados hasta el mirador, con vista a toda la ciudad. Se avanza a ritmo de grupo y con paradas para hidratarse."),
    ("trekking", "diego", "Trekking en Potrerillos", "Trekking", "Física alta", 18000, 300, 12, "07:00",
     "Villa de Potrerillos, Luján de Cuyo", -32.9600, -69.1960,
     "Jornada de trekking por quebradas y lomas de la precordillera. Requiere buena condición física y calzado de montaña; incluye charla de seguridad antes de salir."),
    ("mtb", "diego", "Mountain bike en El Challao", "Mountain Bike", "Física alta", 12000, 150, 10, "09:00",
     "Parque de El Challao, Las Heras", -32.8620, -68.8790,
     "Recorrido en mountain bike por senderos de tierra con subidas y bajadas. Se repasa la técnica de frenado y manejo antes de cada tramo."),
    ("escalada", "diego", "Escalada para principiantes en palestra", "Escalada", "Física media", 11000, 90, 8, "17:00",
     "Palestra de escalada, Ciudad de Mendoza", -32.8850, -68.8400,
     "Primeros pasos en escalada con arnés y cuerda, siempre asegurados por el instructor. Aprendés los nudos básicos y a moverte en la pared."),
    ("natacion", "carolina", "Natación para adultos", "Nadar", "Física media", 9000, 50, 10, "08:00",
     "Pileta climatizada municipal, Godoy Cruz", -32.9270, -68.8500,
     "Clases para mejorar la técnica de crol y espalda, con ejercicios por niveles dentro de la misma pileta. Para quienes ya flotan y quieren nadar mejor."),
    ("ritmos", "carolina", "Ritmos latinos para moverse", "Bailar", "Física media", 6500, 60, 25, "19:00",
     "Salón de la Casa de la Cultura, Godoy Cruz", -32.9260, -68.8450,
     "Una hora de baile con coreografías simples de salsa, cumbia y merengue. No hace falta pareja ni saber bailar: se aprende en el momento."),
    ("taichi", "carolina", "Tai Chi en la plaza", "Tai Chi", "Física baja", 5000, 60, 20, "09:00",
     "Plaza Independencia, Ciudad de Mendoza", -32.8894, -68.8458,
     "Movimientos lentos y coordinados con la respiración, al aire libre. Mejora el equilibrio y la concentración; apto para todas las edades."),
]
TINTS = [
    "linear-gradient(135deg,#1B3A5C,#12B5A5)",
    "linear-gradient(135deg,#22543D,#12B5A5)",
    "linear-gradient(135deg,#173250,#0FB8A9)",
    "linear-gradient(135deg,#0E2A47,#7A52D9)",
]

COMENTARIOS = {
    5: ["Excelente clase, muy bien explicada y con buena onda.", "Me encantó. La profe se toma el tiempo de corregir a cada uno.",
        "Muy recomendable, ya me anoté para la próxima.", "Salí renovado. Muy buena organización y puntualidad.",
        "Ideal para arrancar, se adapta a todos los niveles.", None],
    4: ["Muy buena, aunque el grupo era grande.", "Me gustó mucho, el lugar es lindo. Le pondría un poco más de intensidad.",
        "Buena clase, empezó unos minutos tarde pero valió la pena.", "Muy completa, la recomiendo.", None],
    3: ["Estuvo bien, aunque esperaba algo más dinámico.", "Correcta. El lugar quedaba un poco chico para tanta gente."],
    2: ["No era lo que esperaba, me costó seguir el ritmo del grupo."],
}
RESPUESTAS = {
    5: "¡Gracias por venir! Te esperamos en la próxima.",
    4: "¡Gracias por el comentario! Lo tenemos en cuenta para las próximas clases.",
    3: "Gracias por contarnos. Vamos a ajustar el grupo para que haya más lugar.",
    2: "Gracias por la sinceridad. La próxima vez avisame y te propongo una variante más tranquila.",
}

# --------------------------------------------------------------------------- armado
L = []
w = L.append

w("""-- =============================================================================
-- ActiveHub - 03. Limpieza y carga de datos de demostracion
-- =============================================================================
-- ANTES de ejecutarlo: correr 01-copia-de-seguridad.sql y avisar al equipo.
-- La base es compartida y esto BORRA datos de forma fisica (no es baja logica).
--
-- Que borra:
--   - Todas las cuentas con rol ALUMNO o INSTRUCTOR, con sus perfiles,
--     intereses, documentos, cuentas de Mercado Pago, codigos de verificacion,
--     notificaciones, reportes de soporte y entradas de auditoria propias.
--   - Toda la oferta: actividades, imagenes, agendas, clases, inscripciones,
--     pagos, resenias, denuncias, penalizaciones, favoritos e interacciones.
--   - Todas las categorias y tipos de actividad.
-- Que NO toca: las cuentas de administrador (y las de roles creados por el
-- admin), roles, permisos, niveles de intensidad y el historial de Flyway.
-- Los archivos subidos (fotos, documentos) no estan en la base y quedan
-- huerfanos en el almacenamiento; no molestan.
--
-- Que carga:
--   - 6 categorias y 18 tipos de actividad.
--   - 5 instructores aprobados y 12 alumnos, todos con el correo verificado,
--     correos @activehub.test (dominio reservado: no existe, no llega nada) y
--     LA MISMA CONTRASENIA, acordada con el equipo y que no se escribe aca: el
--     archivo solo tiene su hash BCrypt.
--   - 18 actividades (una por tipo, ninguna competitiva), cada una con 3 clases
--     ya dictadas (Finalizada) y 3 por venir.
--   - Inscripciones pagadas en Efectivo (las de clases futuras, algunas con el
--     cobro todavia sin confirmar, para mostrar "Confirmar cobro"), algunas
--     preinscripciones, favoritos, y resenias en las clases dictadas, varias
--     con respuesta del instructor.
--   No se usa Mercado Pago a proposito: un pago Retenido haria que los jobs
--   intenten capturarlo contra Mercado Pago.
--
-- Las fechas se calculan desde el dia en que se ejecuta (hora de Argentina).
-- Todo corre en UNA transaccion: si algo falla, no cambia nada.
-- Archivo generado; no editar a mano los datos.
-- =============================================================================

BEGIN;

-- Fecha y hora en Argentina, d dias desde hoy (negativo = pasado). Funcion
-- temporal: desaparece sola al cerrar la sesion.
CREATE FUNCTION pg_temp.dia(d int, hora time) RETURNS timestamptz LANGUAGE sql STABLE AS $f$
    SELECT (((now() AT TIME ZONE '""" + TZ + """')::date + d) + hora) AT TIME ZONE '""" + TZ + """'
$f$;

-- ---------------------------------------------------------------- 1. Limpieza
CREATE TEMP TABLE _usuarios_a_borrar ON COMMIT DROP AS
SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
WHERE r.nombre IN ('ALUMNO', 'INSTRUCTOR');

-- V36: los comprobantes de pago de una multa cuelgan de la penalizacion.
DELETE FROM comprobante_penalizacion WHERE penalizacion_id IN (
    SELECT id FROM penalizacion WHERE denuncia_id IS NOT NULL OR usuario_id IN (SELECT id FROM _usuarios_a_borrar));
DELETE FROM penalizacion WHERE denuncia_id IS NOT NULL OR usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM denuncia;
DELETE FROM pago;
DELETE FROM inscripcion;
DELETE FROM resenia;
DELETE FROM actividad_favorita;
DELETE FROM interaccion_alumno WHERE actividad_id IS NOT NULL OR usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM clase;
DELETE FROM agenda_clases;
DELETE FROM actividad_imagen;
DELETE FROM actividad;
DELETE FROM perfil_alumno_interes;
DELETE FROM documento_instructor WHERE perfil_instructor_id IN (
    SELECT id FROM perfil_instructor WHERE usuario_id IN (SELECT id FROM _usuarios_a_borrar));
DELETE FROM perfil_instructor WHERE usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM perfil_alumno WHERE usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM notificacion WHERE usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM verificacion_email WHERE usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM cuenta_mercadopago_instructor WHERE instructor_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM reporte_soporte WHERE usuario_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM audit_log WHERE actor_id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM usuario WHERE id IN (SELECT id FROM _usuarios_a_borrar);
DELETE FROM tipo_actividad;
DELETE FROM categoria;
""")

w("-- ---------------------------------------------------------------- 2. Categorias y tipos")
for nombre, tipos in CATEGORIAS:
    w(f"INSERT INTO categoria (id, nombre) VALUES ('{uid('cat:' + nombre)}', {q(nombre)});")
for nombre, tipos in CATEGORIAS:
    for t in tipos:
        w(f"INSERT INTO tipo_actividad (id, nombre, categoria_id) VALUES ('{TIPO[t]}', {q(t)}, '{uid('cat:' + nombre)}');")

w("\n-- ---------------------------------------------------------------- 3. Instructores")
dni = 30111000
for i, (k, nom, ape, esp, anios, desc) in enumerate(INSTRUCTORES):
    dni += 137
    email = f"{sin_tildes(nom).lower()}.{sin_tildes(ape).lower()}@activehub.test"
    alta = f"now() - interval '{120 - i * 3} days'"
    w(f"INSERT INTO usuario (id, nombre, apellido, email, password_hash, telefono, fecha_nacimiento, rol_id, estado, dni, "
      f"email_verificado, email_verificado_at, auth_proveedor, created_at, updated_at) VALUES ('{uid('u:' + k)}', {q(nom)}, {q(ape)}, "
      f"{q(email)}, {q(HASH)}, '+54 9 261 555 01{i:02d}', DATE '{1984 + i * 2}-0{i + 3}-1{i}', "
      f"(SELECT id FROM rol WHERE nombre = 'INSTRUCTOR'), 'ACTIVO', '{dni}', true, {alta}, 'LOCAL', {alta}, {alta});")
    w(f"INSERT INTO perfil_instructor (id, usuario_id, especialidad, anios_experiencia, descripcion, estado_verificacion, created_at, updated_at) "
      f"VALUES ('{uid('pi:' + k)}', '{uid('u:' + k)}', {q(esp)}, {anios}, {q(desc)}, 'APROBADO', {alta}, now() - interval '100 days');")

w("\n-- ---------------------------------------------------------------- 4. Alumnos")
for i, (k, nom, ape, intereses, salud) in enumerate(ALUMNOS):
    dni += 211
    email = f"{sin_tildes(nom).lower()}.{sin_tildes(ape).lower()}@activehub.test"
    alta = f"now() - interval '{95 - i * 2} days'"
    w(f"INSERT INTO usuario (id, nombre, apellido, email, password_hash, telefono, fecha_nacimiento, rol_id, estado, dni, "
      f"email_verificado, email_verificado_at, auth_proveedor, created_at, updated_at) VALUES ('{uid('u:' + k)}', {q(nom)}, {q(ape)}, "
      f"{q(email)}, {q(HASH)}, '+54 9 261 555 02{i:02d}', DATE '{1990 + i % 12}-{(i % 12) + 1:02d}-{(i * 2) % 27 + 1:02d}', "
      f"(SELECT id FROM rol WHERE nombre = 'ALUMNO'), 'ACTIVO', '{dni}', true, {alta}, 'LOCAL', {alta}, {alta});")
    w(f"INSERT INTO perfil_alumno (id, usuario_id, condicion_salud, created_at, updated_at) "
      f"VALUES ('{uid('pa:' + k)}', '{uid('u:' + k)}', {q(salud)}, {alta}, {alta});")
    for t in intereses:
        w(f"INSERT INTO perfil_alumno_interes (perfil_alumno_id, tipo_actividad_id) VALUES ('{uid('pa:' + k)}', '{TIPO[t]}');")

w("\n-- ---------------------------------------------------------------- 5. Actividades")
for i, (k, ins, nombre, tipo, nivel, precio, dur, cupo, hora, ubic, lat, lng, desc) in enumerate(ACTIVIDADES):
    w(f"INSERT INTO actividad (id, nombre, descripcion, tipo_actividad_id, instructor_id, precio, ubicacion, photo_tint, latitud, longitud, "
      f"duracion_min, nivel_intensidad_id, created_at, updated_at) VALUES ('{uid('a:' + k)}', {q(nombre)}, {q(desc)}, '{TIPO[tipo]}', "
      f"'{uid('u:' + ins)}', {precio}, {q(ubic)}, {q(TINTS[i % 4])}, {lat}, {lng}, {dur}, "
      f"(SELECT id FROM nivel_intensidad WHERE nombre = {q(nivel)} AND deleted = false), now() - interval '{90 - i} days', now() - interval '{90 - i} days');")

# --------------------------------------------------------------------------- clases, inscripciones, pagos, resenias
clases_sql, insc_sql, pago_sql, res_sql, fav_sql = [], [], [], [], []
alumnos_por_tipo = {}
for k, _, _, intereses, _ in ALUMNOS:
    for t in intereses:
        alumnos_por_tipo.setdefault(t, []).append(k)
todos = [a[0] for a in ALUMNOS]
n_res = n_resp = n_pend = n_pre = 0

for i, (k, ins, nombre, tipo, nivel, precio, dur, cupo, hora, *_rest) in enumerate(ACTIVIDADES):
    base = i % 3
    offsets = [-(4 + base), -(11 + base), -(25 + base), 1 + base, 6 + base, 13 + base]
    interesados = alumnos_por_tipo.get(tipo, [])
    for j, d in enumerate(offsets):
        cid = uid(f"c:{k}:{j}")
        pasada = d < 0
        estado = "Finalizada" if pasada else ("Habilitada" if d <= 4 else "Programada")
        # Siempre los interesados; y algunos mas, para que las clases no queden vacias.
        otros = [a for a in todos if a not in interesados]
        rnd.shuffle(otros)
        elegidos = interesados + otros[: rnd.randint(1, 3)]
        if not pasada:
            elegidos = [a for a in elegidos if rnd.random() < 0.75]
        elegidos = elegidos[:cupo]
        ocupados = 0
        for a in elegidos:
            iid = uid(f"i:{k}:{j}:{a}")
            # Una clase dictada: anotado unos dias antes. Una por venir: anotado hace unos dias.
            alta_insc = (f"{dia(d, hora)} - interval '{rnd.randint(2, 4)} days'" if pasada
                         else f"now() - interval '{rnd.randint(1, 6)} days'")
            if not pasada and d > 4 and rnd.random() < 0.25:
                # Clase a mas de 4 dias: todavia se preinscribe (sin pago ni cupo).
                insc_sql.append(f"('{iid}', '{cid}', '{uid('u:' + a)}', 'PreInscripción', {alta_insc}, {alta_insc})")
                n_pre += 1
                continue
            if not pasada and rnd.random() < 0.3:
                est_insc = "PagoPendiente"
                n_pend += 1
            else:
                est_insc = "Inscripto"
            ocupados += 1
            insc_sql.append(f"('{iid}', '{cid}', '{uid('u:' + a)}', '{est_insc}', {alta_insc}, {alta_insc})")
            pago_sql.append(f"('{uid(f'p:{k}:{j}:{a}')}', '{iid}', 'Efectivo', {precio}, 'Efectivo', {precio}, {alta_insc}, {alta_insc})")
            if pasada and rnd.random() < 0.65:
                puntaje = rnd.choices([5, 4, 3, 2], weights=[45, 35, 15, 5])[0]
                comentario = rnd.choice(COMENTARIOS[puntaje])
                alta_res = f"{dia(d, hora)} + interval '{rnd.randint(3, 20)} hours'"
                if rnd.random() < 0.35:
                    resp, resp_at = q(RESPUESTAS[puntaje]), f"{dia(d, hora)} + interval '30 hours'"
                    n_resp += 1
                else:
                    resp, resp_at = "NULL", "NULL"
                res_sql.append(f"('{uid(f'r:{k}:{j}:{a}')}', '{cid}', '{uid('u:' + a)}', {puntaje}, {q(comentario)}, {resp}, {resp_at}, {alta_res}, {alta_res})")
                n_res += 1
        clases_sql.append(
            f"('{cid}', '{uid('a:' + k)}', {dia(d, hora)}, {dia(d, hora)} + interval '{dur} minutes', '{estado}', {cupo}, {ocupados}, {precio}, "
            f"now() - interval '40 days', now() - interval '40 days')")

for n, (a, *_r) in enumerate(ALUMNOS):
    favs = [x[0] for x in ACTIVIDADES if x[3] in ALUMNOS[n][3]][:2]
    for f in favs:
        fav_sql.append(f"('{uid(f'f:{a}:{f}')}', '{uid('u:' + a)}', '{uid('a:' + f)}', now() - interval '{20 + n} days')")


def bloque(titulo, cabecera, filas):
    w(f"\n-- ---------------------------------------------------------------- {titulo}")
    w(cabecera + " VALUES\n" + ",\n".join(filas) + ";")


bloque("6. Clases (3 dictadas y 3 por venir por actividad)",
       "INSERT INTO clase (id, actividad_id, fecha_hora, hora_fin, estado, cupos_max, cupos_ocupados, precio, created_at, updated_at)", clases_sql)
bloque("7. Inscripciones",
       "INSERT INTO inscripcion (id, clase_id, alumno_id, estado, created_at, updated_at)", insc_sql)
bloque("8. Pagos (todos en efectivo)",
       "INSERT INTO pago (id, inscripcion_id, estado, monto, metodo, monto_instructor, created_at, updated_at)", pago_sql)
bloque("9. Resenias de las clases dictadas",
       "INSERT INTO resenia (id, clase_id, alumno_id, puntaje, comentario, respuesta_instructor, respuesta_instructor_at, created_at, updated_at)", res_sql)
bloque("10. Favoritos",
       "INSERT INTO actividad_favorita (id, usuario_id, actividad_id, created_at)", fav_sql)

w("""
-- ---------------------------------------------------------------- 11. Calificacion de cada actividad
-- Mismo calculo que ActividadRepository.recalcularRating.
UPDATE actividad a SET rating = COALESCE((
    SELECT AVG(r.puntaje) FROM resenia r JOIN clase c ON c.id = r.clase_id
    WHERE c.actividad_id = a.id AND r.deleted = false AND r.oculta = false), 0);

COMMIT;

-- ---------------------------------------------------------------- Resumen
SELECT 'categorias' AS que, count(*) FROM categoria
UNION ALL SELECT 'tipos de actividad', count(*) FROM tipo_actividad
UNION ALL SELECT 'instructores', count(*) FROM perfil_instructor
UNION ALL SELECT 'alumnos', count(*) FROM perfil_alumno
UNION ALL SELECT 'actividades', count(*) FROM actividad
UNION ALL SELECT 'clases', count(*) FROM clase
UNION ALL SELECT 'inscripciones', count(*) FROM inscripcion
UNION ALL SELECT 'pagos', count(*) FROM pago
UNION ALL SELECT 'resenias', count(*) FROM resenia
UNION ALL SELECT 'favoritos', count(*) FROM actividad_favorita;
""")

with open(SALIDA, "w", encoding="utf-8", newline="\n") as fh:
    fh.write("\n".join(L))
print(f"clases={len(clases_sql)} inscripciones={len(insc_sql)} (pendientes={n_pend}, pre={n_pre}) pagos={len(pago_sql)} "
      f"resenias={n_res} (con respuesta={n_resp}) favoritos={len(fav_sql)}")
