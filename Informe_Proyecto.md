# Informe Técnico del Proyecto ControlMedicamentos

**Aplicación móvil Android para gestión integral de salud personal y familiar**

---

## Datos generales

| Campo | Valor |
|-------|-------|
| **Título del proyecto** | ControlMedicamentos |
| **Autor** | Carlos [Apellido del desarrollador] |
| **Universidad / Centro** | [Nombre de la universidad] |
| **Carrera / Asignatura** | [Nombre de la carrera o asignatura] |
| **Tutor / Evaluador** | [Nombre del tutor] |
| **Fecha de entrega** | 1 de julio de 2026 |
| **Periodo de desarrollo** | Junio 2025 – Junio 2026 (12 meses) |
| **Versión de la aplicación** | 1.0.0 (versionCode 25) |
| **Código fuente** | Disponible en el repositorio local `d:/Controlmedicamentos` |

---

## Resumen ejecutivo

ControlMedicamentos es una aplicación móvil nativa para Android desarrollada en **Kotlin** con **Jetpack Compose**. Su objetivo es permitir a usuarios y usuarias gestionar de forma centralizada medicamentos, citas, contactos de salud, métricas diarias, ciclos menstruales, embarazos, controles pediátricos, vacunación, actividad física, compras y un diario personal. El proyecto incluye funcionalidades diferenciadas para personas de sexo masculino y femenino, respaldo local, exportación a documentos Word y un asistente de inteligencia artificial integrado.

El desarrollo abarcó aproximadamente **12 meses** de trabajo iterativo, resultando en unos **30 000+ líneas de código fuente** distribuidas en más de 100 archivos Kotlin, archivos de recursos XML y configuración Gradle. La aplicación se compila con **Android SDK 36**, utiliza **Room** como capa de persistencia sobre SQLite, y emplea más de 20 entidades relacionales para cubrir los distintos módulos de salud.

---

## 1. Introducción

### 1.1 Planteamiento del problema

Muchas personas requieren controlar medicamentos diarios, citas médicas, signos vitales y, en el caso de mujeres, ciclos menstruales, embarazos y métodos anticonceptivos. Las apps existentes suelen especializarse en una sola área (por ejemplo, solo recordatorios de pastillas o solo calendario menstrual), obligando al usuario a usar varias herramientas. Además, la pérdida de datos o la falta de respaldo local complica el uso a largo plazo.

### 1.2 Objetivo general

Desarrollar una aplicación Android integral que permita gestionar salud personal y familiar, incluyendo medicamentos, citas, contactos, métricas diarias, salud reproductiva, pediátrica y física, con respaldo, exportación y un asistente IA.

### 1.3 Objetivos específicos

- Gestión de perfiles de paciente con datos personales y foto.
- Registro y seguimiento de medicamentos con recordatorios y control de stock.
- Calendario de citas y directorio de contactos de salud.
- Registro de métricas diarias: presión, latidos, glucemia, temperatura, peso e IMC.
- Seguimiento del ciclo menstrual, embarazo, anticonceptivos y salud reproductiva.
- Control pediátrico: niños, vacunación y controles de crecimiento.
- Registro de actividad física con GPS y mapa.
- Carrito de compras/pedidos de insumos con envío por WhatsApp.
- Diario personal con imágenes.
- Asistente IA para consultas y resúmenes.
- Respaldo local y exportación a Word.

---

## 2. Justificación

Una aplicación unificada reduce la fricción del usuario, mejora la adherencia a tratamientos y permite correlacionar datos (por ejemplo, ver cómo un cambio de peso o ciclo menstrual afecta otros indicadores). El uso de **Android nativo** garantiza acceso a notificaciones locales, alarmas, cámara, GPS y almacenamiento cifrado, elementos clave para una app de salud personal.

---

## 3. Alcance y límites

### 3.1 Alcance

