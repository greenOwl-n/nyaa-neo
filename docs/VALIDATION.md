# Verificación

Este archivo documenta evidencia real. No sustituye una prueba de uso en Android.

## Versión 0.2.0: idiomas, grupo, carátulas y estadísticas

Se ejecutó una primera compilación local con las nuevas dependencias: **BUILD SUCCESSFUL in 1m 43s**, 37 pruebas sin fallos y Lint sin errores.

La revisión final se comprobó con `bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`: **BUILD SUCCESSFUL in 1m 7s**. Resultado: **38 pruebas**, 0 fallos y 0 omitidas; Android Lint, 0 errores y 6 advertencias de actualización de dependencias. Incluye el caso de regresión de códigos de idioma al principio del título y la cancelación de consultas de carátulas.

Una revisión posterior detectó una ambigüedad en la categoría `Non-English-translated`. Se corrigió para dar prioridad al ID de categoría y excluir expresamente `Non-English` en los favoritos antiguos. La comprobación completa posterior terminó **BUILD SUCCESSFUL in 55s**, con **39 pruebas aprobadas**, 0 omitidas y Lint con 0 errores y 6 advertencias de actualización de dependencias.

Fuentes comprobadas por HTTPS durante esta sesión:

- RSS anime de Nyaa (`?page=rss&c=1_0&f=0`).
- Página real `https://nyaa.si/view/2184421`: usuario anónimo, campos de seeds/leechers/completados; se conserva como fixture de parser HTML.
- Respuesta real de Kitsu para `Naruto`: se guardó un fixture reducido con títulos, alias y URLs de poster.
- Descarga de la URL `medium` exacta de ese catálogo: JPEG válido de 390 × 554 píxeles.

Las pruebas verifican que se elige la serie que coincide y no el primer resultado del catálogo, que se rechazan imágenes HTTP o de otros hosts, que los favoritos de 0.1.0 siguen siendo legibles y que «Multi-sub» no se convierte en una afirmación de subtítulos en español.

### GitHub Actions 0.2.0 comprobado

- [Ejecución 42701812527](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42701812527): `completed`, conclusión **success**.
- Código compilado: `23b7c999fefbb6b23d982a466bc3064aded43ab5`.
- Compilación, pruebas unitarias y Lint: **BUILD SUCCESSFUL in 1m 2s**.
- [APK y checksum](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42701812527/artifacts/13712485231), hasta el 9 de diciembre de 2026.
- [Informes de pruebas y Lint](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42701812527/artifacts/13710644924), hasta el 23 de noviembre de 2026.

Se descargó el artefacto real con el conector de GitHub y se verificó su ZIP, el checksum del APK, la firma y los metadatos:

```text
SHA-256 ZIP (coincide con el digest publicado por GitHub):
d65c0cf40b791e02d8a8cd5f2b18b05e4e119384902a480ad5fe5858f98b63bd

SHA-256 nyaa-neo-debug.apk (coincide con el .sha256 incluido):
a23da26fe09b00696d668d5b4def6ff2b3df558db1cbb424331510f6e981f1b0

apksigner verify --verbose: Verifies; APK Signature Scheme v2: true
aapt: dev.nyaaneo; versionCode 2; versionName 0.2.0; minSdk 26; targetSdk 35
```

Se compararon los certificados de los APK de Actions de 0.1.0 y 0.2.0: son diferentes (`16592fa7…` y `08322fda…`, SHA-256 del certificado). Android no permitirá actualizar uno sobre el otro sin reinstalar; desinstalar elimina los favoritos. Esto se informa junto a la descarga en el README. No se ha configurado una clave de firma persistente ni se ha probado la instalación sobre 0.1.0 en un dispositivo.

La evidencia y los enlaces de los apartados siguientes corresponden a 0.1.0.

La interacción en dispositivo sigue pendiente: comprobar etiquetas de idioma y grupo, filtro local, detalle con cuenta o anónimo, refresco de estadísticas, placeholders, carátulas y su interruptor (también sin conexión). Las pruebas de persistencia cubren el formato y su compatibilidad; no se ha probado una actualización del APK en una tablet. La firma de desarrollo puede cambiar entre runners, como se explica en el README.

## Evidencia de la versión 0.1.0

## Entorno

- Linux x86_64; Eclipse Temurin JDK 17.0.21.
- SDK oficial Android 35 y Build Tools 35.0.0 instalados para esta sesión.
- Configuración de GitHub Actions: Ubuntu, Temurin 17 y SDK Android 35.

El primer intento detectó que el Java 21 preinstalado era un runtime sin compilador (`JAVA_COMPILER`). Se instaló un JDK 17 completo en el workspace y se repitió la compilación. La configuración del proyecto no depende de las rutas locales de este entorno.

## Resultado local comprobado

Comando ejecutado con el wrapper del proyecto y Java 17:

```sh
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Resultado del 9 de noviembre de 2026: **BUILD SUCCESSFUL in 56s**.

- APK generado: `app/build/outputs/apk/debug/app-debug.apk`.
- 15 pruebas unitarias, 0 fallos y 0 omitidas: 12 de RSS/URL/orden y 3 de serialización de favoritos.
- Android Lint: 0 errores; 6 advertencias por versiones más recientes de dependencias. Se mantuvieron las versiones fijadas y comprobadas; no se deshabilitó ningún detector.
- `apksigner verify --verbose`: firma APK v2 válida.
- `aapt dump badging`: `dev.nyaaneo`, versión `0.1.0` (código 1), mínimo API 26 y target API 35.
- `bash ./gradlew --version`: Gradle 8.11.1 descargado y checksum oficial validado.

Las pruebas detectaron y permitieron corregir la opción DTD no admitida por algunos proveedores XML y la aceptación de documentos truncados. Lint detectó un atributo de navegación de API 27 en los recursos base; se sustituyó por `SystemBarStyle.dark` de AndroidX, compatible con el mínimo API 26. Las barras conservan iconos claros también cuando el sistema usa tema claro.

No se ha instalado ni ejecutado el APK en un dispositivo o emulador en esta sesión. La prueba de interacción y de persistencia real de DataStore en Android sigue pendiente; las pruebas JVM cubren la serialización de los datos guardados.

## RSS real

Se recibió correctamente, mediante HTTPS, un RSS de `https://nyaa.si/?page=rss&q=ubuntu&c=0_0&f=0`. La respuesta contiene los campos `guid`, `nyaa:infoHash`, `nyaa:category`, `nyaa:size`, `nyaa:seeders`, `nyaa:leechers`, `nyaa:downloads` y `nyaa:trusted` utilizados por la aplicación. Esta comprobación de conectividad no demuestra el funcionamiento en todas las redes ni sustituye la prueba en dispositivo.

## Comprobación manual en la tablet

Pendiente hasta instalar el APK:

1. Abrir la aplicación, cargar el RSS inicial, buscar un título y cambiar categoría/filtro.
2. Cambiar tarjetas/lista, cerrar y abrir la aplicación: verificar que el modo se mantiene.
3. Guardar un favorito, forzar cierre y activar modo avión: comprobar favorito y detalle.
4. Quitar el favorito desde el detalle y volver a la lista.
5. Probar cada orden y la búsqueda local en favoritos.
6. Abrir magnet con una aplicación compatible; probar también sin aplicación compatible y copiar el enlace.
7. Volver desde el navegador o cliente torrent y recorrer la navegación inferior.
8. Rotar la tablet, usar pantalla dividida y aumentar tamaño de fuente.
9. Simular falta de red: comprobar error y botón Reintentar.

## GitHub Actions

Repositorio: <https://github.com/greenOwl-n/nyaa-neo>.

La primera ejecución ([42696047697](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696047697)) falló en `android-actions/setup-android@v3`: su configuración por defecto intentaba instalar el paquete obsoleto `tools` (`Failed to find package 'tools'`). Se configuró explícitamente `packages: platform-tools`; el SDK 35 y Build Tools 35.0.0 se instalan en el paso siguiente.

La segunda ejecución ([42696267100](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696267100)) superó la preparación del SDK y confirmó el fallo de la prueba de XML truncado. La corrección añadió validación de la raíz RSS y su cierre completo.

### Ejecución final exitosa

- [GitHub Actions 42696845362](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696845362): estado `completed`, conclusión **success**.
- Commit de código compilado: `53113e3d4bc6a41fea4dcae6090a1182bf22f291`.
- Compilación, pruebas unitarias y Lint completados en el runner de GitHub con Java 17.
- [Artefacto `nyaa-neo-debug`](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696845362/artifacts/13709293557): APK y checksum, disponible hasta el 9 de diciembre de 2026.
- [Artefacto `android-reports`](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696845362/artifacts/13709328624): informes de pruebas y Lint, disponible hasta el 23 de noviembre de 2026.

Se descargó el ZIP real mediante el conector GitHub y se comprobó:

```text
SHA-256 del ZIP (coincide con el digest de GitHub):
9029972af66af593e38a9633b99e66137b62fa5abc6bb6d5aabc2f24c1919412

SHA-256 de nyaa-neo-debug.apk (coincide con el archivo .sha256 incluido):
98f091cef64876c663da4ec9cef2aa3d77753799b162ff584f60268b3b66a451

apksigner verify --verbose: Verifies; APK Signature Scheme v2: true
aapt dump badging: dev.nyaaneo; versionName 0.1.0; minSdk 26; targetSdk 35
```

La actualización final de enlaces y evidencia es un commit de documentación con `[skip ci]`: no cambia el código compilado ni requiere repetir la compilación.
