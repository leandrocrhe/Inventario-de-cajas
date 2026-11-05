---
name: Tactile Storage & Warm Utility
colors:
  surface: '#fff8f4'
  surface-dim: '#e3d8d0'
  surface-bright: '#fff8f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fdf2e9'
  surface-container: '#f7ece4'
  surface-container-high: '#f1e6de'
  surface-container-highest: '#ebe1d9'
  on-surface: '#201b16'
  on-surface-variant: '#534438'
  inverse-surface: '#352f2a'
  inverse-on-surface: '#faefe7'
  outline: '#867467'
  outline-variant: '#d9c2b3'
  surface-tint: '#8f4d04'
  primary: '#693600'
  on-primary: '#ffffff'
  primary-container: '#FFDCC1'
  on-primary-container: '#2E1500'
  inverse-primary: '#ffb77d'
  secondary: '#6f5b40'
  on-secondary: '#ffffff'
  secondary-container: '#FBE0BD'
  on-secondary-container: '#745f44'
  tertiary: '#6f3026'
  on-tertiary: '#ffffff'
  tertiary-container: '#8c473b'
  on-tertiary-container: '#ffc6bb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdcc3'
  primary-fixed-dim: '#ffb77d'
  on-primary-fixed: '#2f1500'
  on-primary-fixed-variant: '#6e3900'
  secondary-fixed: '#fadebc'
  secondary-fixed-dim: '#ddc2a1'
  on-secondary-fixed: '#271904'
  on-secondary-fixed-variant: '#56442a'
  tertiary-fixed: '#ffdad4'
  tertiary-fixed-dim: '#ffb4a7'
  on-tertiary-fixed: '#3b0904'
  on-tertiary-fixed-variant: '#733429'
  background: '#fff8f4'
  on-background: '#201b16'
  surface-variant: '#F2EFE9'
  background-warm: '#FBF9F5'
  surface-clean: '#FFFFFF'
  on-surface-espresso: '#211A16'
  tag-amber: '#F59E0B'
  tag-blue: '#3B82F6'
  tag-green: '#10B981'
  tag-purple: '#8B5CF6'
  tag-red: '#EF4444'
  tag-teal: '#14B8A6'
  tag-slate: '#64748B'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 38px
  headline-md:
    fontFamily: Roboto Flex
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 30px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 26px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 22px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-tablet: 1rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

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
