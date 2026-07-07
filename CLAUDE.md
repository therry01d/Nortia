# CLAUDE.md — Nortia

Contexto para Claude Code. Léelo al inicio de cada sesión antes de tocar archivos.

## Qué es Nortia
Aplicación Android nativa de **agenda personal**: crear, listar (ordenados por fecha) y
eliminar eventos, con persistencia local. App offline-first, sin backend.

## Stack
- **Kotlin** + **Jetpack Compose** (Material Design 3)
- **Room** (SQLite) para persistencia
- **Coroutines / Flow** para asincronía y observación reactiva de la BD
- `applicationId` / `namespace`: `com.therry.nortia`
- `minSdk` 24, `targetSdk` 34, `compileSdk` 34
- Rama principal: `main`

## Estado actual (IMPORTANTE)
El repo es un esqueleto y **hoy NO compila**. Solo existen:
- `app/src/main/kotlin/com/therry/nortia/screens/AgendaScreen.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

### Falta crear (prioridad para dejarlo compilando)
1. `MainActivity.kt` — punto de entrada; el Manifest referencia `.MainActivity`.
2. `data/Event.kt` — entidad Room. Campos usados hoy por la UI:
   `id: Int`, `title: String`, `description: String`, `date: Long`, `time: String`.
3. `data/EventDao.kt` — insert / delete / getAll (devolver `Flow<List<Event>>`, ordenado por `date`).
4. `data/AppDatabase.kt` — `RoomDatabase` con singleton.
5. `ui/theme/Theme.kt` y `ui/theme/Type.kt` — el Manifest usa `Theme.Nortia`.
6. Recursos: `res/values/strings.xml` (`app_name`), `res/values/themes.xml` (`Theme.Nortia`),
   ícono `mipmap/ic_launcher`.
7. `settings.gradle.kts`, `build.gradle.kts` raíz y Gradle wrapper.

### Bugs / deuda técnica a corregir
- `app/build.gradle.kts` usa `kapt(...)` para Room pero **no aplica el plugin kapt**.
  Agregar en `plugins { }`: `id("org.jetbrains.kotlin.kapt")`.
- `AgendaScreen` guarda eventos en memoria (`mutableStateOf`); al cerrar la app se pierden.
  Conectar a Room vía un `ViewModel` que exponga los eventos como `StateFlow`.
- El `id` se genera con `events.size + 1` (frágil, colisiona tras borrar).
  Usar `@PrimaryKey(autoGenerate = true)` en la entidad.

## Arquitectura objetivo
UI (Compose) → ViewModel (StateFlow) → Repository (opcional) → EventDao → Room.
La pantalla observa un `StateFlow<List<Event>>`; nunca mantener la lista en estado local.

## Convenciones
- Todo el código bajo `com.therry.nortia`, en `app/src/main/kotlin/...`.
- Strings visibles al usuario en español, en `strings.xml` (no hardcodear en Compose).
- Un `@Composable` por responsabilidad; extraer componentes reutilizables (p. ej. `EventCard`).
- Commits en español, imperativos y cortos (ej. "agrega persistencia Room a AgendaScreen").

## Cómo compilar
```bash
./gradlew assembleDebug        # genera el APK debug
./gradlew installDebug         # instala en dispositivo/emulador conectado
```
Para desarrollo normal se abre en Android Studio y se corre con Run.

## Roadmap (del README)
Editar eventos · notificaciones · categorías · buscar/filtrar · exportar (iCal/PDF) ·
sync en la nube (Firebase) · i18n · widget de pantalla de inicio.
