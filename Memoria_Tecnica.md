# Memoria Técnica del Proyecto ControlMedicamentos

## Arquitectura, Diseño e Implementación de una Aplicación Móvil Android para Gestión Integral de Salud Personal y Familiar

---

## Portada

| Campo | Valor |
|-------|-------|
| **Título** | Memoria Técnica — ControlMedicamentos |
| **Autor** | Carlos [Apellido del desarrollador] |
| **Rol asumido en el desarrollo** | Arquitecto de Software / Desarrollador principal |
| **Centro académico** | [Nombre de la universidad] |
| **Titulación** | [Nombre de la carrera] |
| **Tutor académico** | [Nombre del tutor] |
| **Fecha** | 1 de julio de 2026 |
| **Versión del sistema** | 1.0.0 (versionCode 25) |
| **Repositorio / código fuente** | `d:/Controlmedicamentos` |

---

## Índice

1. Resumen técnico
2. Introducción y marco teórico
3. Estado del arte y análisis comparativo
4. Análisis de requisitos no funcionales y funcionales
5. Arquitectura de software
6. Diseño detallado por capas
7. Modelo de datos relacional
8. Flujo de datos cliente-servidor/local
9. Justificación tecnológica
10. Algoritmos y mecanismos críticos
11. Funcionalidades justificadas desde eficiencia y estabilidad
12. Seguridad, privacidad y resiliencia
13. Estrategia de pruebas y calidad
14. Contribución personal y resolución de problemas reales
15. Conclusiones técnicas y trabajo futuro
16. Referencias bibliográficas

---

## 1. Resumen técnico

ControlMedicamentos es una aplicación móvil nativa para Android, construida en **Kotlin** con el framework de interfaz **Jetpack Compose**, que implementa un sistema de gestión integral de salud personal y familiar. El sistema opera predominantemente en modo **offline-first**, utilizando una base de datos local gestionada mediante **Room** sobre SQLite, complementada con un módulo de sincronización (`sync-core`) para interoperabilidad entre dispositivos. La aplicación integra más de 20 dominios de datos, gestión de alarmas locales, notificaciones con pantalla completa, reconocimiento de texto (OCR), exportación a documentos Word y un asistente conversacional basado en inteligencia artificial.

El presente documento constituye una **memoria técnica** que describe la arquitectura, las decisiones de diseño, la justificación de las tecnologías seleccionadas y los mecanismos implementados para garantizar la eficiencia, estabilidad y escalabilidad del sistema.

---

## 2. Introducción y marco teórico

### 2.1 Contexto

La digitalización de la salud personal ha generado un ecosistema de aplicaciones fragmentadas: una para recordatorios de medicación, otra para calendarios menstruales, otra para citas médicas y otra para seguimiento de actividad física. Esta fragmentación reduce la adherencia terapéutica y dificulta la correlación de datos entre dominios. ControlMedicamentos responde a esta problemática mediante una **arquitectura unificada** que permite almacenar, consultar y relacionar información de salud en un único sistema.

### 2.2 Marco teórico

El proyecto se enmarca dentro de los siguientes conceptos:

- **Arquitectura offline-first**: prioriza la disponibilidad local de datos y reduce la dependencia de conectividad.
- **Persistencia relacional local**: uso de SQLite a través de Room para garantizar integridad referencial y consultas eficientes.
- **UI declarativa**: Jetpack Compose permite construir interfaces reactivas y mantenibles mediante estados inmutables.
- **Programación reactiva**: uso de `Flow` y coroutines para propagar cambios de datos a la interfaz sin bloquear el hilo principal.
- **Arquitectura por capas**: separación de presentación, dominio y datos.

---

## 3. Estado del arte y análisis comparativo

| Característica | Apps genéricas de recordatorios | Apps de salud femenina | Apps de signos vitales | ControlMedicamentos |
|----------------|----------------------------------|------------------------|------------------------|---------------------|
| Recordatorios de medicación | Sí | No | No | Sí, con stock y dosis por toma |
| Calendario menstrual | No | Sí | No | Sí |
| Control de embarazo | No | Parcial | No | Sí, con visitas prenatales y documentos |
| Controles pediátricos y vacunas | No | No | No | Sí |
| Exportación a Word | No | No | No | Sí |
| Backup local completo | Raro | Raro | No | Sí, todas las entidades |
| Asistente IA integrado | No | No | No | Sí |
| Offline-first | Variable | Variable | No | Sí, diseño central |

