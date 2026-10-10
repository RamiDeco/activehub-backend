# ActiveHub API

Backend de ActiveHub: Java 21 + Spring Boot 4.1 + PostgreSQL 16, con arquitectura de vertical slices (un paquete por caso de uso bajo `usecases/`).

## Requisitos previos

- **JDK 21** (no funciona con versiones anteriores)
- **Docker Desktop** (para levantar PostgreSQL)
- **Git**

No hace falta instalar Maven: el proyecto incluye el wrapper (`mvnw` / `mvnw.cmd`).

## 1. Clonar el repositorio

```bash
git clone https://github.com/RamiDeco/activehub-backend.git activehub-api
cd activehub-api
```

## 2. Levantar PostgreSQL con Docker

```bash
docker run -d \
  --name activehub-postgres \
  -p 5433:5432 \
  -e POSTGRES_USER=activehub \
  -e POSTGRES_PASSWORD=activehub \
  -e POSTGRES_DB=activehub \
  postgres:16
```

Verificar que quedó arriba:

```bash
docker ps
```

Las próximas veces que quieras levantar el proyecto, alcanza con `docker start activehub-postgres` (no hace falta volver a correr `docker run`).

No hace falta crear tablas a mano: Flyway corre las migraciones automáticamente al iniciar la aplicación.

## 3. Configurar variables de entorno (opcional)

La lista completa de variables, con comentarios y sin valores, está en **`.env.example`**: copialo a `.env` y completalo. Las principales, con su valor por defecto:

| Variable | Default | Para qué sirve |
|---|---|---|
| `PORT` | `8080` | Puerto donde escucha la API |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | ninguno (**obligatorias**) | Conexión a Postgres (la base compartida es la de Supabase; para el Postgres local del paso 2, `jdbc:postgresql://localhost:5433/activehub`) |
| `JWT_SECRET` | clave de desarrollo incluida | Firma de los tokens JWT — **cambiarla si esto se despliega en serio** |
| `JWT_EXPIRATION_MIN` | `30` | Minutos de validez del token; es también la ventana de inactividad de la sesión |
| `ADMIN_SEED_EMAIL` / `ADMIN_SEED_PASSWORD` | vacío (no siembra nada) | Si se completan ambas, al arrancar se crea un usuario ADMIN con esas credenciales (solo si no existe ya) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos para llamar a la API (el puerto por defecto del frontend en Vite) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | vacío (no manda mails) | Cuenta de Gmail y **contraseña de aplicación** con la que se envía el código de verificación. **Hacen falta las dos**: con una sola, el envío fallaría y el código se perdería, así que se cae al modo log |
| `MAIL_FROM` / `MAIL_FROM_NOMBRE` | `MAIL_USERNAME` / `ActiveHub` | Remitente. Gmail reescribe el `From` a la cuenta autenticada, así que poner otra dirección no sirve |
| `VERIFICACION_TTL_MIN` | `15` | Minutos que vale cada código |
| `VERIFICACION_ESPERA_REENVIO_SEG` | `60` | Espera mínima entre dos envíos al mismo usuario |
| `VERIFICACION_MAX_INTENTOS` | `5` | Intentos fallidos antes de invalidar el código |
| `GOOGLE_CLIENT_ID` | vacío (botón oculto) | Client ID de la app web en Google Cloud Console, para "Continuar con Google" |

### Mercado Pago

Cada instructor cobra con **su propia** cuenta de Mercado Pago, que vincula desde su Panel (OAuth). Sin estas variables la aplicación arranca igual, pero la vinculación y el cobro con Mercado Pago no funcionan y las clases sólo aceptan efectivo.

| Variable | Para qué sirve |
|---|---|
| `MP_CLIENT_ID` / `MP_CLIENT_SECRET` | Credenciales de la aplicación de Mercado Pago (OAuth del marketplace) |
| `MP_REDIRECT_URI` | URL de vuelta del OAuth; tiene que coincidir **exacto** con la configurada en la aplicación (ej. `http://localhost:8080/api/instructor/mercadopago/callback`) |
| `MP_TOKEN_ENCRYPTION_KEY` | Clave AES-256 (32 bytes en base64) con la que se cifran en la base los tokens de los instructores |
| `MP_WEBHOOK_SECRET` | Clave secreta del webhook; sin ella toda notificación de Mercado Pago se rechaza |
| `MP_SANDBOX_PAYER_EMAIL` | **Solo sandbox:** correo del comprador de prueba. En producción, vacía |
| `MP_LOG_LEVEL` | `DEBUG` loguea cada pedido a Mercado Pago con los secretos recortados (default `INFO`) |
| `FRONTEND_URL` | A dónde vuelve el navegador después del OAuth (default `http://localhost:5173`) |

