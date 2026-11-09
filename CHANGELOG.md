# Cambios

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