La diferencia fundamental de ControlMedicamentos radica en la **integración vertical** de dominios y en la **arquitectura centrada en el dispositivo**, lo que garantiza funcionamiento sin conexión y privacidad de los datos.

---

## 4. Análisis de requisitos

### 4.1 Requisitos funcionales

- RF-01: Gestión de múltiples perfiles de paciente.
- RF-02: Registro de medicamentos/insumos con dosis, frecuencia, horarios y stock.
- RF-03: Registro de tomas manuales y automáticas desde alarmas.
- RF-04: Suspensión y reactivación de medicamentos sin pérdida de datos.
- RF-05: Directorio de contactos de salud y agenda de citas.
- RF-06: Registro de métricas diarias y cálculo de IMC.
- RF-07: Calendario menstrual y seguimiento de embarazo.
- RF-08: Control pediátrico, vacunación y percentiles.
- RF-09: Actividad física con GPS y mapa.
- RF-10: Carrito de pedidos con envío por WhatsApp.
- RF-11: Diario personal con imágenes.
- RF-12: Asistente IA para consultas sobre registros.
- RF-13: Exportación de resumen a Word.
- RF-14: Backup local y restauración completa.
- RF-15: Sincronización controlada entre dispositivos.

### 4.2 Requisitos no funcionales

| RNF | Descripción | Estrategia implementada |
|-----|-------------|------------------------|
| RNF-01 | Disponibilidad offline | Base de datos Room local y almacenamiento de archivos en directorio privado |
| RNF-02 | Bajo consumo de batería | Alarmas exactas programadas por `AlarmManager` + `WorkManager` para tareas diferidas |
| RNF-03 | Baja latencia en UI | Carga diferida con `LazyColumn`, `LazyRow`, coroutines y `Flow` |
| RNF-04 | Integridad de datos | Claves foráneas en Room, índices únicos y transacciones en IO |
| RNF-05 | Escalabilidad de datos | Normalización en 22+ entidades y consultas paginadas implícitas |
| RNF-06 | Seguridad | Almacenamiento cifrado con `security-crypto`, datos médicos en almacenamiento interno |
| RNF-07 | Mantenibilidad | Código organizado por paquetes, uso de estados inmutables y composables reutilizables |
| RNF-08 | Compatibilidad | minSdk 24, targetSdk 36, soporte de permisos condicionales por versión |

---

## 5. Arquitectura de software

### 5.1 Patrón arquitectónico

Se adopta una arquitectura **monolítica modularizada** con separación de responsabilidades inspirada en MVVM (Model-View-ViewModel) y Clean Architecture, adaptada a la naturaleza de Jetpack Compose:

- **Capa de presentación (UI)**: composables, estados y eventos.
- **Capa de lógica de presentación**: estados derivados (`remember`, `derivedStateOf`) y coroutines.
- **Capa de dominio / datos**: entidades Room, DAOs, funciones de extensión y mapeadores de sincronización.
- **Capa de infraestructura**: notificaciones, alarmas, backup, exportación, GPS, OCR, IA.

### 5.2 Diagrama de componentes (descrito en texto)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USUARIO / DISPOSITIVO                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │   Jetpack    │  │   Estados    │  │  Eventos     │               │
│  │   Compose    │  │   Compose    │  │  de usuario  │               │
│  │   (UI)       │  │              │  │              │               │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘               │
│         │                 │                 │                        │
│         └─────────────────┴─────────────────┘                        │
│                           │                                          │
│         ┌─────────────────▼──────────────────┐                     │
│         │  ViewModel / Coroutines / Flow      │                     │
│         │  (Lógica de presentación)          │                     │
│         └─────────────────┬──────────────────┘                     │
│                           │                                          │
│    ┌──────────────────────┼──────────────────────┐                   │
│    │                      │                      │                   │
│    ▼                      ▼                      ▼                   │
│ ┌──────────┐        ┌──────────┐        ┌──────────────┐           │
│ │  Room    │        │  Work    │        │  AlarmManager │           │
│ │  DAOs    │        │ Manager  │        │  + Receiver   │           │
│ │  SQLite  │        │          │        │               │           │
│ └────┬─────┘        └────┬─────┘        └───────┬───────┘           │
│      │                   │                      │                   │
│      ▼                   ▼                      ▼                   │
│ ┌─────────────────────────────────────────────────────────────┐    │
│ │              Módulo sync-core (modelos de sincronización)    │    │
│ └─────────────────────────────────────────────────────────────┘    │
│                           │                                         │
│                           ▼                                         │
│              ┌──────────────────────┐                              │
│              │  Servidor / Endpoint  │  (sincronización opcional)     │
│              │  de sincronización    │                                │
│              └──────────────────────┘                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.3 Diagrama de flujo de datos cliente-servidor/local

