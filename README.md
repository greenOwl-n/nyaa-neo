# Nyaa Neo

[![Android Debug APK](https://github.com/greenOwl-n/nyaa-neo/actions/workflows/android-debug.yml/badge.svg)](https://github.com/greenOwl-n/nyaa-neo/actions/workflows/android-debug.yml)

Cliente Android nativo no oficial de **Nyaa.si**, desarrollado con Kotlin, Jetpack Compose, Material 3 y MVVM. Interfaz en español, fondo AMOLED negro y acentos cian. Compatible con Android 8.0 o posterior (API 26+).

**[Descargar APK 0.1.0 desde GitHub Actions](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696845362/artifacts/13709293557)** · [Ejecución verificada](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696845362). Inicia sesión en GitHub, extrae el ZIP e instala `nyaa-neo-debug.apk`. Este artefacto caduca el 9 de diciembre de 2026; puedes generar otro desde Actions.

## Funciones

- Búsqueda por RSS con texto, categorías y subcategorías de Nyaa.si.
- Filtros: todos, sin remakes y solo publicadores confiables.
- Idiomas destacados y filtro local: español, inglés, multi-sub/audio, raw y otros/sin indicar.
- Fansub/grupo del título y usuario real que publicó el torrent en el detalle de Nyaa.
- Carátulas orientativas de anime desde Kitsu, con opción para desactivarlas.
- Orden local: recientes, seeds, leechers, descargas y título.
- Tarjetas o lista compacta, con preferencia persistente.
- Contadores destacados de seeds, leechers y descargas; detalle con actualización de usuario y estadísticas.
- Favoritos persistentes con metadatos disponibles sin conexión.
- Abrir y copiar magnets; abrir la página original.
- Navegación inferior: Explorar, Favoritos y Ajustes.
- Estados de carga, resultados vacíos y error con reintento.

El RSS devuelve una ventana limitada de resultados. El orden se aplica a esa ventana, no al catálogo completo. Las descripciones completas y los comentarios se consultan mediante «Ver en Nyaa.si». No incluye un motor BitTorrent: los magnets se abren en una aplicación externa compatible.

Los idiomas se deducen de etiquetas del nombre y de la categoría; no se inspeccionan las pistas del archivo. La etiqueta «Inglés · categoría» puede referirse a audio o subtítulos. «Multi-sub» no garantiza español y «Raw» no confirma el idioma del audio. El filtro de idioma se aplica al RSS recibido: para buscar más publicaciones en español puedes añadir `Spanish`, `ESP` o el nombre de un fansub al texto de búsqueda.

«Grupo» corresponde al prefijo del archivo, por ejemplo `[SubsPlease]`; no verifica la autoría del contenido ni la identidad de la cuenta. El usuario que lo publica se obtiene por separado de Nyaa al abrir el detalle y puede ser anónimo. Sin red se mantienen los datos guardados y los controles de magnet/favoritos.

Las imágenes son carátulas de catálogo de Kitsu, no capturas ni imágenes del archivo torrent. Se limpia el nombre del episodio y se compara con títulos/alias del catálogo; si no hay coincidencia suficiente se muestra un placeholder. La coincidencia es orientativa, especialmente entre temporadas. El enlace a Kitsu del detalle permite comprobarla. Las consultas se deduplican por serie, se espacian al menos 1,1 segundos y se mantienen hasta 128 resultados en memoria; Coil gestiona la caché de imágenes. Las carátulas no están garantizadas sin conexión.

## Compilar y descargar desde una tablet

No necesitas PC ni Android Studio. Una vez subido este proyecto a tu repositorio:

1. Abre el repositorio en el navegador e inicia sesión en GitHub.
2. Entra en **Actions → Android Debug APK**. Si GitHub lo solicita, habilita los workflows.
3. Pulsa **Run workflow → Run workflow** sobre `main`. También se ejecuta al subir cambios a `main` o abrir una pull request.
4. Abre la ejecución y espera a que termine en verde. La compilación incluye pruebas unitarias y Android Lint; si falla, abre el paso «Compilar, probar y analizar» para ver el error.
5. En **Artifacts**, descarga **nyaa-neo-debug**. Extrae el ZIP e instala `nyaa-neo-debug.apk`.
6. Android puede pedir que permitas instalar aplicaciones desde el navegador o gestor de archivos que uses.

El artefacto incluye un checksum SHA-256 y se conserva durante 30 días. Puedes generar otro con **Run workflow**. Los informes de pruebas y Lint se publican como `android-reports` incluso si falla la compilación. La descarga de artefactos de GitHub exige iniciar sesión y tener acceso al repositorio.

El APK de depuración usa una clave de desarrollo. Diferentes entornos de compilación pueden generar claves distintas; si Android rechaza una actualización por firma incompatible, desinstalar borra también los favoritos. Para distribuir actualizaciones estables hará falta configurar una firma de release persistente.

## Herramientas y compilación local opcional

El flujo habitual es [GitHub Actions](https://github.com/greenOwl-n/nyaa-neo/actions/workflows/android-debug.yml). Para otros entornos con Java y Android SDK:

```sh
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- Java 17 (Eclipse Temurin en el entorno de desarrollo y en GitHub Actions).
- Gradle 8.11.1 mediante wrapper, con checksum oficial fijado.
- Android Gradle Plugin 8.9.1; Kotlin y Compose Compiler 2.1.20.
- SDK y target 35; Build Tools 35.0.0.
- Compose BOM 2025.04.01; Material 3; Navigation Compose.
- OkHttp para HTTPS; XmlPullParser para RSS; DataStore Preferences para persistencia.
- Jsoup para usuario/estadísticas del detalle; Coil 2.7.0 para imágenes del catálogo Kitsu.

El SDK se configura con `ANDROID_HOME` o con `sdk.dir` en `local.properties` (no versionado). El APK aparece en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura

```text
app/src/main/java/dev/nyaaneo/
  model/       Torrent, idiomas, grupo, categorías, filtros y orden
  data/        RSS, detalle HTML, carátulas, HTTPS y DataStore
  ui/          ViewModel, pantallas, tema, carátulas y estadísticas
  MainActivity.kt
  NeoApplication.kt
.github/workflows/android-debug.yml
docs/VALIDATION.md
```

El ViewModel expone un `StateFlow`; las pantallas lo observan con el ciclo de vida. Las peticiones y la persistencia se ejecutan fuera del hilo principal. Una nueva búsqueda cancela la anterior. El texto de búsqueda, filtros y detalle seleccionado se conservan mediante `SavedStateHandle`; los favoritos y el modo compacto se guardan en DataStore.

## Privacidad y datos

Las búsquedas se envían a `https://nyaa.si/` por HTTPS. No se añaden cuentas, anuncios ni analítica. Los favoritos permanecen en el almacenamiento privado de la aplicación, sin backup automático, y se pierden al desinstalar o borrar sus datos. «Confiable» reproduce la etiqueta del publicador en Nyaa.si; no valida el contenido del torrent.

Con las carátulas activadas se envía el nombre limpio del anime a `https://kitsu.io/api/edge/anime` y se descarga la imagen por HTTPS desde `media.kitsu.app` o `media.kitsu.io`. Puedes desactivar estas consultas en **Ajustes → Carátulas de anime**; el ajuste se guarda entre sesiones. Los idiomas y el grupo se calculan localmente. El usuario y las estadísticas del detalle se consultan en `https://nyaa.si/view/ID`.

## Estado de verificación

Consulta [docs/VALIDATION.md](docs/VALIDATION.md) para ver los comandos ejecutados, sus resultados reales y las comprobaciones pendientes en una tablet. La existencia del workflow no implica que ya haya una ejecución exitosa en GitHub.
