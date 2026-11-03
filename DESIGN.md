# 🎨 DESIGN.md — Sistema de Diseño "Inventario de Cajas" (Stitch with Google)

Documento de especificación de diseño visual, arquitectura de interfaz y tokens del sistema optimizado para su uso con **Stitch with Google** y la implementación en **Jetpack Compose + Material Design 3 (M3)**.

---

## 📌 1. Visión y Dirección Creativa (Creative Intent)

### 🏷️ Concepto Central: *Tactile Storage & Warm Utility*
Alejándose de interfaces frías, genéricas y grises ("AI slop"), el diseño de **Inventario de Cajas** está inspirado en la calidez de los materiales de almacenamiento clásicos (madera, cartón kraft, ámbar, cuero y terracota) combinado con la precisión de **Google Material Design 3**.

- **Atmósfera**: Cálida, táctil, acogedora y ordenada.
- **Jerarquía Visual**: Clara distinción entre contenedores maestros (cajas), compartimentos (subcajas) y contenidos (objetos).
- **Densidad de Información**: Espaciosa y equilibrada. Nunca se satura la pantalla ni se recortan textos de forma abrupta; cada elemento respeta la cuadrícula base de 8dp.

---

## 🎛️ 2. Tokens del Sistema de Diseño (M3 Tokens)

### 🎨 2.1 Paleta de Color (Color Palette)

#### Tema Claro (Warm Light Mode)
| Token M3 | Nombre Conceptual | Hex | Rol en la Interfaz |
|---|---|---|---|
| `md.sys.color.primary` | Amber 40 | `#8B4A00` | Acciones principales, botones destacados, FAB |
| `md.sys.color.on-primary` | White | `#FFFFFF` | Texto/iconos sobre color primario |
| `md.sys.color.primary-container` | Amber Light Tint | `#FFDCC1` | Fondos de chips activos, badges suaves |
| `md.sys.color.on-primary-container` | Dark Amber Deep | `#2E1500` | Texto sobre contenedores primarios |
| `md.sys.color.secondary` | Amber Grey 40 | `#6F5B40` | Elementos de apoyo, etiquetas secundarias |
| `md.sys.color.secondary-container` | Soft Sand Tint | `#FBE0BD` | Tarjetas de subcategorías y filtros neutros |
| `md.sys.color.tertiary` | Terracotta 40 | `#904A3E` | Indicadores de alerta suave, acciones de movimiento |
| `md.sys.color.background` | Warm Porcelain | `#FBF9F5` | Fondo global de la aplicación (no blanco puro) |
| `md.sys.color.surface` | Clean Card Paper | `#FFFFFF` | Superficie de tarjetas y barras de herramientas |
| `md.sys.color.surface-variant` | Warm Cream Neutral | `#F2EFE9` | Contenedores neutros, divisores, chips inactivos |
| `md.sys.color.on-surface` | Deep Espresso | `#211A16` | Tipografía principal (alto contraste legibilidad) |
| `md.sys.color.on-surface-variant` | Warm Slate Brown | `#5C554F` | Tipografía de subtítulos, fechas y contadores |

#### Tema Oscuro (Warm Dark Mode)
| Token M3 | Nombre Conceptual | Hex | Rol en la Interfaz |
|---|---|---|---|
| `md.sys.color.primary` | Amber 80 | `#FFB77C` | Acciones principales en noche (alta luminancia) |
| `md.sys.color.on-primary` | Dark Roast 20 | `#4D2700` | Texto/iconos sobre primario oscuro |
| `md.sys.color.primary-container` | Amber 30 | `#683500` | Contenedores activos tenues |
| `md.sys.color.background` | Deep Walnut Black | `#171412` | Fondo de pantalla noche orgánico |
| `md.sys.color.surface` | Roasted Cocoa Surface | `#221F1C` | Tarjetas elevadas y barra superior |
| `md.sys.color.surface-variant` | Muted Charcoal Wood | `#322E2A` | Chips inactivos y bordes tenues |
| `md.sys.color.on-surface` | Warm Sand White | `#ECE0DA` | Tipografía principal de lectura nocturna |