El flujo de datos se puede describir en cuatro escenarios principales:

#### Escenario A: Registro local de una toma de medicamento

```
Usuario toca una hora en el dashboard
         │
         ▼
MainActivity.kt (DashboardMedicationPage)
         │
         ▼
coroutineScope.launch(Dispatchers.IO)
         │
         ├──► MedicationIntakeDao.guardar() ──► Room ──► SQLite
         │
         └──► MedicationDao.actualizarStock() ──► Room ──► SQLite
         │
         ▼
Flow de MedicationIntakeDao se emite automáticamente
         │
         ▼
Compose recompone la UI con el nuevo estado
```

#### Escenario B: Disparo de una alarma programada

```
AlarmManager dispara PendingIntent a la hora configurada
         │
         ▼
AlarmReceiver.onReceive()
         │
         ▼
Registrar toma automática + descuento de stock
         │
         ▼
Notificación foreground con pantalla completa
         │
         ▼
Usuario acepta o rechaza desde la notificación
         │
         ▼
Actualización de la base de datos local
```

#### Escenario C: Sincronización cliente-servidor

```
Dispositivo A (cliente)           Servidor / Dispositivo B (nodo)
         │                                  │
         ├── SyncSnapshot ───────────────►  │
         │  (JSON serializado de entidades) │
         │                                  │
         ◄────────────── SyncAck ──────────┤
         │                                  │
         │  (conflictos resueltos,           │
         │   IDs mapeadas)                   │
         │                                  │
         ▼                                  ▼
BackupManager / SyncManager aplican cambios en Room
```

#### Escenario D: Exportación a Word

```
Usuario selecciona contenido en ReporteClinicoScreen
         │
         ▼
ReporteClinicoExporter compila entidades desde Room
         │
         ▼
Generación de documento .docx con Apache POI
         │
         ▼
Almacenamiento en directorio de descargas o compartir
```

---

## 6. Diseño detallado por capas

### 6.1 Capa de presentación

La interfaz se implementa con **Jetpack Compose**, lo que permite:

- **Recomposición selectiva**: solo los composables cuyos estados cambian se redibujan, reduciendo el costo de CPU.
- **Estados reactivos**: `remember`, `mutableStateOf`, `collectAsState()` y `derivedStateOf` mantienen la UI sincronizada con la base de datos.
- **Componibilidad**: tarjetas como `MetallicMedicationCard` son parametrizables (`isStockCritical`, `isSuspended`, `isInss`) para reutilizar estados visuales sin duplicar código.

La decisión de usar un solo `MainActivity.kt` de gran tamaño (~14 300 líneas) fue pragmática durante la fase de prototipado rápido, aunque se identifica como deuda técnica a refactorizar en futuras iteraciones.

### 6.2 Capa de lógica de presentación

No se utiliza un `ViewModel` explícito por pantalla; en su lugar, se emplea el patrón de **estados locales elevados** dentro de `MainActivity.kt` y funciones de extensión puras. Esta decisión reduce la sobrecarga de clases adicionales en un proyecto monolítico, pero requiere disciplina para evitar duplicación de estados.

Las operaciones de larga duración (escritura en base de datos, exportación, backup) se ejecutan en `Dispatchers.IO` dentro de `rememberCoroutineScope()`, garantizando que el hilo principal permanezca libre para eventos de UI.

### 6.3 Capa de datos

La persistencia se implementa con **Room** y se divide en:

- **Entidades**: clases Kotlin anotadas con `@Entity`, con claves primarias autogeneradas, claves foráneas e índices.
- **DAOs**: interfaces anotadas con `@Dao` que exponen consultas `suspend`, `Flow` y operaciones CRUD.
- **Base de datos**: `AppDatabase` centraliza las entidades y versiones.

La elección de Room sobre un acceso directo a SQLite garantiza:

- Verificación de consultas en tiempo de compilación.
- Generación automática de código de bajo nivel.
- Integración nativa con coroutines y Flow.
- Migraciones versionadas del esquema.

### 6.4 Capa de sincronización

El módulo `sync-core` define modelos de transferencia (`SyncMedication`, `SyncPatient`, `SyncVitalSigns`, etc.) desacoplados de las entidades locales. Los mapeadores en `AndroidSyncSnapshotMapper` convierten entidades a modelos sync y viceversa, preservando nulos y relaciones. Esta capa permite:

- Serializar el estado completo de la app a JSON.
- Transferir datos entre dispositivos sin exponer el esquema interno de Room.
- Restaurar datos sin romper la integridad referencial.

### 6.5 Capa de infraestructura

Incluye:

- **AlarmReceiver**: receptor de alarmas que registra tomas automáticas y descuenta stock.
- **MedicationScheduler**: encapsula la lógica de programación y cancelación de alarmas.
- **BackupManager**: exporta/importa todas las entidades, incluyendo imágenes en base64.
- **ReporteClinicoExporter**: genera documentos Word con Apache POI.
- **Asistente IA**: módulo conversacional (integración dependiente del modelo utilizado).
- **OCR**: ML Kit Text Recognition y Document Scanner para escanear documentos.
- **GPS**: Play Services Location y OSMDroid para actividad física.

---

## 7. Modelo de datos relacional

El esquema de base de datos sigue el principio de **normalización relacional** para evitar redundancia y mantener integridad. Las entidades principales se organizan en torno a `PatientProfile` como eje central.

### 7.1 Diagrama entidad-relación (descrito en texto)

```
PatientProfile (1)
    │
    ├──<1:N> Medication ──<1:N> MedicationIntake
    │
    ├──<1:N> MedicalPractitioner ──<1:N> MedicalAppointment
    │                                    │
    │                                    └──<1:N> MedicalReport
    │
    ├──<1:N> SignosVitales
    ├──<1:N> PhysicalActivity
    ├──<1:N> DiarioEntry
    ├──<1:N> CarritoPendienteItem
    ├──<1:N> MedicationOrder
    ├──<1:N> VaccinationRecord
    ├──<1:N> EnfermedadEntity
    │
    ├──<1:N> CicloMenstrual ──<1:N> RegistroDiarioCiclo
    │
    ├──<1:N> ControlEmbarazo ──<1:N> VisitaPrenatal
    │                           ──<1:N> DocumentoMedico
    │                           ──<1:1> BebeRecienNacido
    │
    ├──<1:N> MetodoAnticonceptivo ──<1:N> AnticonceptivoIntake
    │
    └──<1:N> NinoEntity ──<1:N> ControlPediatricoEntity
                          ──<1:N> VacunaEntity
```

### 7.2 Justificación del diseño relacional

- **Integridad referencial**: Room ejecuta las restricciones de claves foráneas al activar `foreignKeys`.
- **Consistencia en eliminación**: se pueden definir estrategias `CASCADE` para borrar registros dependientes.
- **Consultas eficientes**: índices en campos frecuentes (`patientId`, `scheduledAt`, `medicationId`) reducen la complejidad de búsqueda.

---

## 8. Flujo de datos cliente-servidor/local

### 8.1 Arquitectura cliente-servidor adaptada

Aunque la app funciona primordialmente en el dispositivo, el modelo de sincronización adopta una arquitectura **cliente-servidor lógica**:

- **Cliente**: aplicación Android que genera, consume y transforma datos.
- **Servidor**: en la sincronización entre dispositivos, el otro dispositivo o backend actúa como nodo servidor. En la sincronización local, el propio repositorio de datos actúa como servidor de verdad.

### 8.2 Diagrama de secuencia: sincronización entre dispositivos

