# Verificación

Este archivo documenta evidencia real. No sustituye una prueba de uso en Android.

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

La primera ejecución ([42696047697](https://github.com/greenOwl-n/nyaa-neo/actions/runs/42696047697)) falló en `android-actions/setup-android@v3`: su configuración por defecto intentaba instalar el paquete obsoleto `tools` (`Failed to find package 'tools'`). Se configuró explícitamente `packages: platform-tools`; el SDK 35 y Build Tools 35.0.0 se instalan en el paso siguiente. Pendiente de verificar la nueva ejecución y su artefacto.