#### Etiquetas Semánticas de Cajas (Box Tags)
Tokens cromáticos para categorización visual inmediata de contenedores:
- `box.tag.amber`: `#F59E0B` (General / Herramientas)
- `box.tag.blue`: `#3B82F6` (Tecnología / Cables)
- `box.tag.green`: `#10B981` (Hogar / Jardín / Salud)
- `box.tag.purple`: `#8B5CF6` (Documentos / Archivos)
- `box.tag.red`: `#EF4444` (Frágil / Prioritario)
- `box.tag.teal`: `#14B8A6` (Ropa / Temporada)
- `box.tag.slate`: `#64748B` (Misceláneo / Almacén)

---

### 🔤 2.2 Sistema Tipográfico (Typography Scale)

Diseñado para cumplir estándares de accesibilidad WCAG AAA y evitar truncamientos o desbordes:

| Nivel M3 | Familia Sugerida | Peso | Tamaño (sp) | Interlineado (sp) | Uso Principal |
|---|---|---|---|---|---|
| `displayLarge` | Google Sans / Roboto Flex | Bold (700) | 32sp | 38sp | Números destacados en Estadísticas |
| `headlineMedium` | Google Sans | SemiBold (600) | 24sp | 30sp | Títulos de diálogos y hojas modales |
| `titleLarge` | Google Sans | Bold (700) | 20sp | 26sp | Título de la caja actual o pantalla |
| `titleMedium` | Google Sans | Medium (500) | 16sp | 22sp | Títulos en tarjetas de cajas y objetos |
| `bodyLarge` | Roboto / System | Regular (400) | 16sp | 24sp | Descripciones extensas y notas |
| `bodyMedium` | Roboto / System | Regular (400) | 14sp | 20sp | Texto secundario y etiquetas de campo |
| `labelLarge` | Google Sans | Medium (500) | 14sp | 20sp | Texto en botones y chips de acción |
| `labelSmall` | Roboto | Medium (500) | 11sp | 16sp | Metadatos, conteos y badges |

> **Regla de Estructura**: Todo texto de título (`titleMedium`, `titleLarge`) y subtítulo (`labelSmall`, `bodySmall`) dentro de barras o tarjetas debe contar con `maxLines = 1` (o `2` en descripciones) y `overflow = TextOverflow.Ellipsis`.

---

### 📐 2.3 Espaciado y Cuadrícula (Spacing & Elevation)

- **Grid Base**: 8dp (micro-espacios a 4dp).
- **Márgenes de Pantalla**: 16dp horizontal estándar (24dp en tablets).
- **Separación entre Tarjetas**: 12dp en cuadrícula, 8dp en listas.
- **Esquinas Redondeadas (Corner Radii)**:
  - Chips / Badges: `8dp`
  - Tarjetas de Cajas / Objetos: `16dp`
  - Diálogos y Modales: `24dp`
  - Botones Flotantes (FAB): `16dp`
- **Elevaciones Tonales M3**:
  - `Level 0 (Flat)`: Fondo principal (0dp).
  - `Level 1 (Subtle)`: Barra superior y tarjetas en reposo (1dp a 2dp).
  - `Level 2 (Hover/Active)`: Tarjetas presionadas o resaltadas (3dp).
  - `Level 3 (Floating/Modal)`: Diálogos, Hojas de Préstamos y FAB (6dp).

---

## 🧩 3. Arquitectura de Componentes

### 3.1 Encabezado de Dos Niveles (Two-Tier Top Header)
Estructura diseñada para eliminar desbordamientos y permitir interacción ágil:
```text
┌──────────────────────────────────────────────────────────────────┐
│ [Logo/Back]  Inventario de Cajas                 [Search] [View] │
│              5 cajas · 12 objetos (24 uds)                       │
├──────────────────────────────────────────────────────────────────┤
│ (🤝 Préstamos [2])  (📊 Estadísticas)  (💾 Copias de seguridad)   │
└──────────────────────────────────────────────────────────────────┘
```
- **Nivel Superior**: Identidad de marca + Título elástico (`weight(1f)`) + Búsqueda directa + Conmutador Cuadrícula/Lista.
- **Nivel Inferior (Scroll Horizontal)**: Chips interactivos con iconos y texto explicativo para herramientas clave sin saturar la barra de título.