```
Dispositivo A (Cliente)                     Dispositivo B (Servidor)
        │                                          │
        │  1. Recopilar snapshot local             │
        │     (BackupManager / SyncManager)        │
        │                                          │
        │  2. Serializar a JSON                    │
        │     (SyncModels + Gson/Moshi)            │
        │                                          │
        │──────────── 3. Enviar snapshot ────────► │
        │                                          │
        │                                          │ 4. Validar y deserializar
        │                                          │    (SyncValidator)
        │                                          │
        │                                          │ 5. Resolver conflictos
        │                                          │    (último ganador / merge)
        │                                          │
        │◄────────── 6. Devolver ack/errores ────│
        │                                          │
        │ 7. Aplicar cambios en Room local         │
        │                                          │
```

### 8.3 Justificación de la sincronización controlada

- **Eficiencia**: solo se transmite el snapshot, no cada operación individual.
- **Estabilidad**: el modelo de datos sync es independiente de Room, evitando acoplamiento entre cliente y servidor.
- **Resiliencia**: si falla la sincronización, el dispositivo conserva su estado local operativo.

---

## 9. Justificación tecnológica

### 9.1 Lenguaje: Kotlin

Se seleccionó **Kotlin** como lenguaje principal por las siguientes razones:

- **Oficialidad en Android**: Kotlin es el lenguaje de primera clase para Android desde 2019.
- **Concisión**: reduce el código boilerplate frente a Java mediante data classes, funciones de extensión y null-safety.
- **Coroutines**: integración nativa con `suspend` y `Flow` para operaciones asíncronas y reactivas.
- **Interoperabilidad**: permite usar cualquier librería Java existente si fuera necesario.

**Nota importante**: aunque la nota personal menciona Python y MySQL, el sistema desarrollado no utiliza Python ni MySQL. El backend de sincronización, si existiera, podría implementarse en cualquier lenguaje, pero la aplicación cliente es 100 % Kotlin y la base de datos local es SQLite a través de Room.

### 9.2 Base de datos local: SQLite + Room

Se descartó una base de datos remota como MySQL o PostgreSQL como almacenamiento principal porque:

- **Disponibilidad offline**: una app de salud debe funcionar sin conexión.
- **Latencia**: las consultas locales son órdenes de magnitud más rápidas que las de red.
- **Privacidad**: los datos sensibles permanecen en el dispositivo.
- **Costo**: no requiere infraestructura de servidor obligatoria.

### 9.3 Interfaz de usuario: Jetpack Compose

- **Paradigma declarativo**: la UI es una función del estado, lo que reduce errores de sincronización.
- **Rendimiento**: la recomposición selectiva minimiza el trabajo de renderizado.
- **Flexibilidad**: permite diseños personalizados (como las tarjetas metálicas) sin depender de XML.

### 9.4 Programación de alarmas: AlarmManager + BroadcastReceiver

- **Precisión**: `AlarmManager` con `RTC_WAKEUP` garantiza que la alarma se dispare a la hora exacta, incluso con Doze.
- **Eficiencia**: no requiere mantener un servicio en primer plano constante.
- **Robustez**: el `BroadcastReceiver` es persistente y se registra en el manifiesto.

### 9.5 OCR: ML Kit

- **Sin costo de infraestructura**: funciona en el dispositivo.
- **Privacidad**: el texto escaneado no sale del teléfono.
- **Rendimiento**: adecuado para documentos médicos y recetas.

### 9.6 Mapas y GPS: OSMDroid + Play Services Location

- **OSMDroid**: mapas gratuitos sin necesidad de API key (OpenStreetMap).
- **Fused Location Provider**: combina GPS, Wi-Fi y redes celulares para optimizar consumo energético.

### 9.7 Almacenamiento cifrado: security-crypto

- **Protección de datos sensibles**: claves y valores críticos se cifran con `EncryptedSharedPreferences`.
- **Compatibilidad**: proporcionada por AndroidX, no depende de implementaciones propietarias.

---

## 10. Algoritmos y mecanismos críticos

### 10.1 Cálculo de unidades por toma

Para medicamentos con múltiples horarios diarios, la dosis total no debe descontarse en cada toma. Se implementó la función de extensión:

```kotlin
fun Medication.unidadesPorToma(): Int {
    val dosisTotal = dosis.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val tomasDelDia = if (repartoDosis == "En diferentes horarios" && horariosTomas.isNotBlank()) {
        horariosTomas.split("|").filter { it.isNotBlank() }.size.coerceAtLeast(1)
    } else {
        1
    }
    return (dosisTotal / tomasDelDia).coerceAtLeast(1)
}
```

