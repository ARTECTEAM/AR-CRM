# Deploy — EasyPanel (VPS)

Backend Spring Boot 4 / Java 21. Arranca como un `.jar` ejecutable construido
desde el `Dockerfile` de la raíz. Depende de **PostgreSQL** y **Keycloak**.

Arquitectura objetivo en EasyPanel (3 servicios en el mismo proyecto):

```
crm2-postgres   ← base de datos de la app
crm2-keycloak   ← autenticación (+ su propia base, crm2-keycloak-db)
crm2-backend    ← esta app, construida desde el repo de GitHub
```

Los servicios se hablan entre sí por su **nombre interno** (red privada de
EasyPanel). No expongas Postgres ni Keycloak-DB a internet.

---

## 1. Servicio: crm2-postgres

- Tipo: **Postgres** (template de EasyPanel) o imagen `postgres:17-alpine`.
- Variables:
  - `POSTGRES_DB=crm2`
  - `POSTGRES_USER=crm2`
  - `POSTGRES_PASSWORD=<password-fuerte>`   ← NADA de `crm2/crm2` en prod.
- Volumen persistente en `/var/lib/postgresql/data`.
- NO publicar puerto a internet (solo red interna).

## 2. Servicio: crm2-keycloak

- Imagen: `quay.io/keycloak/keycloak:25.0.6`.
- Necesita su propia DB (otro servicio Postgres, `crm2-keycloak-db`).
- Importá tu realm: subí `Keycloak/realm-export.json` y usá `--import-realm`.
- Dominio público con HTTPS (ej. `https://auth.tudominio.com`), porque el
  `issuer` del token DEBE ser una URL pública estable.
- Variables clave: `KC_DB`, `KC_DB_URL`, `KC_DB_USERNAME`, `KC_DB_PASSWORD`,
  `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, y `KC_HOSTNAME=auth.tudominio.com`.

> ⚠️ **El gotcha de Keycloak.** El `issuer-uri` que configurás en el backend
> tiene que coincidir EXACTAMENTE con el claim `iss` que Keycloak mete en los
> tokens. Si Keycloak está detrás del proxy de EasyPanel, configurá
> `KC_HOSTNAME` con la URL pública. Si no coinciden, el backend rechaza TODOS
> los tokens con 401 y vas a perder horas. Es el error #1 de Keycloak + proxy.

## 3. Servicio: crm2-backend (esta app)

- **Source**: GitHub → tu repo → rama `main`.
- **Build**: `Dockerfile` (raíz del repo). EasyPanel lo detecta solo.
- **Port**: `8080` (coincide con `server.port`).
- **Dominio**: `https://api.tudominio.com` con HTTPS (Let's Encrypt en EasyPanel).
- **Environment** (estas variables sobrescriben los defaults de `application.yml`):

```env
# Base de datos (apunta al nombre interno del servicio Postgres)
CRM2_DB_URL=jdbc:postgresql://crm2-postgres:5432/crm2
CRM2_DB_USERNAME=crm2
CRM2_DB_PASSWORD=<password-fuerte>

# Keycloak — validación de tokens (URL PÚBLICA, debe matchear el iss del token)
CRM2_KEYCLOAK_ISSUER_URI=https://auth.tudominio.com/realms/crm2-local
CRM2_KEYCLOAK_AUDIENCE=crm2-api

# Keycloak — cliente admin (puede usar la URL interna)
CRM2_KEYCLOAK_SERVER_URL=http://crm2-keycloak:8180
CRM2_KEYCLOAK_REALM=crm2-local
CRM2_KEYCLOAK_CLIENT_ID=crm2-api
KEYCLOAK_ADMIN_SECRET=<secret-del-cliente-crm2-api>

# Mail (SMTP Gmail)
CRM2_NOTIFICATION_MAIL_USERNAME=<tu-cuenta@gmail.com>
CRM2_NOTIFICATION_MAIL_PASSWORD=<app-password-de-gmail>
CRM2_MAIL_FROM_ADDRESS=<from@tudominio.com>
```

---

## 4. Autodeploy (push a git → redeploy)

En EasyPanel, dentro del servicio `crm2-backend`:

1. Sección **Source/Deployments** → activá **Auto Deploy** y copiá la
   **Deploy Webhook URL** que te genera.
2. En GitHub → repo → **Settings → Webhooks → Add webhook**:
   - **Payload URL**: la URL del webhook de EasyPanel.
   - **Content type**: `application/json`.
   - **Events**: solo `push`.
3. Listo. Cada `push` a `main` dispara: EasyPanel hace `pull` → reconstruye la
   imagen con el `Dockerfile` → redeploya. Cero clicks tuyos.

> Si querés deployar solo cuando termina un build de CI (no en cada push crudo),
> en vez del webhook nativo poné un step al final de un GitHub Action que haga
> `curl -X POST <deploy-webhook-url>`.

---

## Notas

- `ddl-auto: update` deja que Hibernate cree/actualice el schema solo. Está bien
  para arrancar; cuando madures, migrá a Flyway/Liquibase y poné `validate`.
- Primer arranque: levantá **Postgres y Keycloak primero**, esperá que estén
  healthy, y recién después el backend (si no, falla la conexión al iniciar).
- Memoria: la JVM respeta el límite del contenedor (`-XX:MaxRAMPercentage=75`).
  Asigná al backend al menos 512 MB–1 GB en EasyPanel.
