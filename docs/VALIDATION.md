# Verificación

Este archivo documenta evidencia real. No sustituye una prueba de uso en Android.

## Entorno

- Linux x86_64; Eclipse Temurin JDK 17.0.21.
- SDK oficial Android 35 y Build Tools 35.0.0 instalados para esta sesión.
- Configuración de GitHub Actions: Ubuntu, Temurin 17 y SDK Android 35.

El primer intento detectó que el Java 21 preinstalado era un runtime sin compilador (`JAVA_COMPILER`). Se instaló un JDK 17 completo en el workspace y se repitió la compilación. La configuración del proyecto no depende de las rutas locales de este entorno.

## Comprobaciones en curso

Se está ejecutando la compilación de depuración, las pruebas unitarias y Android Lint. Se actualizará este apartado con el resultado final antes de entregar el proyecto.

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