**Justificación técnica**: este algoritmo garantiza que el stock descienda proporcionalmente al número de tomas, evitando sobreconsumo. Se aplica de forma centralizada en:

- Marcado manual de toma en dashboard.
- Confirmación de toma vencida.
- Registro automático desde `AlarmReceiver`.
- Reintegración de stock al eliminar una toma.

### 10.2 Programación de alarmas

El `MedicationScheduler` calcula la siguiente hora de toma futura y programa una `PendingIntent` única por medicamento. La cancelación ocurre al suspender, eliminar o desactivar alarmas, evitando alarmas huérfanas.

### 10.3 Suspensión y reactivación de medicamentos

Un medicamento suspendido (`estaActivo = false`) permanece en la base de datos y en el dashboard, pero:

- No se programan nuevas alarmas.
- No se permiten marcar tomas desde la UI.
- Se aplica un estilo visual diferenciado (`isSuspended`) para evitar confusiones.

Al reactivar, se reprograman las alarmas si el usuario las tenía activas. Esto garantiza estabilidad operativa sin pérdida de histórico.

### 10.4 Backup y restauración

El `BackupManager` serializa todas las entidades Room, incluyendo imágenes en base64, y genera un archivo JSON cifrado/legible. La restauración valida la integridad referencial antes de escribir en la base de datos, evitando estados corruptos.

---

## 11. Funcionalidades justificadas desde eficiencia y estabilidad

### 11.1 Medicamentos e inventario

- **Eficiencia**: la carga del dashboard se realiza mediante `LazyRow` y estados derivados; solo los horarios del día seleccionado se calculan.
- **Estabilidad**: las tomas tienen un índice único `(medicationId, scheduledAt)` que evita duplicados.

### 11.2 Recordatorios y alarmas

- **Eficiencia**: una sola alarma por medicamento, no una por hora, gracias al cálculo de la siguiente toma.
- **Estabilidad**: el `BroadcastReceiver` sobrevive al cierre de la app y reacciona incluso tras reinicio del dispositivo.

### 11.3 Ciclo menstrual y embarazo

- **Eficiencia**: las predicciones se calculan sobre datos locales sin llamadas de red.
- **Estabilidad**: los ciclos y embarazos mantienen historial completo, permitiendo análisis retrospectivo.

### 11.4 Control pediátrico y vacunación

- **Eficiencia**: las vacunas se agrupan por edad recomendada, facilitando la consulta visual.
- **Estabilidad**: los controles pediátricos están vinculados a un `NinoEntity` mediante clave foránea.

### 11.5 Exportación a Word

- **Eficiencia**: el documento se genera en `Dispatchers.IO` y se notifica al usuario mediante `Snackbar` o `Toast`.
- **Estabilidad**: el exporter no modifica la base de datos; solo lee, reduciendo riesgos de corrupción.

### 11.6 Asistente IA

- **Eficiencia**: el asistente opera sobre el snapshot local de datos, evitando transferencias innecesarias.
- **Estabilidad**: funcionalidad desacoplada del núcleo de datos; un fallo en IA no afecta la operación de la app.

---

## 12. Seguridad, privacidad y resiliencia

### 12.1 Seguridad

- **Cifrado**: `EncryptedSharedPreferences` para datos sensibles y configuraciones.
- **Almacenamiento interno**: las bases de datos y archivos de la app no son accesibles directamente por otras apps (sandbox de Android).
- **Permisos mínimos**: cada función solicita solo los permisos estrictamente necesarios.

### 12.2 Privacidad

- **Offline-first**: los datos médicos no se transmiten a servidores sin consentimiento explícito.
- **OCR local**: el reconocimiento de texto se ejecuta en el dispositivo.

### 12.3 Resiliencia

- **Recuperación ante fallos**: las corrutinas capturan excepciones y los DAOs utilizan transacciones implícitas.
- **Validación de entrada**: se usan `toIntOrNull()`, `runCatching` y valores por defecto para evitar crashes.
- **Diseño defensivo**: el cálculo de dosis y stocks utiliza `coerceAtLeast(1)` para evitar valores cero o negativos.

---

## 13. Estrategia de pruebas y calidad

