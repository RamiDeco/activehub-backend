# Handoff — ActiveHub (API REST + Front)

Plataforma web de gestión de actividades deportivas, recreativas y formativas. Tres roles: **Alumno**, **Instructor**, **Administrador**.

Este paquete es el punto de partida para implementar el sistema real en **Claude Code**. Acá tenés todo el contexto: arquitectura, modelo de dominio, contratos de API, reglas de negocio y la especificación visual de las pantallas.

---

## ⚠️ Nota sobre el front (actualizado)

El plan original de este handoff asumía un front en **Next.js**. En la práctica, el front de este proyecto (`activehub-frontend`, repo hermano) ya está implementado como un **SPA en Vite + React + TypeScript**, con las 36 pantallas del prototipo recreadas y con auth/datos **mockeados** (`AuthContext` + `DataContext`, persistidos en `localStorage`, sin backend real). Este repo (`activehub-api`) es el que falta: cuando el slice de Auth esté andando acá, el `AuthContext` del front se reemplaza por llamadas HTTP reales siguiendo los contratos de `03-CASOS-DE-USO-AUTH.md` — no hace falta tocar las pantallas.

---

## Cómo arrancar en Claude Code

1. Este repo (`activehub-api`) ya tiene `CLAUDE.md` en la raíz — Claude Code lo lee primero y de ahí saca las convenciones (arquitectura por casos de uso, soft-delete, auditoría, manejo de errores).
2. La carpeta `design_handoff_activehub/` (esta) tiene el resto del contexto.
3. Pedile a Claude Code, por ejemplo:
   > "Leé `CLAUDE.md` y el handoff en `design_handoff_activehub/`. Implementá el **slice de Auth** descrito en `03-CASOS-DE-USO-AUTH.md`: registro de Alumno, registro de Instructor, login, seed del admin inicial, y el endpoint protegido para que un admin cree otro admin. Seguí la arquitectura de `01-ARQUITECTURA.md` al pie de la letra."
4. Cuando ese slice esté andando y probado, seguís con el próximo caso de uso (catálogo, inscripción, etc.).

---

## Stack y servicios (decidido)

| Capa | Tecnología |
|---|---|
| API | **Java + Spring Boot** (REST) |
| Front | **Vite + React + TypeScript** (SPA) — repo hermano `activehub-frontend`, ya implementado con mocks |
| Base de datos | **Supabase** (PostgreSQL) |
| Storage de archivos | **Supabase Storage** (documentación del instructor) |
| Deploy API | **Render** |
| Deploy Front | **Vercel** |
| Pagos | **Mercado Pago** — *primero simulado*, integración real después |
| Mapas | **Google Maps API** (en el front) |

### Estrategia de pagos (importante)
Arrancamos con un **gateway de pago simulado** (mock) detrás de una interfaz. Primero hacemos andar todo el flujo de inscripciones / bajas / estados de pago sobre ese mock; cuando el flujo esté estable y probado, enchufamos Mercado Pago real implementando la misma interfaz. Ver `01-ARQUITECTURA.md` → "Seam de pagos".

---

## Orden de implementación

**Slice 1 (este handoff):** Registrarse (Alumno + Instructor) · Iniciar sesión · Admin inicial sembrado · Un admin crea otro admin.
Los administradores **no** se registran desde una pantalla pública: existe un único admin sembrado (seed) y los demás se crean desde adentro por otro admin.

**Siguientes (más adelante):** catálogo/explorar · detalle de actividad · pre-inscripción / inscripción + pago · gestión del instructor (actividades, clases, asistencia, cobro) · reseñas · denuncias · panel admin.

---

## Índice de documentos

- **`00-CLAUDE-modelo-negocio.md`** — el `CLAUDE.md` original del proyecto de diseño: taxonomía del dominio, reglas de negocio clave, glosario. Fuente de verdad del *negocio* (no de la arquitectura de código).
- **`01-ARQUITECTURA.md`** — arquitectura por casos de uso (vertical slice), estructura de paquetes, cross-cutting, deploy, env vars.
- **`02-MODELO-DOMINIO.md`** — entidades, enums/estados, relaciones y esquema de DB del slice de Auth.
- **`03-CASOS-DE-USO-AUTH.md`** — contratos de API, DTOs, validaciones, flujos y errores de cada caso de uso de Auth.
- **`04-PANTALLAS-AUTH.md`** — spec hi-fi de las pantallas Login y Registro (colores, tipografía, layout, copy, estados) — ya implementadas en `activehub-frontend`, referencia para que la API devuelva exactamente lo que esas pantallas esperan.

## Glosario de negocio
Definido en detalle en `02-MODELO-DOMINIO.md` y `00-CLAUDE-modelo-negocio.md`. Reglas clave (pre-inscripción vs inscripción, estados, pagos) en `03-CASOS-DE-USO-AUTH.md`.
