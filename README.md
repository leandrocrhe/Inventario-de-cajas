# 📦 Inventario de Cajas

**Inventario de Cajas** es una aplicación móvil nativa para Android diseñada para organizar, clasificar y rastrear objetos y contenedores de manera jerárquica y visual. Permite gestionar cajas dentro de otras cajas sin límite de profundidad, garantizando la consistencia estructural mediante algoritmos de prevención de ciclos, además de ofrecer control de préstamos, estadísticas, búsqueda en tiempo real y copias de seguridad.

---

## 🌟 Características Principales

- 📁 **Organización Jerárquica Ilimitada**:
  - Crea cajas maestras y subcajas con cualquier nivel de anidación.
  - Navega cómodamente con una barra de ruta interactiva (**Breadcrumb**).
  - Mueve cajas y objetos entre contenedores con validación en tiempo real.

- 🛡️ **Prevención Matemática de Ciclos (`MoveBoxUseCase`)**:
  - Algoritmo que detecta y bloquea cualquier intento de mover una caja dentro de sí misma o dentro de sus propios descendientes, evitando referencias circulares o pérdida de visibilidad en el árbol de contenedores.

- 🏷️ **Gestión Completa de Objetos**:
  - Registra artículos con nombre, cantidad, categoría, notas y fotografías.
  - Integración nativa con el selector de fotos de Android (**Android Photo Picker**), sin requerir permisos invasivos de almacenamiento.

- 🤝 **Control y Seguimiento de Préstamos**:
  - Asigna artículos prestados indicando el nombre de la persona, fecha del préstamo y estado.
  - Indicadores visuales en tarjetas y lista dedicada de préstamos activos.

- 🔍 **Búsqueda Global en Tiempo Real**:
  - Encuentra cajas, artículos y categorías rápidamente mediante coincidencias en nombre, notas o etiquetas.

- 📊 **Panel de Estadísticas**:
  - Resumen visual con conteo total de cajas, artículos almacenados, valor/cantidad y distribución por categoría.

- 💾 **Respaldos y Restauración (Backup & Restore)**:
  - Exporta todo el inventario (cajas, objetos, categorías y préstamos) a archivos JSON locales.
  - Restaura copias de seguridad preservando la integridad referencial.

- 🎨 **Experiencia de Usuario Material Design 3**:
  - Soporte para vista en cuadrícula (**Grid**) y vista en lista (**List**).
  - Temas visuales personalizables, paleta dinámica y compatibilidad con modo oscuro y modo claro.
  - Navegación fluida con soporte para gestos del sistema y botón atrás (**BackHandler**).

---

## 🛠️ Tecnologías y Arquitectura

- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **Interfaz Gráfica**: [Jetpack Compose](https://developer.android.com/jetpack/compose) con **Material 3**
- **Persistencia Local**: [Room Database](https://developer.android.com/training/data-storage/room) con Kotlin Coroutines y `Flow`
- **Carga de Imágenes**: [Coil Compose](https://coil-kt.github.io/coil/compose/)
- **Serialización**: [Moshi](https://github.com/square/moshi) con codegen
- **Arquitectura**: MVVM + Clean Architecture:
  - `data/local`: Entidades y DAOs de Room (`BoxEntity`, `ItemEntity`, `CategoryEntity`, etc.)
  - `data/repository`: Implementación de repositorios de inventario y gestión de copias de seguridad.
  - `domain/usecase`: Casos de uso de negocio (ej. detección de ciclos en movimientos).
  - `presentation`: Componentes de interfaz reactiva (`MainInventoryScreen`, diálogos, hojas inferiores y tarjetas).
  - `ui/theme`: Configuración de tipografía, colores y formas Material 3.
- **Testing**: Pruebas unitarias con JUnit y pruebas JVM simuladas con [Robolectric](https://robolectric.org/).

---

## 📂 Estructura del Proyecto

```text
app/src/main/java/com/example/
├── MainActivity.kt                       # Punto de entrada de la aplicación
├── data/
│   ├── backup/BackupManager.kt           # Exportación e importación JSON
│   ├── local/
│   │   ├── AppDatabase.kt                # Base de datos Room
│   │   ├── dao/                          # BoxDao, ItemDao, CategoryDao
│   │   └── entity/                       # BoxEntity, ItemEntity, CategoryEntity
│   └── repository/InventoryRepository.kt # Acceso unificado a datos
├── domain/
│   └── usecase/MoveBoxUseCase.kt         # Lógica de prevención de ciclos
├── presentation/
│   ├── MainInventoryScreen.kt            # Pantalla principal con navegación
│   ├── InventoryViewModel.kt             # Manejo de estado con StateFlow
│   └── components/                       # Componentes modulares de Compose:
│       ├── BackupDialog.kt
│       ├── BoxItemCards.kt
│       ├── BreadcrumbBar.kt
│       ├── DeleteConfirmDialog.kt
│       ├── EditBoxDialog.kt
│       ├── EditItemDialog.kt
│       ├── LoansSheet.kt
│       ├── MoveDialogs.kt
│       ├── PhotoPickerSection.kt
│       ├── SearchScreen.kt
│       └── StatsDashboardDialog.kt
└── ui/theme/                             # Color, Type y Theme M3
```

---

## 🚀 Compilación y Ejecución

### Requisitos
- Android Studio Ladybug / Meerkat o superior (o entorno con Gradle 9.3+ y JDK 21).
- `minSdk`: 24 (Android 7.0)
- `targetSdk` / `compileSdk`: 36 (Android 16)

### Comandos útiles

Compilar la aplicación en modo depuración (Debug APK):
```bash
gradle assembleDebug
```

Ejecutar las pruebas unitarias y de Robolectric:
```bash
gradle :app:testDebugUnitTest
```

---

## 📄 Licencia

Este proyecto se distribuye bajo los términos acordados en su repositorio de origen.