### 3.2 Migas de Pan (Breadcrumb Trail)
- Visualización jerárquica con desplazamiento horizontal automático al añadir profundidad.
- Separadores de dirección sutiles (`Icons.AutoMirrored.Filled.ArrowForwardIos`).
- Estado raíz diferenciado con icono de inicio (`Icons.Default.Home`).

### 3.3 Tarjetas de Contenedores y Artículos (Cards)
- **Vista Cuadrícula (Grid)**:
  - Proporción 1:1 o 4:3 para imágenes.
  - Indicador de color semántico en el borde superior o icono.
  - Insignias flotantes para cantidad y conteo de subelementos.
- **Vista Lista (List)**:
  - Fila horizontal de 72dp de altura mínima con objetivo táctil de 48dp.
  - Miniatura a la izquierda, título y notas al centro, stepper de cantidad y menú contextual a la derecha.

---

## 💫 4. Microinteracciones y Animaciones

- **Transición de Jerarquía**: Animación compartida de contenedor (Container Transform) de 250ms al entrar a una subcaja.
- **Respuesta Háptica y Ripple**: Toda superficie interactiva cuenta con indicación de onda Material M3 (`Modifier.clickable`) con radio delimitado.
- **Física de Rebote**: Movimientos de chips y tarjetas con curva `spring(dampingRatio = Spring.DampingRatioLowBouncy)`.

---

## 🤖 5. Prompts para "Stitch with Google" (Ready to Use)

Utiliza estos prompts en la herramienta **Stitch with Google** para generar maquetas visuales, assets y prototipos alineados:

### Prompt 1: Rediseño de Pantalla Principal
```text
Prompt for Stitch with Google:
Generate a modern Material Design 3 mobile UI for an Android app called "Inventario de Cajas" (Storage & Box Inventory).
Aesthetic: Warm, tactile, crafted utility. Palette includes warm amber (#8B4A00), soft parchment background (#FBF9F5), terracotta accents (#904A3E), and warm surface cards (#FFFFFF).
Header: A clean two-tiered surface header. The first row has a rounded 3D cardboard box logo, bold title "Inventario de Cajas" with subtitle "8 cajas · 24 objetos", plus minimalist search and grid/list icons on the right. The second row contains horizontal rounded action chips: "Préstamos" with a notification badge "3", "Estadísticas", and "Respaldos".
Content: A 2-column grid showing isometric cardboard/storage box cards with customizable color tag pills, item count badges, and smooth rounded corners (16dp).
Floating Action Button: Extended Amber FAB at bottom right with "+ Nueva Caja".
Clean, spacious, zero text clutter, adhering to Android 16 guidelines.
```

### Prompt 2: Ficha Detallada de Objeto y Préstamos
```text
Prompt for Stitch with Google:
Design a bottom sheet modal in Material Design 3 for tracking an inventory item called "Taladro Inalámbrico 20V".
Style: Warm minimalist Android interface with 24dp top corner radius.
Sections:
1. Header with photo preview, item title, and category chip "Herramientas".
2. Quantity controller (+ / -) with a clean pill stepper.
3. Loan Status Card (Estado de Préstamo): Highlighted in warm blue-gray tint with borrower name "Carlos Morales", date "12 Oct 2026", and a toggle switch or button "Marcar como devuelto".
4. Action buttons: "Mover a otra caja", "Editar", and "Eliminar".
Clear typography, generous 16dp padding, and accessible 48dp touch targets.
```

---

## 📋 6. Checklist de Cumplimiento de Diseño

- [x] **Sin desbordes**: Todo texto dinámico tiene elipsis y límite de líneas.
- [x] **Accesibilidad**: Áreas táctiles mínimas de 48x48 dp en todos los iconos y chips.
- [x] **Safe Areas**: Soporte para barra de estado (`statusBarsPadding()`) y navegación gestual del sistema.
- [x] **Tema Dual**: Contraste adecuado tanto en modo claro cálido como en modo oscuro.
- [x] **Consistencia**: Unicidad en la iconografía usando Material Symbols / Icons consistentes.
