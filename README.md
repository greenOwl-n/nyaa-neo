# Nyaa Neo

[![Android Debug APK](https://github.com/greenOwl-n/nyaa-neo/actions/workflows/android-debug.yml/badge.svg)](https://github.com/greenOwl-n/nyaa-neo/actions/workflows/android-debug.yml)

Cliente Android nativo no oficial de **Nyaa.si**, con sección opcional +18 de Sukebei, desarrollado con Kotlin, Jetpack Compose, Material 3 y MVVM. Interfaz en español, fondo AMOLED negro y acentos cian. Compatible con Android 8.0 o posterior (API 26+).

**[Descargar APK 0.2.0 desde GitHub Actions](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42701812527/artifacts/13712485231)** · [Ejecución verificada](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42701812527). Inicia sesión en GitHub, extrae el ZIP e instala `nyaa-neo-debug.apk`. Este artefacto caduca el 9 de diciembre de 2026; puedes generar otro desde Actions.

**Si ya instalaste 0.1.0:** se comprobó que este APK tiene otra firma de depuración; Android no permite actualizar directamente sobre el anterior. Antes de reinstalar, copia los magnets de los favoritos que quieras conservar: desinstalar elimina los datos de la aplicación. El formato de favoritos sigue siendo compatible; la limitación está en la firma de estos APK de desarrollo.

## Funciones

- Búsqueda por RSS con texto, categorías y subcategorías de Nyaa.si.
- Secciones Anime, Manga y Todo; sección +18 de Sukebei con activación voluntaria y miniaturas neutras.
- Filtros: todos, sin remakes y solo publicadores confiables.
- Idiomas destacados y filtro local: español, inglés, multi-sub/audio, raw y otros/sin indicar.
- Fansub/grupo del título y usuario real que publicó el torrent en el detalle de Nyaa.
- Portadas de anime y manga desde Kitsu, TVMaze y MangaDex; búsqueda manual, caché persistente y opción para desactivarlas.
- Orden local: recientes, seeds, leechers, descargas y título.
- Cuadrícula adaptable de pósteres con estética de plataforma de streaming o lista compacta; preferencia persistente.
- Banners de catálogo en los detalles, cuando están disponibles.
- Contadores destacados de seeds, leechers y descargas; detalle con actualización de usuario y estadísticas.
- Favoritos persistentes con metadatos disponibles sin conexión.
- Abrir y copiar magnets; abrir la página original.
- Navegación inferior: Explorar, Favoritos y Ajustes.
- Estados de carga, resultados vacíos y error con reintento.

El RSS devuelve una ventana limitada de resultados. El orden se aplica a esa ventana, no al catálogo completo. Las descripciones completas y los comentarios se consultan mediante «Ver en Nyaa.si». No incluye un motor BitTorrent: los magnets se abren en una aplicación externa compatible.

Los idiomas se deducen de etiquetas del nombre y de la categoría; no se inspeccionan las pistas del archivo. La etiqueta «Inglés · categoría» puede referirse a audio o subtítulos. «Multi-sub» no garantiza español y «Raw» no confirma el idioma del audio. El filtro de idioma se aplica al RSS recibido: para buscar más publicaciones en español puedes añadir `Spanish`, `ESP` o el nombre de un fansub al texto de búsqueda.

«Grupo» corresponde al prefijo del archivo, por ejemplo `[SubsPlease]`; no verifica la autoría del contenido ni la identidad de la cuenta. El usuario que lo publica se obtiene por separado de Nyaa al abrir el detalle y puede ser anónimo. Sin red se mantienen los datos guardados y los controles de magnet/favoritos.

La sección Manga usa las categorías Literatura de Nyaa, que agrupan manga, cómics y novelas. El RSS no ofrece una clasificación estricta que permita separar todas esas publicaciones con certeza.

La sección +18 usa `sukebei.nyaa.si`, separada de las búsquedas de Nyaa. Se activa desde su pestaña o **Ajustes → Sección +18**. Ofrece anime, manga y doujinshi del catálogo para adultos. Sus resultados no aparecen en Anime, Manga ni Todo, y sus favoritos se consultan en su propia pestaña +18. Usa miniaturas neutras y no envía títulos a los catálogos generales de portadas. La activación es una preferencia local, no un sistema de verificación de edad.

Las imágenes son portadas de catálogo de Kitsu, TVMaze o MangaDex, no imágenes verificadas del archivo torrent. Kitsu se consulta primero; si no hay coincidencia o está inaccesible, se usa TVMaze para anime o MangaDex para manga. La búsqueda limpia episodios, volúmenes y etiquetas, prueba títulos alternativos y reconoce diferencias habituales de romanización. Si no hay coincidencia suficiente se muestra una miniatura neutra. **La cobertura no es del 100%**: depende de los títulos, sus alias y la disponibilidad de los catálogos. La presentación tiene identidad visual Neo y un diseño de pósteres y banners propio de una plataforma de streaming.

En el detalle puedes usar **Buscar o cambiar portada** con el nombre original o inglés. La coincidencia y las correcciones se guardan por serie y tipo (anime/manga) en este dispositivo. El enlace a la ficha del proveedor permite comprobarla; entre temporadas puede usarse una portada general de la serie. Se priorizan imágenes originales y se prueban otros tamaños cuando falla la URL principal. Los banners usan la imagen panorámica del catálogo o un recorte de la portada cuando no hay banner.

Las consultas se deduplican por serie, se espacian al menos 1,1 segundos por host y se permiten hasta tres búsquedas simultáneas. Se guardan hasta 256 coincidencias; las positivas duran siete días y se conservan en almacenamiento privado, mientras que los fallos se recuerdan diez minutos en memoria. Coil gestiona la caché de los archivos de imagen. Las portadas sin conexión dependen de que tanto sus metadatos como su imagen estén disponibles en caché.

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
- Jsoup para usuario/estadísticas del detalle; Coil 2.7.0 para portadas de los catálogos públicos.

El SDK se configura con `ANDROID_HOME` o con `sdk.dir` en `local.properties` (no versionado). El APK aparece en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura

```text
app/src/main/java/dev/nyaaneo/
  model/       Fuentes, secciones, torrents, idiomas y consultas de portadas
  data/        RSS, detalle HTML, catálogos, caché de portadas y DataStore
  ui/          ViewModel, pantallas, tema, carátulas y estadísticas
  MainActivity.kt
  NeoApplication.kt
.github/workflows/android-debug.yml
docs/VALIDATION.md
```

El ViewModel expone un `StateFlow`; las pantallas lo observan con el ciclo de vida. Las peticiones y la persistencia se ejecutan fuera del hilo principal. Una nueva búsqueda cancela la anterior. El texto de búsqueda, filtros y detalle seleccionado se conservan mediante `SavedStateHandle`; los favoritos y el modo compacto se guardan en DataStore.

## Privacidad y datos

Las búsquedas generales se envían a `https://nyaa.si/` por HTTPS. La sección +18, cuando se activa y consulta, usa `https://sukebei.nyaa.si/`. No se añaden cuentas, anuncios ni analítica. Los favoritos y la caché permanecen en el almacenamiento privado de la aplicación, sin backup automático, y se pierden al desinstalar o borrar sus datos. «Confiable» reproduce la etiqueta del publicador en la fuente; no valida el contenido del torrent.

Con las portadas activadas se envía el nombre limpio de la obra a `kitsu.io/api/edge/anime` o `manga` y, como respaldo, a `api.tvmaze.com` o `api.mangadex.org`. Las imágenes se descargan por HTTPS desde `media.kitsu.app`, `media.kitsu.io`, `static.tvmaze.com` o `uploads.mangadex.org`. Puedes desactivar estas consultas en **Ajustes → Portadas de anime y manga**; el ajuste se guarda entre sesiones. Los idiomas y el grupo se calculan localmente. El usuario y las estadísticas se consultan en `/view/ID` de la fuente correspondiente. Cada torrent se identifica por la pareja fuente/ID para evitar mezclar datos de ambos sitios.

## Estado de verificación

Consulta [docs/VALIDATION.md](docs/VALIDATION.md) para ver los comandos ejecutados, sus resultados reales y las comprobaciones pendientes en una tablet. La existencia del workflow no implica que ya haya una ejecución exitosa en GitHub.