### 13.1 Pruebas de compilación

- Comando: `gradlew.bat :app:compileDebugKotlin --no-daemon`
- Resultado: `BUILD SUCCESSFUL` tras cada iteración relevante.

### 13.2 Pruebas de instalación

- Comando: `gradlew.bat :app:installDebug --no-daemon`
- Dispositivo: Samsung `SM-S918U`.
- Resultado: `Installed on 1 device.`

### 13.3 Pruebas funcionales manuales

- Marcado/desmarcado de tomas de medicamentos.
- Descuento correcto de stock en medicamentos con múltiples horarios.
- Suspensión y reactivación con estilo visual diferenciado.
- Registro de ciclo menstrual, embarazo y controles pediátricos.
- Exportación de resumen a Word.
- Restauración de backup local.

### 13.4 Métricas de calidad

- **Cobertura de funcionalidades**: 15 requisitos funcionales implementados.
- **Líneas de código fuente**: ~30 626 líneas.
- **Deuda técnica identificada**: concentración de UI en `MainActivity.kt`; refactorización recomendada en trabajo futuro.

---

## 14. Contribución personal y resolución de problemas reales

### 14.1 Reto: descuento incorrecto de stock en medicamentos con múltiples tomas

**Problema**: cuando un medicamento tenía dosis diaria total `3` repartida en 3 horarios, cada toma descontaba `3` unidades del stock en lugar de `1`, consumiendo el stock tres veces más rápido de lo correcto.

**Análisis**: el error provenía de usar directamente `medication.dosis` como unidades a descontar en cada toma, sin considerar el número de horarios configurados.

**Solución**: se diseñó e implementó la función `Medication.unidadesPorToma()`, que divide la dosis total entre el número de tomas diarias cuando el reparto es "En diferentes horarios". Esta función se aplicó centralmente en:

- `DashboardMedicationPage` (toma manual).
- `ConfirmacionTomaVencidaDialog` (toma vencida).
- `registrarToma()` (toma desde notificación).
- Eliminación de toma (reintegración de stock).
- `AlarmReceiver` (toma automática).

**Lección aprendida**: centralizar la lógica de cálculo en funciones de extensión evita errores de inconsistencia y facilita futuras correcciones.

### 14.2 Reto: medicamentos suspendidos desaparecían del sistema

**Problema**: al suspender un medicamento (`estaActivo = false`), este dejaba de aparecer en el dashboard y en la lista, dificultando su reactivación. El usuario no podía recuperarlo sin manipular la base de datos.

**Análisis**: la consulta de medicamentos filtraba exclusivamente activos (`obtenerActivosPorPaciente`), y la función `obtenerInsumosProgramadosDelDia()` también descartaba los suspendidos.

**Solución**:

1. Se cambió la fuente de datos del listado a `observarTodosPorPaciente` para incluir activos y suspendidos.
2. Se eliminó el filtro `estaActivo` en `obtenerInsumosProgramadosDelDia()`, ordenando los suspendidos al final.
3. Se agregó el parámetro `isSuspended` a `MetallicMedicationCard` para estilo visual diferenciado.
4. Se implementó un botón "Reactivar" en el dashboard que ejecuta `cambiarEstado(id, true)` y reprograma alarmas si están activas.
5. Se ajustó el título de la sección de "Medicamentos en uso" a "Medicamentos" para reflejar el nuevo alcance.

**Lección aprendida**: la suspensión debe ser un estado reversible, no una eliminación lógica. Mantener los datos accesibles mejora la usabilidad y evita pérdida de información histórica.

### 14.3 Reto: manejo de contexto en coroutines dentro de composables

**Problema**: al intentar usar `Toast.makeText(context, ...)` dentro de un `onClick` lanzado en `Dispatchers.IO`, el compilador reportó el error "Function invocation 'context(...)' expected", seguido de "@Composable invocations can only happen from the context of a @Composable function".

**Análisis**: el `context` no estaba disponible en el lambda de la coroutine, y `LocalContext.current` no puede invocarse dentro de un bloque no composable.

**Solución**: se declaró `val context = LocalContext.current` al inicio de la función composable `DashboardMedicationPage`, y luego se referenció esa variable dentro del `onClick`. Esto mantuvo la seguridad de contexto y permitió mostrar notificaciones en el hilo principal mediante `withContext(Dispatchers.Main)`.