- Aplicación funcional instalable en dispositivos Android con API 24+ (Android 7.0 en adelante).
- Múltiples perfiles de paciente dentro de un mismo dispositivo.
- Recordatorios locales con alarmas exactas.
- Persistencia offline mediante Room.
- Exportación de resúmenes a Word (.docx).
- Copias de seguridad locales restaurables.

### 3.2 Límites

- No es una aplicación médica certificada; no reemplaza el diagnóstico profesional.
- La sincronización en la nube es opcional/controlada por el usuario.
- Requiere permisos de notificación, cámara, ubicación y almacenamiento según la función.

---

## 4. Tecnologías y herramientas utilizadas

### 4.1 Lenguajes de programación

| Lenguaje | Uso principal | Porcentaje aproximado |
|----------|---------------|----------------------|
| **Kotlin** | Lógica de negocio, UI con Jetpack Compose, DAOs, modelos, notificaciones, sincronización | ~92 % |
| **XML** | Recursos de Android: layouts auxiliares, strings, temas, menús, manifest | ~5 % |
| **Gradle / Kotlin DSL** | Configuración de compilación, dependencias, versiones, firma | ~2 % |
| **Markdown** | Documentación técnica (DOSSIER, informes) | ~1 % |

### 4.2 Frameworks, librerías y SDK

| Tecnología | Versión / Detalle | Propósito |
|------------|-------------------|-----------|
| Android SDK | API 24 (min) – API 36 (compile/target) | Plataforma móvil |
| Jetpack Compose | BOM 2024.09.00 | Interfaz de usuario declarativa |
| Material 3 | Compose Material3 | Diseño visual y componentes |
| Navigation Compose | 2.8.5 | Navegación entre pantallas |
| Room | 2.7.0 | Persistencia local con SQLite |
| KSP | 2.2.10-2.0.2 | Procesador de anotaciones para Room |
| Kotlin Coroutines | 1.8.1+ | Operaciones asíncronas |
| WorkManager | 2.10.0 | Tareas en segundo plano |
| Health Connect | 1.1.0 | Integración con datos de salud del dispositivo |
| ML Kit Text Recognition | 16.0.1 | Escaneo de textos en documentos |
| ML Kit Document Scanner | 16.0.0-beta1 | Escaneo de documentos médicos |
| OSMDroid | 6.1.18 | Mapas OpenStreetMap para actividad física |
| Play Services Location | 21.3.0 | GPS y ubicación en tiempo real |
| Security Crypto | 1.1.0-alpha06 | Almacenamiento cifrado de datos sensibles |
| Apache POI (via módulo exportador) | Generación de archivos .docx | Exportación de resúmenes |

### 4.3 Herramientas de desarrollo

- **Android Studio** o IDE compatible con Kotlin y Gradle.
- **Gradle** 9.4.1 con Android Gradle Plugin 9.0.1.
- **Git** para control de versiones.
- **Dispositivo físico** Samsung Galaxy SM-S918U (Android 14/15) para pruebas e instalación.

---

## 5. Metodología

El proyecto se desarrolló bajo un enfoque **iterativo e incremental** organizado en fases mensuales:

1. **Fase 1: Análisis y diseño** (meses 1–2). Definición de requisitos, modelado de entidades y prototipos de UI.
2. **Fase 2: Núcleo y persistencia** (meses 3–4). Implementación de Room, DAOs, perfiles de paciente y medicamentos.
3. **Fase 3: Módulos de salud** (meses 5–7). Métricas diarias, citas, directorio, ciclo menstrual, embarazo y pediátrico.
4. **Fase 4: Recordatorios y alarmas** (meses 8–9). Notificaciones, WorkManager, alarmas exactas y registro automático de tomas.
5. **Fase 5: Exportación, backup e IA** (meses 10–11). Exportación a Word, copias de seguridad, asistente IA y sincronización.
6. **Fase 6: Refactor, pulido y despliegue** (mes 12). Ajustes visuales, corrección de bugs, pruebas en dispositivo físico y entrega.

---

## 6. Descripción del sistema

### 6.1 Arquitectura general