**Para probar en sandbox** hay que usar cuentas de prueba para todo, **también la aplicación** (creada por un usuario de prueba "Integrador"), con credenciales `APP_USR-`: la Orders API rechaza las `TEST-`. El detalle y la matriz de pruebas están en `CLAUDE.md` (sección "Mercado Pago") y en `docs/PRUEBAS-SANDBOX-MERCADOPAGO.md`.

### Correo (código de verificación)

**Sin las credenciales completas la app arranca igual y el registro funciona**: en vez de mandar el mail, `EmailSender` escribe el código y el HTML completo en el log del backend, en nivel WARN. Es el modo de desarrollo y es lo que permite usar el repo sin secretos.

Para mandar mails de verdad con Gmail hace falta una **contraseña de aplicación**, no la contraseña de la cuenta:

1. Activar la verificación en dos pasos en la cuenta de Google.
2. Google Account → Seguridad → *Contraseñas de aplicaciones* → generar una.
3. Usar esa cadena de 16 caracteres como `MAIL_PASSWORD` (sin espacios) y el correo completo como `MAIL_USERNAME`.

### "Continuar con Google"

`GOOGLE_CLIENT_ID` sale de Google Cloud Console → *APIs & Services* → *Credentials* → *OAuth 2.0 Client ID* de tipo **Web application**, agregando `http://localhost:5173` (y el puerto que uses) en **Authorized JavaScript origins**. No hace falta client secret: el flujo es el de Google Identity Services, que entrega un ID token al navegador y el backend lo verifica contra las claves públicas de Google.

El frontend **no** necesita su propia variable: pide el client id a `GET /api/auth/google/config`. Sin la variable configurada, ese endpoint responde `habilitado: false` y el botón directamente no se muestra.

Para setearlas en Windows (PowerShell), antes de correr la app:

```powershell
$env:ADMIN_SEED_EMAIL = "admin@activehub.test"
$env:ADMIN_SEED_PASSWORD = "Admin123!"
```

En Linux/Mac (bash):

```bash
export ADMIN_SEED_EMAIL=admin@activehub.test
export ADMIN_SEED_PASSWORD=Admin123!
```

Sin un admin sembrado no vas a poder entrar a ninguna pantalla de `/admin` — hace falta al menos una vez para tener un usuario con ese rol.

## 4. Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

La API queda escuchando en `http://localhost:8080` (o el `PORT` que hayas configurado). Vas a ver en el log `Started ActivehubApiApplication` cuando terminó de arrancar.

## 5. Correr los tests

```bash
./mvnw test
```

Usá un JDK 21 (`JAVA_HOME` apuntando a él). **Necesita la base configurada en `.env`**: la mayoría de los tests usan mocks (Mockito) y `@WebMvcTest`, pero los `@SpringBootTest` levantan el contexto completo, y al hacerlo Flyway aplica las migraciones pendientes. Los 2 tests de `MercadoPagoGatewaySandboxTest` se omiten a propósito: son una prueba manual contra el sandbox.

## Notas

- El frontend (`activehub-frontend`) espera la API en `http://localhost:8080` por defecto — si cambiás `PORT`, actualizá también `VITE_API_URL` en el `.env` del frontend.
- Si vas a correr el frontend en un puerto distinto al `5173` (por ejemplo para tener dos instancias en paralelo), agregalo a `CORS_ALLOWED_ORIGINS` separado por coma: `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5180`.
- Los pagos usan Mercado Pago real (en desarrollo, su sandbox); no hay pasarela simulada. Las imágenes y documentos se guardan en disco o en Supabase Storage, según las variables configuradas. Ver `CLAUDE.md` para el detalle.