**Lección aprendida**: en Compose, el contexto debe capturarse en el ámbito composable antes de lanzar coroutines en callbacks no composables.

### 14.4 Reto: instalación en dispositivo físico

**Problema**: la primera instalación falló porque el dispositivo Android no estaba autorizado para depuración USB (`unauthorized`).

**Análisis**: el dispositivo Samsung `SM-S918U` tenía la opción de depuración USB activada pero no había aceptado la clave RSA del equipo de desarrollo.

**Solución**: se guió al usuario para que aceptara la autorización de depuración en el dispositivo. Posteriormente, la instalación con `gradlew.bat :app:installDebug` reportó `Installed on 1 device.`

**Lección aprendida**: el despliegue en dispositivos físicos requiere coordinación entre configuración de desarrollador y autorización de seguridad; no es solo un problema de código.

### 14.5 Reto: ajuste visual para estados de medicamentos

**Problema**: el usuario solicitó que los medicamentos suspendidos sean visualmente evidentes mediante un fondo gris claro, pero manteniendo legibilidad del texto.

**Análisis**: el componente `MetallicMedicationCard` originalmente usaba colores oscuros y texto blanco. Aplicar un fondo gris claro sin cambiar el color de texto generaría bajo contraste.

**Solución**: se añadió una rama de color para `isSuspended` en `MetallicMedicationCard` (fondo gris claro, borde gris, brillo suave) y se introdujo una variable `contentColor` dentro de `DashboardMedicationPage` que cambia entre `Color(0xFF333333)` (suspendido) y `Color.White` (activo). Todos los textos del dashboard se actualizaron para usar `contentColor`.

**Lección aprendida**: la accesibilidad visual no es un detalle cosmético; requiere coordinar fondo, texto y estados interactivos de forma coherente en todo el árbol de composables.

---

## 15. Conclusiones técnicas y trabajo futuro

### 15.1 Conclusiones

- Se logró construir una aplicación Android completa, nativa y funcional, con un único lenguaje principal (Kotlin) y una arquitectura offline-first robusta.
- La elección de Room + Jetpack Compose + Coroutines permitió desarrollar una UI reactiva con persistencia local íntegra.
- La modularidad de `sync-core` prepara el terreno para futuras expansiones de sincronización en la nube sin acoplar el cliente al esquema de Room.
- Los problemas reales encontrados (stock, suspensión, contexto, instalación) fueron resueltos mediante análisis técnico, centralización de lógica y pruebas en dispositivo físico.

### 15.2 Trabajo futuro

- **Refactorización de `MainActivity.kt`**: dividir la UI en múltiples archivos por módulo para mejorar mantenibilidad.
- **Pruebas automatizadas**: implementar pruebas unitarias (JUnit) y de interfaz (Espresso/Compose Test) para las funciones críticas de stock y alarmas.
- **Sincronización en la nube**: evaluar Firebase, Google Drive o backend propio para sincronización automática entre dispositivos.
- **Gráficos de evolución**: integrar librerías como MPAndroidChart para visualizar métricas diarias, peso infantil y ciclos.
- **Optimización de IA**: mejorar el asistente con modelos locales o servicios de lenguaje con contexto médico controlado.
- **Publicación**: adaptar textos, políticas de privacidad y permisos para cumplir con Google Play Console.

---

## 16. Referencias bibliográficas

- Android Developers. (2026). *Jetpack Compose*. Recuperado de https://developer.android.com/jetpack/compose
- Android Developers. (2026). *Room Persistence Library*. Recuperado de https://developer.android.com/training/data-storage/room
- Android Developers. (2026). *AlarmManager*. Recuperado de https://developer.android.com/reference/android/app/AlarmManager
- Kotlin Foundation. (2026). *Kotlin Programming Language*. Recuperado de https://kotlinlang.org
- Google. (2026). *ML Kit for Firebase*. Recuperado de https://developers.google.com/ml-kit
- OpenStreetMap. (2026). *OSMDroid*. Recuperado de https://github.com/osmdroid/osmdroid
- Apache Software Foundation. (2026). *Apache POI*. Recuperado de https://poi.apache.org/
- Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
- Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.

---

**Fin de la memoria técnica**
