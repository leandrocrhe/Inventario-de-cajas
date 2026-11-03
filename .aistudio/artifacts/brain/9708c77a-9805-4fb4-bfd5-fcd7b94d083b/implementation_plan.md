# Diseño del Logo e Icono Adaptativo: Inventario de Cajas

Diseño y generación del logotipo oficial e icono adaptativo para Android (Adaptive Icon) basado en una composición 3D isométrica premium con cajas translúcidas anidadas en tonalidades ámbar cálido, oro y terracota.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Estilo visual:** 3D isométrico moderno con cajas modulares anidadas (caja principal abierta revelando subcajas organizadas con acabados mate y acrílico elegante).
> - **Paleta de color:** Ámbar cálido (#F59E0B / #C25E00) con acentos dorados suaves (#FBBF24) y base terracota (#904A3E / #1E1B18), en armonía con el tema visual de la aplicación.
> - **Formato técnico:** Icono adaptativo nativo de Android (Android 8.0+) con capa de primer plano (Foreground de 66dp centrada en lienzo de 108dp), fondo sólido coordinado (Background) y versiones rasterizadas de alta resolución para todas las densidades (mdpi a xxxhdpi) con soporte para icono circular (`ic_launcher_round.png`).

---

### 1. Overview & Core Concept

- **Qué representa:** El logotipo simboliza la esencia de la app: organización jerárquica modular, donde una caja contenedora alberga ordenadamente cajas secundarias y elementos clave con precisión milimétrica.
- **Audiencia:** Usuarios que buscan una herramienta visual, profesional y de alta estética para catalogar sus pertenencias.
- **Impacto visual:** Sustituye el icono genérico de Android por una identidad de marca reconocible y distintiva en el lanzador de aplicaciones del sistema.

---

### 2. User Experience & Visual Design

#### Composición Gráfica

- **Geometría Isométrica:** Perspectiva oblicua a 30 grados que muestra tres caras con iluminación cenital suave y sombras de contacto sutiles.
- **Elementos de la escena:**
  - Una caja exterior de líneas depuradas en tono ámbar y madera tratada.
  - Dos subcajas interiores en contraste (una en terracota satinado y otra con acabado translúcido tipo policarbonato).
  - Un pequeño distintivo de etiqueta de inventario en tono oro sutil.
- **Color de fondo fijo (`#1E1B18`):** Fondo oscuro cálido grafito que maximiza el contraste y la profundidad tridimensional del isotipo.

#### Especificaciones de Iconografía Android

- **Safe Zone (66dp / 108dp):** El motivo 3D principal se mantendrá dentro del círculo central de 66dp para evitar recortes al aplicar máscaras circulares, redondeadas o cuadradas en diferentes fabricantes (Pixel, Samsung One UI, Xiaomi).
- **Icono Circular (`ic_launcher_round.png`):** Generado con máscara circular alfa limpia para dispositivos que utilizan iconos circulares por defecto.

---

### 3. Key Product Decisions & Trade-Offs

- **Generación mediante IA de Imagen con Color Pinned vs. Vector Plano Manual:**
  - *Decisión elegida:* Emplear `generate_image` con un prompt ultra-detallado de renderizado 3D isométrico fijando exactamente el color de fondo `#1E1B18`.
  - *Por qué:* Permite texturas, iluminación volumétrica y reflejos de calidad de estudio que no son reproducibles con simples trazados SVG de vectores planos.
- **Conversión de Densidades con ImageMagick (`convert`):**
  - *Decisión elegida:* Redimensionar el activo de primer plano a las 5 densidades oficiales de Android (`mdpi:48px`, `hdpi:72px`, `xhdpi:96px`, `xxhdpi:144px`, `xxxhdpi:192px`) eliminando los antiguos `.webp` por defecto para evitar colisiones de recursos AAPT2.
  - *Por qué:* Asegura nitidez perfecta tanto en pantallas de baja densidad como en pantallas AMOLED QHD+.

---

### 4. Technical Architecture & Data Strategy

#### Diagrama de Recursos del Icono

```
┌────────────────────────────────────────────────────────┐
│               Android Adaptive Icon Pipeline           │
│                                                        │
│  ┌───────────────────────────┐                         │
│  │    generate_image         │ (Asset 3D Isométrico)   │
│  │  app_logo_cajas_3d.png    │ Background: #1E1B18     │
│  └─────────────┬─────────────┘                         │
│                │                                       │
│                ├──► res/drawable/ic_launcher_foreground.xml (66dp safe zone)
│                ├──► res/drawable/ic_launcher_background.xml (#1E1B18 sólido)
│                │                                       │
│                ▼ ImageMagick Processing                │
│  ┌──────────────────────────────────────────────────┐  │
│  │ res/mipmap-anydpi-v26/ic_launcher.xml            │  │
│  │ res/mipmap-anydpi-v26/ic_launcher_round.xml      │  │
│  │                                                  │  │
│  │ Densidades Raster (PNG32):                       │  │
│  │  - mipmap-mdpi (48x48)      - mipmap-xhdpi (96x96)│  │
│  │  - mipmap-hdpi (72x72)      - mipmap-xxhdpi(144) │  │
│  │  - mipmap-xxxhdpi (192x192) + versiones _round   │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

#### Fases de Implementación tras Aprobación

1. **Generación del activo maestro:** Creación de `app_logo_cajas_3d` mediante `generate_image` con aspecto 1:1 y fondo fijado `#1E1B18`.
2. **Configuración de capas vectoriales adaptativas:**
   - `res/drawable/ic_launcher_background.xml` con `<color>#1E1B18</color>`.
   - `res/drawable/ic_launcher_foreground.xml` con `<layer-list>` y tamaño seguro de `66dp`.
3. **Generación de fallbacks rasterizados:** Ejecución del script ImageMagick para procesar las 5 carpetas `mipmap-*` y eliminar los archivos `.webp` antiguos.
4. **Verificación:** Ejecución de `compile_applet` y validación en el emulador de Android.