La aplicación sigue una arquitectura **monolítica modularizada** en dos proyectos Gradle:

- **`app`**: Módulo principal con UI, lógica de negocio, base de datos, notificaciones y exportación.
- **`sync-core`**: Módulo compartido con modelos de sincronización y utilidades comunes.

Dentro de `app`, el código se organiza en paquetes por responsabilidad:

```
com.carlos.controlmedicamentos
├── data/local          # Entidades Room y DAOs
├── data/remote         # Modelos y servicios de sincronización
├── notifications       # AlarmReceiver, scheduler, canales de notificación
├── backup              # BackupManager para respaldo local
├── sync                # Mapeo de entidades a modelos de sincronización
├── ui/theme            # Temas, colores y tipografía
└── MainActivity.kt     # Punto de entrada principal y gran parte de la UI
```

### 6.2 Funcionalidades para usuarios (hombre)

| Módulo | Descripción |
|--------|-------------|
| **Perfil de paciente** | Creación, edición y foto de perfil. |
| **Medicamentos / Inventario** | Registro de insumos, dosis, frecuencia, stock, tomas, alarmas, suspensión y reactivación. |
| **Dashboard de tomas** | Vista diaria con horarios, estados (pendiente, tomada, vencida, no tomada) y marcado manual. |
| **Directorio de contactos** | Agenda de contactos de salud (médicos, farmacias, etc.) con especialidad, teléfono y citas. |
| **Agenda / Citas** | Programación de citas médicas con recordatorio. |
| **Métricas diarias** | Registro de presión arterial, frecuencia cardíaca, glucosa, temperatura, peso y cálculo de IMC. |
| **Actividad física** | Registro de actividades con GPS, mapa OpenStreetMap, distancia y duración. |
| **Historial de compras / Pedidos** | Carrito de insumos con cálculo de total y envío por WhatsApp. |
| **Diario personal** | Entradas de texto con imágenes adjuntas. |
| **Asistente IA** | Chatbot interno para consultas sobre registros y recomendaciones generales. |
| **Exportar resumen** | Generación de documento Word (.docx) con métricas, inventario, citas y más. |
| **Backup local** | Exportación e importación de la base de datos a almacenamiento interno. |

### 6.3 Funcionalidades adicionales para usuarias (mujer)

| Módulo | Descripción |
|--------|-------------|
| **Ciclo menstrual** | Calendario del ciclo, registro de sangrado, síntomas, predicción de próxima menstruación y ovulación. |
| **Historial de ciclos** | Listado de todos los ciclos registrados con estadísticas. |
| **Control de embarazo** | Seguimiento semanal por FUR, tamaño del bebé, señales de alerta, notas y controles. |
| **Visitas prenatales** | Registro de citas y controles durante el embarazo. |
| **Documentos médicos** | Adjuntos asociados a un embarazo. |
| **Bebé recién nacido** | Registro de datos del parto y del recién nacido. |
| **Métodos anticonceptivos** | Control de píldoras, inyecciones, parches u otros métodos con recordatorios diarios. |
| **Control pediátrico** | Seguimiento de niños: peso, talla, percentiles, controles y vacunación. |
| **Esquema de vacunación** | Vacunas recomendadas por edad y registro de dosis aplicadas. |
| **Enfermedades y alergias** | Registro de antecedentes del paciente. |

### 6.4 Funcionalidades transversales (hombre y mujer)

- Recordatorios y alarmas locales.
- Notificaciones con pantalla completa para emergencias.
- Control de stock con alerta de stock crítico.
- Cálculo de dosis por toma para medicamentos con múltiples horarios.
- Suspensión y reactivación de medicamentos sin pérdida de datos.
- Exportación e importación de copias de seguridad.
- Sincronización controlada entre dispositivos.
- Interfaz adaptativa con estilos metálicos diferenciados por estado (crítico, INSS, suspendido).

---

## 7. Base de datos

La base de datos local se implementa con **Room** y contiene más de **22 entidades** relacionadas:

| Entidad | Propósito |
|---------|-----------|
| `PatientProfile` | Perfiles de paciente. |
| `Medication` / `insumos` | Medicamentos e insumos. |
| `MedicationIntake` | Tomas registradas. |
| `MedicationOrder` | Pedidos de medicamentos. |
| `CarritoPendienteItem` | Items del carrito de compras. |
| `MedicalPractitioner` | Contactos de salud (directorio). |
| `MedicalAppointment` | Citas médicas. |
| `MedicalReport` | Informes médicos. |
| `DocumentoMedico` | Documentos adjuntos. |
| `SignosVitales` | Métricas diarias. |
| `VaccinationRecord` | Registro de vacunación general. |
| `PhysicalActivity` | Actividades físicas. |
| `DiarioEntry` | Entradas del diario personal. |
| `CicloMenstrual` | Ciclos menstruales. |
| `RegistroDiarioCiclo` | Notas diarias del ciclo. |
| `ControlEmbarazo` | Embarazos en curso o históricos. |
| `VisitaPrenatal` | Visitas prenatales. |
| `BebeRecienNacido` | Datos del recién nacido. |
| `MetodoAnticonceptivo` | Métodos anticonceptivos. |
| `AnticonceptivoIntake` | Tomas diarias de anticonceptivos. |
| `NinoEntity` | Niños registrados. |
| `ControlPediatricoEntity` | Controles pediátricos. |
| `VacunaEntity` | Vacunas del esquema pediátrico. |
| `EnfermedadEntity` | Enfermedades y alergias. |

---

## 8. Estadísticas del código fuente

### 8.1 Métricas generales

| Métrica | Valor |
|---------|-------|
| **Archivos fuente Kotlin** | 102 |
| **Archivos de recursos XML** | 14 |
| **Líneas de código Kotlin** | ~30 025 |
| **Líneas de recursos XML** | ~469 |
| **Líneas de configuración Gradle** | ~132 |
| **Total aproximado de líneas de código fuente** | **~30 626** |
| **Archivo más grande** | `MainActivity.kt` (~14 300 líneas) |
| **Módulos Gradle** | 2 (`app`, `sync-core`) |
| **Entidades Room** | 22+ |
| **Dependencias externas** | 20+ |

### 8.2 Distribución por tipo de archivo

```
.kt  : 102 archivos, 30 025 líneas (~98 % del código fuente)
.xml :  14 archivos,    469 líneas (~ 2 % del código fuente)
.kts :   3 archivos,    132 líneas (configuración Gradle)
```

### 8.3 Notas metodológicas

El conteo de líneas excluye directorios de compilación (`build`, `.gradle`, `.kotlin`), entornos virtuales (`.venv`), metadatos de IDE (`.idea`, `.vscode`) y archivos binarios (imágenes, APK, keystore). Incluye únicamente el código fuente legible y mantenible del proyecto.

---

## 9. Cronograma de desarrollo (12 meses)

| Mes | Actividad principal | Entregable |
|-----|---------------------|------------|
| 1 | Definición de requisitos y arquitectura | Documento de especificación, diagrama de entidades |
| 2 | Prototipos de UI y validación de flujo | Prototipos navegables en Compose |
| 3 | Configuración de proyecto, Room y DAOs | Esqueleto funcional con pacientes y medicamentos |
| 4 | Sistema de medicamentos, tomas y stock | Dashboard de medicamentos, alarmas básicas |
| 5 | Citas, directorio y métricas diarias | Agenda, contactos y registro de signos vitales |
| 6 | Ciclo menstrual y control reproductivo | Calendario menstrual, predicciones |
| 7 | Embarazo, anticonceptivos y pediátrico | Seguimiento prenatal, niños, vacunación |
| 8 | Notificaciones, alarmas y WorkManager | Recordatorios robustos con pantalla completa |
| 9 | Actividad física, diario y pedidos | GPS, mapa, carrito y WhatsApp |
| 10 | Backup local, exportación a Word | BackupManager, generador de .docx |
| 11 | Asistente IA, sincronización y pulido | Chatbot, modelos sync, ajustes UX |
| 12 | Pruebas, refactor, correcciones e instalación | APK estable instalado en dispositivo físico |

