# Cambios

## 0.2.0

- Idiomas destacados en tarjetas, lista compacta y detalles, con filtro local por español, inglés, multi-sub/audio, raw y otros/sin indicar.
- Detección orientativa desde el título; se diferencia la categoría inglesa de Nyaa de un idioma explícito. Multi-sub no se interpreta automáticamente como español ni raw como audio japonés.
- Fansub/grupo extraído del prefijo del título, omitiendo etiquetas técnicas e idiomas.
- Usuario real de publicación consultado desde el detalle de Nyaa, separado del grupo del archivo; también se muestra cuando la publicación es anónima.
- Carátulas orientativas de anime desde Kitsu con Coil, placeholders y ajuste persistente para desactivarlas. Solo se buscan para anime, con coincidencia de títulos, caché y consultas espaciadas.
- Contadores destacados de seeds, leechers y descargas; orden por leechers y actualización de estadísticas desde el detalle.
- Los favoritos mantienen compatibilidad con 0.1.0 y pueden conservar el usuario y los datos actualizados cuando se consulta su detalle.
- Pruebas de idiomas/grupos, búsqueda de carátulas, parser HTML real y compatibilidad de favoritos.

## 0.1.0

Se inicia el proyecto desde cero: no había repositorio ni código previo disponible.

- Configuración Android nativa con Kotlin, Compose, Material 3 y MVVM.
- Búsqueda RSS con categorías y filtros, cancelación de consultas y errores recuperables.
- Parser con namespaces, entidades XML, CDATA y validación de ID/info hash; rechaza DTD.
- Orden local explícito, tarjetas y lista compacta.
- Favoritos con persistencia atómica en DataStore y detalle disponible sin red.
- Detalles, apertura externa y copia de magnet, enlace a la página original.
- Navegación inferior y tema AMOLED negro/cian.
- GitHub Actions para compilar APK debug, ejecutar pruebas/Lint y publicar artefactos.
- Gradle Wrapper y documentación de instalación desde una tablet.
- Pruebas de RSS, codificación de búsquedas, orden y serialización de favoritos.
- Corrección tras las pruebas: compatibilidad entre proveedores XML al desactivar DTD, conservando su rechazo explícito.
- Corrección tras la primera ejecución de Actions: eliminar la instalación implícita del paquete obsoleto Android `tools`.
- Rechazo explícito de HTML y RSS truncado; 15 pruebas unitarias verificadas.
- Corrección de compatibilidad API 26 en las barras del sistema y reglas explícitas de exclusión de backup/transferencia en Android 12+.
- Compilación local, pruebas y Android Lint completados; firma APK v2 comprobada.