---

## 10. Pruebas y despliegue

### 10.1 Compilación

- Comando utilizado: `gradlew.bat :app:compileDebugKotlin --no-daemon`
- Resultado: `BUILD SUCCESSFUL`

### 10.2 Instalación en dispositivo

- Comando utilizado: `gradlew.bat :app:installDebug --no-daemon`
- Dispositivo: `SM-S918U` (Samsung Galaxy S23 Ultra / S24 Ultra, Android 14/15)
- Resultado: `Installed on 1 device.`

### 10.3 Pruebas funcionales realizadas

- Marcado y desmarcado de tomas de medicamentos.
- Descuento correcto de stock por toma en medicamentos con múltiples horarios.
- Suspensión y reactivación de medicamentos con estilo visual diferenciado.
- Registro de ciclo menstrual, embarazo y controles pediátricos.
- Exportación de resumen a Word.
- Restauración de backup local.

---

## 11. Resultados obtenidos

- Aplicación funcional, instalable y estable en dispositivo físico.
- Sistema de recordatorios de medicamentos con control de stock.
- Diferenciación clara entre medicamentos activos y suspendidos.
- Cobertura de salud reproductiva, pediátrica y física.
- Generación de documentos Word para entrega a profesionales o archivo personal.
- Respaldo local completo de la base de datos.

---

## 12. Conclusiones

ControlMedicamentos demuestra que es posible construir una aplicación de salud personal completa en Android nativo usando Kotlin y Jetpack Compose. El proyecto integra múltiples dominios de salud en una sola aplicación, ofrece persistencia offline robusta y exporta información útil para el usuario. La modularidad de Room y el uso de alarmas locales permiten un comportamiento fiable incluso sin conexión a internet.

---

## 13. Recomendaciones y trabajo futuro

- Integrar sincronización en la nube opcional (Firebase, Google Drive).
- Añadir gráficos de evolución de métricas, peso y talla infantil.
- Mejorar el asistente IA con modelos locales o servicios en la nube.
- Implementar pruebas unitarias y de interfaz para automatizar la regresión.
- Optimizar la división de `MainActivity.kt` en múltiples archivos por módulo.
- Publicar la aplicación en Google Play Console tras cumplir todas las políticas.

---

## 14. Referencias

- Android Developers. (2026). *Jetpack Compose*. https://developer.android.com/jetpack/compose
- Google. (2026). *Room Persistence Library*. https://developer.android.com/training/data-storage/room
- Kotlin Foundation. (2026). *Kotlin Programming Language*. https://kotlinlang.org
- OpenStreetMap. (2026). *OSMDroid*. https://github.com/osmdroid/osmdroid
- Apache Software Foundation. *Apache POI*. https://poi.apache.org/

---

## 15. Anexos

### 15.1 Comandos de compilación utilizados

```bash
# Compilar el código Kotlin de debug
gradlew.bat :app:compileDebugKotlin --no-daemon

# Instalar la aplicación en dispositivo conectado
gradlew.bat :app:installDebug --no-daemon
```

### 15.2 Estructura de directorios del código fuente

```
d:/Controlmedicamentos
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/carlos/controlmedicamentos/
│       │   ├── MainActivity.kt
│       │   ├── backup/
│       │   ├── data/local/
│       │   ├── data/remote/
│       │   ├── notifications/
│       │   ├── sync/
│       │   └── ui/theme/
│       └── res/
│           ├── drawable/
│           ├── mipmap/
│           ├── values/
│           └── xml/
├── sync-core/
│   ├── build.gradle.kts
│   └── src/main/kotlin/...
├── build.gradle.kts
├── settings.gradle.kts
├── DOSSIER.md
└── Informe_Proyecto.md
```

---

**Fin del informe**
