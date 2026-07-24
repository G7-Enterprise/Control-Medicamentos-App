# Dossier del proyecto ControlMedicamentos

## 24 de julio de 2026 (tercer ajuste) — Corrección completa del backup/restore

### 1. Problema detectado

Las copias de seguridad no respaldaban/restauraban correctamente:

- **Foto de perfil:** no se restauraba en el destino.
- **Documentos adjuntos de informes médicos:** no se mostraban tras restaurar en otro dispositivo.
- **Signos vitales:** faltaban meses de datos por usar el `syncSnapshot` reducido en lugar del array de primer nivel.
- **Nuevos módulos:** `sedentarismo`, `hidratación` y `alertas de caídas` no aparecían en los checkboxes ni en la lógica de backup/restore.

### 2. Solución implementada

**Archivos modificados:**

- `app/src/main/java/com/carlos/controlmedicamentos/backup/BackupManager.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/DialogosPrincipalesPanel.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/HidratacionDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/FallAlertDao.kt`

Cambios:

1. Añadidos `registrosHidratacion` y `fallAlerts` a `BackupSelection` y `BackupSummary`.
2. Incluidos los arrays `registrosHidratacion` y `fallAlerts` en `buildBackupJson` y `restoreFromJson`.
3. Añadidos métodos masivos (`guardarTodos`, `obtenerTodosLista`, `eliminarTodos`) en `HidratacionDao` y `FallAlertDao`.
4. Añadidas funciones de serialización/deserialización JSON para `RegistroHidratacion` y `FallAlert`.
5. Actualizados los diálogos de backup/restore con checkboxes para:
   - Sedentarismo (registros)
   - Sedentarismo (configuración)
   - Hidratación
   - Alertas de caídas
6. **Corrección de foto de perfil, documentos adjuntos y signos vitales:** en `restoreFromJson` se cambió la prioridad para usar los **arrays de primer nivel** del JSON (`patients`, `reports`, `vitalSigns`, etc.) antes de recurrir al `syncSnapshot`. Los arrays de primer nivel contienen la foto codificada en base64, los adjuntos embebidos y el campo `fechaRegistro`, evitando la pérdida de datos.

### 3. Verificación

- Compilación:
  ```text
  BUILD SUCCESSFUL
  ```
- APK generada en `app/build/outputs/apk/debug/app-debug.apk`.
- Instalación: en el momento de la instalación el dispositivo no estaba conectado (`adb devices` devolvió lista vacía). El APK está listo para instalar con:
  ```text
  .\gradlew.bat :app:installDebug
  ```
  o manualmente con adb una vez se reconecte el dispositivo.
- Se recomienda probar un ciclo completo: crear backup, desinstalar/instalar, restaurar y verificar foto de perfil, documentos de informes y signos vitales por meses.

---

## 24 de julio de 2026 (continuación) — Reparación del botón "Ver listado de registros guardados" en signos vitales

### 1. Problema detectado

En la pantalla **Signos vitales**, el botón **"Ver listado de registros guardados"** no mostraba el listado y regresaba al escritorio. La primera causa era que la lambda que debía cambiar el estado `mostrarListadoSignosPanel` estaba vacía:

```kotlin
val onMostrarListadoSignosPanelChange: (Boolean) -> Unit = { /* handled externally */ }
```

Al pulsar el botón se ocultaba `mostrarPanelSignosVitales` pero nunca se activaba el panel del listado, por lo que `mostrarEscritorio` volvía a `true`.

Tras conectar correctamente el callback, la app lanzaba un **crash** con el mensaje:

```text
java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed.
```

El `logcat` mostró que el error se producía al renderizar `ListadoSignosVitalesPanel`.

### 2. Solución implementada

**Archivos modificados:**

- `app/src/main/java/com/carlos/controlmedicamentos/MedicamentoFormPanelSecundarios.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/MedicamentoFormBodyPaneles.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/MedicamentoFormBody.kt`

Cambios:

1. Se añadió el parámetro `onMostrarListadoSignosPanelChange: (Boolean) -> Unit` a `MedicamentoFormPanelSecundarios` y a `MedicamentoFormBodyPaneles`.
2. En `MedicamentoFormBody` se creó el setter real:
   ```kotlin
   val onMostrarListadoSignosPanelChange: (Boolean) -> Unit = { mostrarListadoSignosPanelState.value = it }
   ```
   y se propagó hacia `MedicamentoFormPanelSecundarios`.
3. Se eliminó el lambda no-op `/* handled externally */` de `MedicamentoFormPanelSecundarios`, dejando que el callback recibido actualice el estado.
4. Se actualizó `mostrarEscritorio` para incluir `!mostrarListadoSignosPanel && !mostrarListadoSignosGuardados`, evitando que el escritorio se considere activo cuando el listado de signos vitales esté abierto.
5. **Ajuste final del crash**: `MedicamentoFormGradientWrapper` añade `verticalScroll` al contenedor principal cuando `panelUsaScrollInterno` es `false`. `ListadoSignosVitalesPanel` también tiene `verticalScroll` en su `Column` raíz. Al activarse el listado, ambos scrolls anidados creaban una altura infinita. Se solucionó añadiendo `mostrarListadoSignosPanel` y `mostrarListadoSignosGuardados` a `panelUsaScrollInterno`, para que el wrapper no aplique `verticalScroll` mientras se muestra el listado y deje que el propio panel gestione su scroll.

El flujo correcto ahora es:

- Pulsa **"Ver listado de registros guardados"** → `mostrarPanelSignosVitales = false` y `mostrarListadoSignosPanel = true` → se muestra `ListadoSignosVitalesPanel`.
- Desde el listado, el icono de cerrar o el `BackHandler` vuelven a `mostrarListadoSignosPanel = false` y `mostrarPanelSignosVitales = true`.

### 3. Verificación

- Compilación:
  ```text
  BUILD SUCCESSFUL
  ```
- Instalación:
  ```text
  > Task :app:installDebug
  Installing APK 'app-debug.apk' on 'SM-X115 - 16' for :app:debug
  Installed on 1 device.
  ```
- No se afectó el formulario de informes médicos ni los diálogos de eliminación implementados en la sesión anterior.

---

## 24 de julio de 2026 (segundo ajuste) — Layout del menú flotante y etiqueta "Signos vitales"

### 1. Problema detectado

Tras corregir el crash, al pulsar **"Ver listado de registros guardados"** se mostraba una pantalla en blanco con el menú hamburguesa visible. Dentro del menú flotante no aparecía la opción **"Signos vitales"**; en su lugar se mostraba **"Métricas Diarias"**.

La causa fue que `MenuHamburguesaFlotante` usaba `Modifier.fillMaxSize()` y se ubicaba antes que `ListadoSignosVitalesPanel` dentro del `Column` de `MedicamentoFormBody`. Al consumir toda la altura disponible, dejaba al listado fuera de la pantalla. Además, la opción del menú flotante tenía una etiqueta diferente a la del escritorio.

### 2. Solución implementada

**Archivos modificados:**

- `app/src/main/java/com/carlos/controlmedicamentos/MenuHamburguesaFlotante.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/MedicamentoFormBody.kt`

Cambios:

1. Se cambió el `Box` raíz de `MenuHamburguesaFlotante` de `fillMaxSize()` a `fillMaxWidth().wrapContentHeight()`, de modo que el menú ocupe solo la barra superior y no tape el contenido.
2. Se movió la llamada a `MenuHamburguesaFlotante` al inicio del contenido de `MedicamentoFormGradientWrapper` en `MedicamentoFormBody`, por encima de los paneles y del escritorio.
3. Se renombró la opción del menú flotante de **"Métricas Diarias"** a **"Signos vitales"** para mantener consistencia con el menú del escritorio.

### 3. Verificación

- Compilación:
  ```text
  BUILD SUCCESSFUL
  ```
- Instalación:
  ```text
  > Task :app:installDebug
  Installing APK 'app-debug.apk' on 'SM-X115 - 16' for :app:debug
  Installed on 1 device.
  ```

---

## 23 de julio de 2026 (sesión nocturna) — Corrección UI de informes médicos, texto negro en botones, diálogos de eliminación y refuerzo del profesional médico

### 1. Problema detectado

- Al pulsar **"Nuevo informe"** o **"Editar"** en la sección **Documentos / Informes médicos** de la ficha del paciente, la app navegaba a una pantalla en blanco en la que solo se veía el menú hamburguesa. El formulario `FormularioInformePanel` no llegaba a renderizarse.
- El texto de numerosos botones de menús, submenús y paneles secundarios se mostraba en blanco o con poco contraste, dificultando su lectura.
- Varios botones de **Eliminar** (documentos, citas, adjuntos, médicos, pedidos y vacunas) ejecutaban la acción directamente sin pedir confirmación, con riesgo de borrados accidentales.

### 2. Corrección de la navegación en blanco de informes médicos

**Archivo:** `app/src/main/java/com/carlos/controlmedicamentos/MedicamentoFormPanelSecundarios.kt`

El `FormularioInformePanel` ya existía como composable, pero nunca se invocaba desde `MedicamentoFormPanelSecundarios`. Se añadió la renderización condicional con todos los estados y callbacks necesarios:

- Variables derivadas desde `MedicamentoFormState`:
  - `tituloInforme`
  - `descripcionInforme`
  - `expandedProfesionalInforme`
  - `practitionerIdInforme`
  - `estudiosAdjuntos`
  - `visorAdjuntos`
  - `tienePermisoCamara`
  - `cameraPermissionPending`

- Callbacks conectados:
  - `onTituloInformeChange`
  - `onDescripcionInformeChange`
  - `onExpandedProfesionalInformeChange`
  - `onPractitionerIdInformeChange`
  - `onVisorAdjuntosChange`
  - `onCameraPermissionPendingChange`
  - `onGuardarInformeMedicoActual`
  - `onInformeMedicoTieneCambiosSinGuardar`
  - `onCerrarFormularioInforme`
  - `onMostrarDialogoCerrarInformeSinGuardarChange`
  - `onLaunchDocumentScanner`

- Invocación añadida:

```kotlin
FormularioInformePanel(
    mostrarFormularioInforme = mostrarFormularioInforme,
    tituloInforme = tituloInforme,
    descripcionInforme = descripcionInforme,
    expandedProfesionalInforme = expandedProfesionalInforme,
    practitionerIdInforme = practitionerIdInforme,
    profesionalesHabituales = profesionalesHabituales,
    estudiosAdjuntos = estudiosAdjuntos,
    visorAdjuntos = visorAdjuntos,
    tienePermisoCamara = tienePermisoCamara,
    cameraPermissionPending = cameraPermissionPending,
    onTituloInformeChange = onTituloInformeChange,
    onDescripcionInformeChange = onDescripcionInformeChange,
    onExpandedProfesionalInformeChange = onExpandedProfesionalInformeChange,
    onPractitionerIdInformeChange = onPractitionerIdInformeChange,
    onVisorAdjuntosChange = onVisorAdjuntosChange,
    onCameraPermissionPendingChange = onCameraPermissionPendingChange,
    onGuardarInformeMedicoActual = onGuardarInformeMedicoActual,
    onInformeMedicoTieneCambiosSinGuardar = onInformeMedicoTieneCambiosSinGuardar,
    onCerrarFormularioInforme = onCerrarFormularioInforme,
    onMostrarDialogoCerrarInformeSinGuardarChange = onMostrarDialogoCerrarInformeSinGuardarChange,
    onLaunchDocumentScanner = onLaunchDocumentScanner,
    cameraPermissionLauncher = launchers.cameraPermissionLauncher,
    pickStudyImagesLauncher = launchers.pickStudyImagesLauncher
)
```

Este cambio hace que el formulario de informes aparezca correctamente tanto al crear un documento nuevo como al editar uno existente.

### 3. Texto negro en botones de menús y paneles

Se aplicó `contentColor = Color.Black` y `Text(..., color = Color.Black)` en todos los botones afectados para garantizar legibilidad.

#### 3.1 `FichaPacientePanel.kt`

- Botones **"Editar"** y **"Eliminar"** de cada documento/informe.
- Botones de exportación por periodo.
- Botón **"Nuevo informe"**.
- Botón **"Volver al escritorio"**.

Ejemplo del cambio:

```kotlin
Button(
    onClick = { onCargarInformeMedico(reporte) },
    modifier = Modifier.weight(1f),
    colors = ButtonDefaults.buttonColors(contentColor = Color.Black)
) {
    Text("Editar", color = Color.Black)
}
```

#### 3.2 `FormularioInformePanel.kt`

- Botones **"Escanear"**, **"Galeria"** y **"Quitar"**.

#### 3.3 `CitasMedicasActivity.kt`

- Botones de listado: **"Ya realizada"**, **"Editar"**, **"Eliminar"**, **"Ver historial"**, **"Nuevo Evento"**.
- Formulario: **"Guardar Cita"**, **"Cancelar"**, selector de fecha.
- Historial: **"Exportar a PDF"** y **"Cerrar"**.
- Botones del diálogo de eliminación de cita: **"Eliminar"** y **"Cancelar"**.
- Además se corrigió el título de la pantalla: de `"Informes médicos"` a `"Citas médicas"`, usando `CApptTextMain` para que sea visible sobre el fondo oscuro.

#### 3.4 `ListaInsumosPanel.kt`

- **"Recargar stock"**
- **"Añadir a la lista de pedidos"**
- **"Suspender"** / **"Reactivar"**
- **"Desactivar alarma"** / **"Activar alarma"**
- **"Ver lista de pedidos"**
- **"Volver al escritorio"**

#### 3.5 `PanelProfesionalesPanel.kt`

- **"Nuevo"**, **"Editar"**, **"Eliminar"** y **"Ver informes"**.

#### 3.6 `FormularioProfesionalPanel.kt`

- **"Guardar"** y **"Cerrar"**.

### 4. Diálogos de confirmación para eliminar

#### 4.1 `FichaPacientePanel.kt`

Se añadieron dos `AlertDialog` al final del composable:

- **Eliminar documento**: usa el estado `reportePendienteDeEliminar` y, tras confirmar, ejecuta `database.medicalReportDao().eliminar(reporte)`.
- **Eliminar cita médica**: usa el estado `citaPendienteDeEliminar` y, tras confirmar, cancela la alarma (`MedicalAppointmentScheduler`) y elimina la cita. También limpia la selección actual.

#### 4.2 `FormularioInformePanel.kt`

- El botón **"Quitar"** de un adjunto ya no elimina directamente del `SnapshotStateList`, sino que muestra un diálogo de confirmación con estado `adjuntoPendienteDeEliminar`.

#### 4.3 `PanelProfesionalesPanel.kt`

- El botón **"Eliminar"** de un médico habitual ahora abre un diálogo con iconos de aceptar/cancelar antes de llamar a `database.medicalPractitionerDao().eliminar(...)`.

#### 4.4 `PanelPedidosPanel.kt`

- El botón **"Vaciar lista de pedidos"** ahora requiere confirmación a través del estado `mostrarDialogoVaciar`.

#### 4.5 `NuevaVacunaActivity.kt`

- El diálogo de eliminación de registros de vacuna fue estilizado con iconos de aceptar/cancelar y fondo `Color(0xFF2A0040)`.

### 5. Campo teléfono en profesionales médicos

Se añadió soporte para guardar el teléfono del profesional médico en todas las capas:

- `MedicalPractitioner.kt`: nueva propiedad `phone: String = ""` con `@ColumnInfo(defaultValue = "")`.
- `AppDatabase.kt`:
  - Versión de base de datos subida a **58**.
  - Migración `MIGRATION_57_58` añade la columna `phone TEXT NOT NULL DEFAULT ''` en `medical_practitioners`.
- `MedicamentoFormState.kt`: añadido `telefonoProfesionalState` y su getter.
- `MedicamentoFormActions.kt`:
  - `guardarMedicoHabitualActualAction` recibe y persiste `telefonoProfesional`.
  - `resetMedicoHabitualAction` resetea el campo.
- `MainActivity.kt`:
  - Carga `practitioner.phone` al editar.
  - Pasa el teléfono al guardar.
- `FormularioProfesionalPanel.kt`: nuevo `OutlinedTextField` con etiqueta **"Teléfono"**.
- `MedicamentoFormPanelSecundarios.kt`: conecta el estado y callback del teléfono con el panel y el formulario.
- Backup y sincronización:
  - `BackupManager.kt`: `phone` se incluye en JSON de `MedicalPractitioner` y `SyncMedicalPractitioner`, y se lee/escribe en las conversiones.
  - `AndroidSyncSnapshotMapper.kt`: `MedicalPractitioner.toSyncModel()` copia `phone`.
  - `SyncModels.kt`: `SyncMedicalPractitioner` incluye `phone`.

### 6. Escritorio: indicador de próxima cita o evento

**Archivo:** `app/src/main/java/com/carlos/controlmedicamentos/EscritorioContent.kt`

- Se añadió una `data class ProximaCitaInfo(titulo: String, fechaHora: Long)`.
- Se añadió un `LaunchedEffect(pacienteActivo?.id)` que consulta en `Dispatchers.IO` la próxima cita médica, cita dental, próxima dosis de vacuna y fecha probable de parto.
- Se muestra una tarjeta amarilla en la parte superior del escritorio con el evento más cercano y un marquee para títulos largos.

Queries añadidas:

- `MedicalAppointmentDao.kt`: `obtenerProximaNoCompletada(patientId, now)`.
- `VaccinationRecordDao.kt`: `obtenerProximaDosis(patientId, now)`.

### 7. Notificaciones de pantalla completa

**Archivo:** `app/src/main/AndroidManifest.xml`

- Se añadió la actividad `ReminderAlertActivity` con flags `showOnLockScreen`, `turnScreenOn`, `showWhenLocked`, `excludeFromRecents` y `noHistory`.

**Archivo:** `app/src/main/java/com/carlos/controlmedicamentos/notifications/NotificacionHelper.kt`

- Las notificaciones de **stock bajo**, **hidratación** y **sedentarismo** ahora lanzan `ReminderAlertActivity` a pantalla completa (`context.startActivity(fullScreenIntent)`), además de la notificación estándar.

### 8. Ajustes de compilación, firma y seguridad

- `app/build.gradle.kts`:
  - `appVersionCode` subido a **41**.
  - Release: `isMinifyEnabled = false` y `isShrinkResources = false` para facilitar depuración.
  - Configuración de firma: `enableV1Signing = true` y `enableV2Signing = true`.
- `build.gradle.kts`:
  - Forzar `sourceCompatibility` y `targetCompatibility` a Java **21** en todas las tareas `JavaCompile`.
- `sync-core/build.gradle.kts`:
  - Compatibilidad Java 11 para `JavaCompile`.
  - `jvmToolchain(21)` para Kotlin, con `jvmTarget = JVM_11`.
- `settings.gradle.kts`:
  - Limpieza de espacios al incluir `:sync-core`.
- `SecurityManager.kt`:
  - `EXPECTED_CERT_SHA256` cambiado a `"CONFIGURE_ME"` para desactivar la validación de firma del certificado hasta que se configure el hash real del keystore de release.
- `LicenseManager.kt`:
  - La fecha de expiración ahora se calcula a partir de `firstInstallTime` del paquete, no de `BuildConfig.BUILD_TIMESTAMP`, para que la caducidad sea relativa a la instalación del usuario.

### 9. Otros ajustes menores

- `MenuHamburguesaEscritorio.kt` y `MenuHamburguesaFlotante.kt`: la opción **"Galería"** se comentó/ocultó temporalmente.
- `HidratacionScreen.kt`: los chips de selección de meta de hidratación (1500, 2000, 2500, 3000 ml) se reorganizaron en dos filas con `Modifier.weight(1f)`, evitando que se corten en pantallas pequeñas.

### 10. Verificación

- Compilación:
  ```text
  > Task :app:compileDebugKotlin
  > Task :app:compileDebugJavaWithJavac
  BUILD SUCCESSFUL
  ```
- Instalación:
  ```text
  > Task :app:installDebug
  Installing APK 'app-debug.apk' on 'SM-X115 - 16' for :app:debug
  Installed on 1 device.
  ```
- No se produjeron errores de compilación. Los únicos warnings fueron por APIs obsoletas (`Modifier.menuAnchor()`, `Locale` constructors y flags de pantalla de bloqueo).

---

## 1 de julio de 2026 (sesión tarde) — Corrección del descuento de stock en medicamentos con múltiples tomas

### 1. Problema detectado
En los medicamentos configurados con **"En diferentes horarios"** (varias tomas diarias), al aceptar una sola toma el sistema descontaba del stock la **cantidad total diaria** (`medication.dosis`) en lugar de la porción correspondiente a esa toma. Por ejemplo, si un medicamento tenía dosis diaria `3` repartida en 3 horarios, cada toma descontaba `3` unidades en vez de `1`, consumiendo el stock tres veces más rápido de lo correcto.

### 2. Solución implementada
Se creó una función helper `unidadesPorToma()` en `Medication.kt` que calcula la cantidad correspondiente a cada toma según la dosis total diaria y el número de horarios configurados:

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

### 3. Lugares corregidos
- **`MainActivity.kt:DashboardMedicationPage`** — al marcar una toma manual desde el dashboard, ahora descuenta y guarda la dosis correcta por toma.
- **`MainActivity.kt:ConfirmacionTomaVencidaDialog`** — al confirmar una toma vencida, usa `unidadesPorToma()`.
- **`MainActivity.kt:registrarToma()`** — al aceptar una toma desde el diálogo de notificación, descuenta y guarda la cantidad por toma.
- **`MainActivity.kt:eliminar toma` (2 diálogos)** — al eliminar una toma, reintegra al stock la cantidad correcta por toma (`med.unidadesPorToma()`).
- **`AlarmReceiver.kt:registrarTomasAceptadas`** — al aceptar automáticamente una toma desde el recordatorio, usa `unidadesPorToma()` para descuento y stock bajo.

### 4. Efecto en el stock
- Caso común: dosis diaria `3` en 3 tomas → cada toma descuenta `1` unidad (total `3` al día).
- Caso con dosis diaria `2` en 2 tomas → cada toma descuenta `1` unidad (total `2` al día).
- Caso con dosis diaria `1` en una sola toma → descuenta `1` unidad.

### 5. Compilación e instalación
- `BUILD SUCCESSFUL` con `gradlew.bat :app:compileDebugKotlin --no-daemon`.
- `BUILD SUCCESSFUL` con `gradlew.bat :app:installDebug --no-daemon` en dispositivo `SM-S918U - 16`.
- APK instalado correctamente: `Installed on 1 device.`

---

## 1 de julio de 2026 (sesión tarde) — Medicamentos suspendidos visibles y reactivables

### 1. Problema detectado
Al suspender un medicamento desaparecía del dashboard y de la lista, dificultando su reactivación. No estaba eliminado, pero no había forma de recuperarlo.

### 2. Solución implementada
- Se cambió la fuente de datos en la lista de medicamentos para mostrar **todos los medicamentos** (activos y suspendidos), ordenando activos primero y suspendidos después.
- Se modificó `obtenerInsumosProgramadosDelDia()` en `MainActivity.kt` para incluir medicamentos suspendidos en el dashboard.
- Se agregó el parámetro `isSuspended` a `MetallicMedicationCard` para estilizar tarjetas suspendidas con fondo gris claro y contenido oscuro.
- En el dashboard, las tarjetas suspendidas muestran:
  - Nombre con etiqueta **"(Suspendido)"**.
  - Horarios programados como texto informativo (no interactivo).
  - Botón **"Reactivar"** que vuelve a activar el medicamento y reprograma sus alarmas si están activas.
- Se cambió el título del listado de **"Medicamentos en uso"** a **"Medicamentos"** para reflejar que incluye ambos estados.

### 3. Lugares modificados
- `MainActivity.kt:obtenerInsumosProgramadosDelDia` — eliminado filtro `estaActivo` y ordenado suspendidos al final.
- `MainActivity.kt:DashboardMedicationPage` — UI diferenciada para suspendidos, botón de reactivar y colores adaptativos.
- `MainActivity.kt:MetallicMedicationCard` — soporte de estilo visual para `isSuspended`.
- `MainActivity.kt:listado de medicamentos` — fuente cambiada a `observarTodosPorPaciente`.

### 4. Compilación e instalación
- `BUILD SUCCESSFUL` con `gradlew.bat :app:compileDebugKotlin --no-daemon` después de los ajustes visuales.
- `BUILD SUCCESSFUL` con `gradlew.bat :app:installDebug --no-daemon` en dispositivo `SM-S918U - 16`.
- APK instalado correctamente: `Installed on 1 device.`

---

## 20 de junio de 2026 (sesión tarde) — Refactor del panel de Signos Vitales

### 1. Motivación
El panel de Signos Vitales se había vuelto excesivamente largo: incluía el formulario de entrada, el listado de todos los registros guardados, la exportación a Word y el IMC, todo en la misma pantalla. El objetivo fue separar el listado de registros en una pantalla dedicada, simplificar el panel principal y organizar el historial por meses desplegables.

### 2. Cambios en `MainActivity.kt`

#### Estados nuevos
- `mostrarListadoSignosPanel`: controla la visibilidad de la nueva pantalla de listado de registros.
- `mesesExpandidosSignos`: lista de meses (clave `yyyy-MM`) actualmente desplegados en el acordeón.

#### Funciones helper añadidas
- `yearMonthKey(timestamp)`: devuelve `"yyyy-MM"` para agrupar registros.
- `formatYearMonthLabel(timestamp)`: devuelve `"Mes Año"` en español (ej. `"Junio 2026"`).

#### Simplificación del panel principal de Signos Vitales
- Solo conserva los campos de entrada (presión, latidos, glucemia, temperatura, peso), el IMC calculado, el botón **"Guardar registro"** y el botón **"Volver al escritorio"**.
- Eliminados del panel principal:
  - Botón **"Cerrar"** (duplicado con "Volver al escritorio").
  - Botón **"Restaurar desde backup"** (ya existe en el panel de Copias de Seguridad).
  - Botón **"Exportar a Word"** y toda la sección de listado inline.
- El recordatorio diario se convirtió en un **icono de campana** en la fila inferior:
  - Púrpura cuando está activo, blanco cuando está inactivo.
  - Toque activa/desactiva el recordatorio y abre el selector de hora al activar.
  - Se corrigió el icono: `Icons.Filled.NotificationsNone` y `Icons.Default.NotificationsOff` no existían; se usa `Icons.Filled.Notifications` con cambio de color.

#### Nueva pantalla de listado de registros
- Pantalla completa con fondo rojo metálico distinto al panel principal.
- **Filtro de fechas** y botón **"Exportar a Word (.docx)"** con rango personalizado.
- **Listado agrupado por mes/año** con acordeón desplegable:
  - Cada mes muestra su nombre, año y cantidad de registros.
  - Toque en la cabecera expande/contrae el mes.
  - Checkbox en la cabecera del mes para seleccionar/deseleccionar todos los registros de ese mes.
  - Dentro del mes desplegado: checkbox por registro individual con fecha/hora.
  - Botón **"Exportar mes seleccionado (.docx)"** aparece cuando hay registros seleccionados dentro del mes expandido.
- Botones globales:
  - **"Seleccionar todos"** / **"Quitar todo"**.
  - **"Limpiar selección"**.
  - **"Visualizar selección"**.
  - **"Exportar selección (.docx)"**.
- Vista previa de registros seleccionados con todos los detalles clínicos.

#### Navegación corregida
- Al abrir el listado: `mostrarPanelSignosVitales = false` y `mostrarListadoSignosPanel = true`.
- Al cerrar el listado (botón ✕, botón atrás del sistema o `cerrarPanelesSecundarios()`): `mostrarListadoSignosPanel = false` y `mostrarPanelSignosVitales = true`.
- `mostrarListadoSignosPanel` se añadió a `cerrarPanelesSecundarios()` para limpiar el estado al volver al escritorio.
- `mesesExpandidosSignos` se limpia al cerrar el panel para evitar que queden meses abiertos entre visitas.

#### Corrección de eventos táctiles
- El panel de listado usaba inicialmente `.drawWithCache { onDrawBehind { ... } }` para el fondo, pero eso no bloquea los toques de los elementos debajo. Se cambió a `.background(Brush...)` para que el panel intercepte correctamente los eventos táctiles.

### 3. Revisión de copias de seguridad
Se verificó que `BackupManager` ya incluye `SignosVitales` en exportación, importación y restauración manual:
- `BackupSummary` y `BackupSelection` cubren **vitalSigns**.
- `buildBackupJson` exporta todos los campos de `SignosVitales` (`sistolica`, `diastolica`, `comentarioPresion`, `latidos`, `comentarioLatidos`, `glucemia`, `comentarioGlucemia`, `temperatura`, `comentarioTemperatura`, `peso`, `pesoUnidad`, `imc`, `fechaRegistro`).
- `restoreFromJson` restaura los registros desde el JSON directo o desde el `syncSnapshot`.
- `restoreOnlyVitalSigns` permite restaurar solo signos vitales desde un backup.

**Bug corregido en `AndroidSyncSnapshotMapper.kt`:**
- El mapper de `SignosVitales.toSyncModel()` convertía valores `null` de `sistolica`, `diastolica` y `latidos` a `0` (`sistolica ?: 0`), lo cual distorsionaba los registros incompletos al restaurar desde el `syncSnapshot` de un backup.
- Se corrigió para preservar los `null` originales en `SyncVitalSigns`, alineando el comportamiento con el backup JSON directo y manteniendo la integridad de los datos.

### 4. Diálogo de confirmación para eliminar documentos (corrección urgente)
**Problema:** El botón "Eliminar" de documentos/informes en el panel de salud ejecutaba la eliminación directamente sin confirmación, causando borrados accidentales.

**Solución:**
- Se añadió el estado `reportePendienteDeEliminar: MedicalReport?`.
- El botón "Eliminar" de cada documento ahora solo asigna el reporte a ese estado.
- Se agregó un `AlertDialog` con:
  - Título: **"Eliminar documento"**.
  - Mensaje: `"¿Estas seguro de eliminar \"${reporte.titulo}\"? Esta accion no se puede deshacer."`.
  - Botón de confirmar con icono `Icons.Filled.Check` (verde).
  - Botón de cancelar con icono `Icons.Filled.Close` (rojo).
- Solo tras confirmar se ejecuta `database.medicalReportDao().eliminar(reporte)` y se muestra el Toast "Documento eliminado".

### 5. Compilación e instalación
- `BUILD SUCCESSFUL` con `gradlew.bat :app:installDebug --no-daemon` en dispositivo `SM-S918U - 16`.
- APK instalado correctamente: `Installed on 1 device.`
- Varios intentos de compilación por errores menores de iconos inexistentes y de visibilidad, todos resueltos.

---

## 14 de junio de 2026 (sesión tarde) — Alertas de signos vitales + notificaciones al boot

### 1. Corrección de compilación (refactor manual del usuario)
**Problema:** El usuario renombró manualmente `incluirInsumos` → `incluirMedicamentos` en `ReporteClinicoScreen.kt` y `ReporteClinicoExporter.kt`, pero `MainActivity.kt` seguía llamando a `compilarReporteClinico(..., incluirInsumos = true, ...)`.

**Fix:** `@MainActivity.kt:2212` — `incluirInsumos = true` → `incluirMedicamentos = true`.

**Compilación:** `BUILD SUCCESSFUL` con `assemblePlaystoreDebug`.

---

### 2. Notificación de signos vitales → pantalla completa + navegación directa
**Motivación:** El recordatorio de signos vitales solo mostraba una notificación normal que al tocar llevaba al escritorio. Se mejoró para que sea una alerta de pantalla completa igual que las de medicamentos, y que al tocar vaya directo al panel de signos vitales.

**Cambios:**

#### NotificacionHelper.kt
- `mostrarRecordatorioSignosVitales()`:
  - Nuevo extra `EXTRA_OPEN_SIGNOS_VITALES` agregado al companion object.
  - Notificación ahora usa `PRIORITY_MAX`, `CATEGORY_ALARM`, `setOngoing(true)`, `setAutoCancel(false)`.
  - Agregado `setFullScreenIntent(fullScreenPendingIntent, true)` para que aparezca sobre pantalla de bloqueo.
  - Agregado botón de acción "Registrar ahora" que envía broadcast `ACTION_ACCEPT_SIGNOS_VITALES`.
  - El `fullScreenIntent` apunta a `MainActivity` con extras `EXTRA_LAUNCH_CRITICAL_ALERT = true` + `EXTRA_OPEN_SIGNOS_VITALES = true` + `patientId`.

#### AlarmReceiver.kt
- Agregada constante `ACTION_ACCEPT_SIGNOS_VITALES = "com.carlos.controlmedicamentos.notifications.ACCEPT_SIGNOS_VITALES"`.
- Nuevo bloque de manejo: al recibir esta acción, cancela la notificación y lanza `MainActivity` con los extras de navegación a signos vitales.

#### MainActivity.kt
- `MedicamentoForm()` ahora acepta parámetro `launchIntent: Intent? = null`.
- En `onCreate()` se pasa `intent` a `MedicamentoForm(..., launchIntent = intent)`.
- Nuevo `LaunchedEffect(launchIntent)` que detecta `EXTRA_OPEN_SIGNOS_VITALES` y:
  - Cierra todos los paneles secundarios (`mostrarFormulario = false`, `mostrarFichaPaciente = false`, etc.).
  - Abre directamente `mostrarPanelSignosVitales = true`.

**Compilación:** `BUILD SUCCESSFUL` con `assemblePlaystoreDebug` (4m 12s).

---

### 3. Detección de tomas perdidas al encender el teléfono (BOOT_COMPLETED)
**Motivación:** Cuando el teléfono está apagado en el momento de una toma programada, la alarma se pierde. Al encender, la app ahora detecta tomas de medicamentos y anticonceptivos que deberían haber ocurrido mientras estuvo apagado y muestra una notificación resumen.

**Cambios:**

#### BootReceiver.kt (reescrito completamente)
- Guarda timestamp del último boot en `SharedPreferences` (`boot_receiver_prefs` / `last_boot_time`).
- Al recibir `BOOT_COMPLETED` o `MY_PACKAGE_REPLACED`:
  1. Reprograma todas las alarmas (medicamentos, citas, vacunas, signos vitales, anticonceptivos).
  2. Lee `lastBootTime`. Si existe (>0), calcula rango de lookback (máximo 3 días).
  3. Llama a `detectarYNotificarTomasPerdidas()` y `detectarYNotificarAnticonceptivosPerdidos()`.
- `detectarYNotificarTomasPerdidas()`:
  - Itera todos los medicamentos activos con alarma.
  - Calcula todas las tomas programadas en el rango `[lastBootTime, now]`.
  - Verifica en `medication_intakes` si cada toma fue registrada.
  - Si hay tomas sin registro, agrupa por paciente y muestra notificación `mostrarTomasPerdidasBoot()` con lista detallada.
- `detectarYNotificarAnticonceptivosPerdidos()`:
  - Itera todos los métodos anticonceptivos activos.
  - Solo evalúa métodos con `requiereAlarmaDiaria = true` (píldora, minipíldora; parche 7 días; anillo 30 días).
  - Calcula tomas programadas en el rango.
  - Verifica en `anticonceptivo_intakes` si cada toma fue registrada.
  - Si hay tomas sin registro, muestra notificación separada.
- Funciones helper locales: `scheduledDoseTimesInRange()`, `scheduledAnticonceptivoTimesInRange()`, `inicioDelDia()`, `truncateToMinute()`, `formatHora()`.

#### NotificacionHelper.kt
- Nueva función `mostrarTomasPerdidasBoot()`:
  - Notificación con estilo `InboxStyle` (lista de líneas).
  - `PRIORITY_HIGH`, `CATEGORY_REMINDER`.
  - `contentIntent` apunta a `MainActivity` con `EXTRA_LAUNCH_CRITICAL_ALERT`.
  - IDs: `300_000` (medicamentos), `300_001` (anticonceptivos).

#### MetodoAnticonceptivoDao.kt
- Agregado método `suspend fun obtenerActivos(): List<MetodoAnticonceptivo>` (sin parámetro `patientId`) para obtener todos los métodos activos del sistema.

**Compilación:** `BUILD SUCCESSFUL` con `assemblePlaystoreDebug` (4m 26s).

---

## Estado actual
Aplicación Android en Kotlin con Jetpack Compose y Room para gestionar medicamentos por paciente, registrar tomas, guardar informes médicos, citas, vacunación, signos vitales, pedidos y realizar copias de seguridad manuales y automáticas.

## Cambios recientes - Checkbox Control Niño Sano (PENDIENTE DE RESTAURAR)
**IMPORTANTE:** Estos cambios deben reaplicarse después de restaurar MainActivity.kt desde copia de seguridad.

### 1. PatientProfile.kt
- Agregar campo `controlNinoSanoActivo: Boolean = false` al data class

### 2. AppDatabase.kt
- Incrementar versión de base de datos a 30
- Agregar migración MIGRATION_29_30:
  ```kotlin
  private val MIGRATION_29_30 = object : Migration(29, 30) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE patient_profile ADD COLUMN controlNinoSanoActivo INTEGER NOT NULL DEFAULT 0")
      }
  }
  ```
- Agregar MIGRATION_29_30 a la lista de migraciones en addMigrations()

### 3. MainActivity.kt
- Agregar variable de estado: `var controlNinoSanoActivo by remember { mutableStateOf(false) }`
- En `resetFichaPaciente()`: agregar `controlNinoSanoActivo = false`
- En `cargarFichaPaciente()`: agregar `controlNinoSanoActivo = perfilConCumplePersistido.controlNinoSanoActivo`
- En guardado del perfil: agregar `controlNinoSanoActivo = controlNinoSanoActivo` al crear PatientProfile
- En selector de sexo (radio buttons): agregar lógica para marcar automáticamente:
  ```kotlin
  onClick = {
      sexoPaciente = opcion
      controlNinoSanoActivo = (opcion == "Mujer")
  }
  ```
- Agregar checkbox en formulario de perfil (después del selector de sexo):
  ```kotlin
  if (editandoFichaPaciente) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
      ) {
          Checkbox(
              checked = controlNinoSanoActivo,
              onCheckedChange = { controlNinoSanoActivo = it }
          )
          Column(modifier = Modifier.padding(start = 8.dp)) {
              Text("Activar control niño sano", color = Color.White, fontSize = 14.sp)
              Text(if (sexoPaciente == "Mujer") "Por defecto en mujeres" else "Opcional en hombres",
                  color = Color.Gray, fontSize = 12.sp)
          }
      }
  }
  ```
- En menú hamburguesa: envolver DropdownMenuItem "Control Niño Sano" con condición:
  ```kotlin
  if (pacienteActivo?.controlNinoSanoActivo == true) {
      DropdownMenuItem(...)
  }
  ```

### Comportamiento esperado
- Al seleccionar "Mujer" como sexo, el checkbox se marca automáticamente
- Al seleccionar "Hombre" como sexo, el checkbox se desmarca automáticamente
- El usuario puede cambiar manualmente el estado del checkbox
- El ítem "Control Niño Sano" solo aparece en el menú si el checkbox está marcado
- Funciona tanto para hombres como para mujeres

## Lo realizado hasta ahora
- Estructura principal activa:
  - `data/local`
  - `data/remote`
  - `notifications`
  - `backup`
  - `ui`
  - `sync`
- Modulo compartido activo:
  - `sync-core`
- Persistencia local con Room implementada en `AppDatabase`.
- Entidades Room activas:
  - `Medication`
  - `MedicationIntake`
  - `PatientProfile`
  - `MedicalReport`
  - `MedicalAppointment`
  - `MedicalPractitioner`
  - `VaccinationRecord`
  - `SignosVitales`
  - `MedicationOrder`
  - `PhysicalActivity`
  - `CarritoPendienteItem`
  - `CicloMenstrual`
  - `MetodoAnticonceptivo`
  - `RegistroTomaAnticonceptivo`
  - `SintomaInterPeriodo`
  - `MensajeChat`
- DAOs activos:
  - `MedicationDao`
  - `MedicationIntakeDao`
  - `PatientProfileDao`
  - `MedicalReportDao`
  - `MedicalAppointmentDao`
  - `MedicalPractitionerDao`
  - `VaccinationRecordDao`
  - `SignosVitalesDao`
  - `MedicationOrderDao`
  - `PhysicalActivityDao`
  - `CarritoPendienteDao`
  - `CicloMenstrualDao`
  - `MetodoAnticonceptivoDao`
  - `RegistroTomaAnticonceptivoDao`
  - `SintomaInterPeriodoDao`
  - `MensajeChatDao`
- Formulario principal Compose consolidado en `MainActivity`.
- Vademécum local de ejemplo implementado:
  - `FakeVademecumRepository`
  - `VademecumMedication`
- Alta y edición de medicamentos con:
  - sugerencias por nombre
  - selección de formato y concentración
  - dosis única o repartida en distintos horarios
  - frecuencia diaria, semanal, mensual y ciclos personalizados
  - fechas reales de inicio y fin
- Modulo independiente de citas medicas:
  - agenda separada del panel de pacientes
## Estado de restauración (Abril 2026)

**Rollback aplicado:**
- Eliminados módulos y referencias de vacunación, signos vitales, pedidos WhatsApp, panel de facultativos, alertas críticas, exportación Word/docx y funciones Compose agregadas después del 17/04/2026.
- App compila y genera APK funcional sin errores de referencias rotas.
- Warnings de estilo corregidos.

**Plan de recuperación modular:**
1. Validar funcionamiento base de la app (instalación, registro de medicamentos, citas, informes).
2. Recuperar y probar cada módulo eliminado, uno por uno, documentando cambios y errores en este dossier.
3. Restaurar primero signos vitales, luego pedidos WhatsApp, facultativos, alertas críticas, exportación Word/docx y funciones visuales avanzadas.
4. Mantener siempre una versión funcional y documentar cada paso.

**Última instalación debug verificada:**
- 19 de abril de 2026 en entorno limpio tras rollback, sin errores de compilación.
  - navegación horizontal por fechas con scroll centrado en la parte superior
  - el día centrado del escritorio se agranda visualmente para facilitar la lectura
  - cabecera fija fuera del scroll
  - eliminación del título fijo `Escritorio de medicamentos`
  - eliminación del contador visible de medicamentos programados
  - fondo azul metalico restaurado en el escritorio principal
  - textos del escritorio ajustados a blanco para mejorar contraste
  - navegacion interna por pantallas con soporte del boton atras nativo de Android en los flujos principales
- Formulario de medicamentos alineado con el escritorio:
  - contenedor principal con acabado metalico azul
  - textos del formulario en blanco
  - campos del formulario adaptados a la paleta azul del escritorio
  - botones principales del formulario ajustados a la misma gama azul
- Fondo general de la aplicación con estilo azul metálico generado en Compose.
- Panel de pacientes y ficha de paciente con fondo azul integrado al mismo lenguaje visual del escritorio.
- Tarjetas de medicamentos del escritorio y del listado con estilo metálico azul.
- Registro persistente de pedidos de medicamentos:
  - tabla `medication_orders` en Room para guardar historial de pedidos enviados
  - total estimado por pedido segun precios por unidad configurados
  - historial por paciente con fecha, destino y resumen del pedido
- Reporte independiente de consumo y gasto de medicamentos desde el menú hamburguesa:
  - pantalla propia separada de la lista de medicamentos
  - filtro desplegable por día, semana, mes o año usando la fecha del escritorio como base
  - boton `Ver` para mostrar solo el periodo elegido
  - boton `Exportar` a archivo CSV compatible con Excel
  - resumen de tomas, gasto estimado, cobertura INSS y tomas sin precio
  - detalle por medicamento e historial de tomas con fecha y hora marcada
- Flujo de pedidos por WhatsApp mejorado:
  - boton `Añadir a la lista de pedidos` tipo carrito
  - al añadir al carrito se pide cuantas unidades se desean solicitar
  - envio unico por WhatsApp con todos los medicamentos acumulados
  - soporte para WhatsApp normal y WhatsApp Business al abrir el contacto manual
- Usabilidad del panel de medicamentos mejorada:
  - scroll vertical interno en cada tarjeta de medicamento
  - scroll vertical interno en la tarjeta de lista de pedidos
  - teclado numerico en campos de numeros, decimal para precio y telefono para WhatsApp
- Sistema de notificaciones y alarmas:
  - `AlarmReceiver`
  - `MedicalAppointmentScheduler`
  - `MedicationScheduler`
  - `VaccinationScheduler`
  - `NotificacionHelper`
  - las notificaciones de medicamentos, citas y vacunas muestran el nombre del paciente para facilitar el control simultaneo de varios registros familiares
  - `CriticalAlertService`
  - `CriticalAlertSettings`
  - registro de toma desde la notificación cuando corresponde
  - alertas críticas con reproducción doble y reintento persistente hasta interacción
  - configuración global de sonido e intervalo de reintento desde el menú hamburguesa
  - uso de permisos de alarmas exactas, notificaciones y acceso a No molestar
- Sistema de copias de seguridad:
  - exportación manual
  - importación manual
  - backup automático diario o semanal
  - restauración de medicamentos, pacientes, informes y tomas
  - panel de copias de seguridad con fondo naranja degradado inspirado en la referencia visual mas reciente
- Exportación de tomas a documento RTF por rango de fechas basado en la fecha seleccionada del escritorio.
- Eliminación de la integración con IA/Gemini del flujo actual del proyecto.

### Cambios recientes en estadísticas de uso
- `StatisticsScreen.kt` actualizado para mostrar estadísticas reales de uso en un panel de diálogo que ocupa toda la pantalla disponible debajo de la barra superior.
- Se agregó selección de mes y cálculo por insumo para mostrar cuántas tomas tuvo cada medicamento en el período seleccionado.
- El listado de estadísticas ahora se renderiza con scroll vertical y manejo de textos largos para evitar cortes visuales.
- Se creó `StatisticsPdfExporter.kt` para exportar las estadísticas mensuales a PDF en la carpeta Descargas.
- Se guarda un registro en la base de datos usando `MedicalReport` con el PDF adjunto, para que el historial de estadísticas quede persistido.
- `ReporteClinicoExporter.kt` reforzado para incluir la sección de estadísticas por insumo en los resúmenes exportados.
- Como el backup ya incluye `medical_reports`, esta nueva información queda incluida en las copias de seguridad manuales y automáticas.
- Reparamos el viejo panel de "Estadísticas de uso" que antes mostraba información placeholder: ahora consume datos reales de Room, filtra por mes, agrupa por medicamento e incluye exportación y persistencia.

## Arquitectura actual
- La UI sigue concentrada principalmente en `MainActivity`, con varias secciones del producto resueltas dentro de una sola pantalla Compose.
- Room es la fuente de verdad local para medicamentos, tomas, pacientes, informes y signos vitales.
- Room tambien conserva la agenda de citas medicas con sus recordatorios por paciente.
- Room ahora tambien conserva el historial de vacunacion y las proximas dosis programadas por paciente.
- La pantalla de vacunacion ya calcula sugerencias segun edad del paciente cuando existe fecha de nacimiento registrada.
- La configuración crítica compartida de alertas se guarda fuera de Room en preferencias locales para aplicarse globalmente a todos los medicamentos.
- El vademécum sigue siendo local y simulado, útil como apoyo de captura pero todavía sin API real.
- Las alarmas y notificaciones ya forman parte del flujo operativo del tratamiento.
- El módulo de backup ya cubre exportación, importación y programación automática.
- El modulo `sync-core` ya define el contrato de snapshot compartido para una futura app de Windows y sincronizacion bidireccional.
- El versionado de la app para Play Store ahora se controla desde `gradle.properties` con `APP_VERSION_CODE` y `APP_VERSION_NAME`.

## Archivos principales
- `app/src/main/java/com/carlos/controlmedicamentos/MainActivity.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/AppDatabase.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/Medication.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicationDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicationIntake.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicationIntakeDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/PatientProfile.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/PatientProfileDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicalReport.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicalReportDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicalAppointment.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicalAppointmentDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicalPractitioner.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/MedicalPractitionerDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/VaccinationRecord.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/VaccinationRecordDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/SignosVitales.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/SignosVitalesDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/remote/FakeVademecumRepository.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/notifications/AlarmReceiver.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/notifications/MedicalAppointmentScheduler.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/notifications/MedicationScheduler.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/notifications/VaccinationScheduler.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/notifications/NotificacionHelper.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/notifications/CriticalAlertSettings.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/backup/BackupManager.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/backup/AutoBackupScheduler.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/backup/AutoBackupWorker.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/sync/AndroidSyncSnapshotMapper.kt`
- `sync-core/src/main/kotlin/com/carlos/controlmedicamentos/sync/model/SyncModels.kt`

## Qué funciona ahora
- Compilación correcta con `./gradlew assembleDebug` y `./gradlew :app:compileDebugKotlin`.
- Instalación correcta con `./gradlew :app:installDebug` en dispositivo conectado.
- Instalacion debug verificada el 7 de mayo de 2026 en `SM-X115 - Android 16` con `BUILD SUCCESSFUL`.
- Instalacion debug verificada el 17 de abril de 2026 en `SM-S918U - Android 16` con `BUILD SUCCESSFUL`.
- Base de datos Room en **versión 41** con 17 entidades activas (`ControlEmbarazo` y `VisitaPrenatal` añadidas en v25; tabla `subscription_state` eliminada en v41 al migrar a modelo gratuito con publicidad).
- Módulo de actividad física con podómetro, GPS, TTS y seguimiento de distancia/calorías/altitud máxima.
- Módulo de salud reproductiva: seguimiento del ciclo menstrual, síntomas inter-período y predicción de próximo período.
- Módulo de anticonceptivos: alta y seguimiento de métodos orales, periódicos y de larga duración con alarmas propias.
- Asistente MediAI: chat con Gemini 2.0 Flash contextualizado con todos los datos del paciente, historial de conversación persistido en Room.
- ~~Pantalla de suscripción con periodo de prueba de 30 días, integración con Google Play Billing y preferencias cifradas.~~ **Eliminada en Junio 2026** — Ahora la app es gratuita con publicidad (AdMob banner en la parte inferior).
- **Publicidad integrada:** Banner de AdMob en la parte inferior de la pantalla principal usando ID de pruebas de Google (`ca-app-pub-3940256099942544/6300978111`).
- `AlarmAlertActivity`: pantalla de alarma a pantalla completa que aparece sobre la pantalla de bloqueo.
- `NuevaVacunaActivity`: actividad separada para el formulario de vacunas.
- El escritorio principal queda funcional y completado en su alcance actual, con cabecera fija, carrusel de fechas sincronizado y contenido diario paginado operativo.
- Registro y edición de medicamentos asociados al paciente activo.
- Formulario de medicamentos visualmente integrado con el escritorio azul metalico.
- Visualización de medicamentos programados por fecha.
- Selector horizontal tipo carrusel para cambiar el día del escritorio sin abrir calendario, con scroll lateral, fecha activa centrada, magnificada y desvanecido suave en los bordes.
- Flechas laterales del selector ajustadas para avanzar o retroceder un dia por toque.
- Cabecera fija con perfil y menú siempre visibles en el escritorio.
- Tarjeta de perfil con acabado metalico verde.
- Tarjetas de medicamentos con acabado metalico azul.
- Ficha de perfil en modo vista y edición con acciones por iconos.
- Marcado y desmarcado manual de tomas registradas.
- El escritorio muestra cada horario del dia con tipografia roja si la dosis sigue pendiente y verde si ya fue tomada.
- El escritorio muestra la hora real de la toma registrada debajo de la hora programada cuando una dosis ya fue marcada, usando el valor persistido de `acceptedAt`.
- El desmarcado de una toma desde el escritorio pasa por un dialogo de confirmacion con accion explicita de aceptar o cancelar.
- Activación de un paciente actual y filtrado del contenido por paciente.
- Registro y consulta de informes médicos con adjuntos.
- Pantalla de informes medicos corregida para mostrar los datos, analiticas y capturas en una pila vertical desplazable en movil.
- Edicion de informes medicos recuperando los adjuntos con la misma decodificacion usada por el visor.
- Formulario de informes medicos ajustado para volver a mostrar siempre los botones de guardar y cerrar en movil, incluso cuando ya existen capturas adjuntas.
- Visualizacion de adjuntos en el formulario reconvertida a una galeria horizontal para poder recorrer varias imagenes y abrirlas en el visor sin perder accesibilidad a las acciones principales.
- Panel de lista de informes ajustado para mostrar arriba las acciones rapidas de `Nuevo informe`, `Escritorio` y exportacion, evitando que desaparezcan visualmente cuando hay muchos informes en pantalla.
- Miniaturas del listado principal de informes reorganizadas en filas visibles para que todas las capturas queden expuestas en la tarjeta y no parezca que faltan imagenes.
- Formulario de informes reforzado con una fila temprana de `Guardar` y `Cerrar` tras el titulo, junto con `imePadding` y `navigationBarsPadding`, para evitar que las acciones queden fuera de alcance en moviles pequeños.
- Se reactiva el modulo `Medicos` en el menu hamburguesa con formulario propio para los profesionales habituales del paciente, reutilizando `MedicalPractitioner` y mostrando `Proxima cita` e `Informes` como datos sincronizados desde los modulos de citas medicas e informes medicos.
- Tarjetas del listado principal reorganizadas para mostrar antes las fotos y recortar descripcion/analisis, priorizando la visibilidad de adjuntos sin perder acceso a editar o eliminar.
- Contenedor principal de desplazamiento ajustado para respetar barras del sistema y teclado, evitando que el tramo final de informes y botones quede oculto en la parte inferior.
- Al seleccionar un facultativo en el formulario, la UI ya no añade dos campos altos que empujaban las acciones fuera de pantalla; ahora se resume en un bloque compacto.
- Cada informe del listado ahora ofrece tambien un boton `Ver capturas` para abrir el visor completo aunque la tarjeta este compactada.
- Agenda de citas medicas por paciente con alta, edición, eliminación, marcado como realizada y alarmas configurables.
- Registro de facultativos en menu hamburguesa con nombre, especialidad y resumen de citas pasadas o futuras asociadas.
- Vinculacion de citas medicas con facultativos registrados, tomando el nombre del profesional desde ese catalogo cuando existen registros.
- Modulo propio de citas medicas accesible desde el menu hamburguesa, separado de pacientes.
- Historial de citas en pantalla independiente con acciones para editar, marcar, reactivar alarma o eliminar.
- Historial de vacunacion por paciente con registro de aplicacion, proxima dosis, alarma y edicion completa.
- Aceptacion de citas desde la notificacion para marcarlas como realizadas y mantenerlas en el historial.
- Registro manual de presión arterial, latidos y glucemia desde panel dedicado.
- Registro manual de presión arterial, latidos, glucemia, temperatura y comentarios clinicos asociados.
- Panel de signos vitales con acabado rojo metálico integrado al lenguaje visual de la app.
- Panel principal de signos vitales simplificado: solo campos de entrada, "Guardar registro", "Volver al escritorio" e icono de recordatorio diario.
- Pantalla dedicada para el listado de registros de signos vitales con fondo rojo metálico distinto.
- Listado de signos vitales agrupado por mes y año con acordeón desplegable; selección individual, por mes y exportación a Word (.docx) del mes seleccionado.
- Visualizacion y exportacion a Word de registros seleccionados de signos vitales con filtros de fecha.
- Menú hamburguesa con acceso a reporte independiente de consumo y gasto de medicamentos.
- Listado de signos vitales en pantalla independiente con seleccion compacta y exportacion desde una vista separada del formulario.
- Exportación e importación de copias de seguridad.
- Programación de backups automáticos.
- Los backups ya conservan tambien el historial de vacunacion y sus proximas dosis programadas.
- Los backups ahora tambien conservan el historial de pedidos de medicamentos y su gasto estimado.
- Exportación de historial de tomas en RTF.
- Exportacion de signos vitales en `.docx` con filtros y seleccion manual.
- Alertas críticas con configuración global compartida de sonido y reintento.
- Panel de configuracion de alertas con tarjeta y fondo exterior negros para mantener una vista uniforme.
- Portada inicial animada con acceso por toque al escritorio.
- Versionado configurable para Play Store desde `gradle.properties`.

## Qué queda por hacer

### Pendiente inmediato — Control de embarazo
- Ampliar `BackupManager` para exportar e importar las tablas `control_embarazo` y `visita_prenatal`; incrementar `SCHEMA_VERSION` a 9.
- Actualizar `AndroidSyncSnapshotMapper` en `sync-core` para incluir los datos de embarazo en el snapshot de sincronización con Windows.
- Validar en el dispositivo real que el menú hamburguesa cambia dinámicamente al iniciar y cerrar un embarazo.
- Considerar añadir un acceso rápido al control de embarazo desde el escritorio principal cuando hay un embarazo activo (banner o icono 🤰 en la cabecera).
- Revisar que `EmbarazoScreen` calcula correctamente las semanas de gestación al registrar cada visita prenatal y que los diálogos guardan todos los campos sin pérdida.
- Probar el flujo completo: iniciar embarazo → registrar visitas → registrar parto → verificar que `activo` pasa a 0 y el historial queda accesible.
- Verificar que al cerrar el parto (activo=0) el menú hamburguesa vuelve a mostrar Ciclo menstrual y Anticonceptivos automáticamente sin reiniciar la app.

### Pendiente general
- Separar la lógica de `MainActivity` en pantallas, componentes y ViewModels.
- Añadir edición de registros, gráficas de tendencias y alertas personalizadas a los signos vitales.
- Si se quiere usar exactamente la fotografía original del arranque como recurso incrustado, añadir el archivo binario final al proyecto para sustituir la ilustración Compose inspirada en esa imagen.
- Mejorar el diseño visual general para que el escritorio y paneles tengan una jerarquía más clara.
- Si se quiere un calendario vacunal exacto por país, adaptar el catálogo base y reglas de refuerzo a la pauta oficial del sistema sanitario correspondiente.
- Sustituir el vademécum local por una fuente remota real cuando exista backend o API disponible.
- Añadir más pruebas de regresión para flujos clave de medicamentos, backups y notificaciones.
- Revisar y reducir advertencias deprecadas de Compose y `Locale` que todavía existen en otras zonas del proyecto.
- Validar en distintos fabricantes el comportamiento real de alertas críticas, ya que Android no garantiza saltarse todas las restricciones de volumen o personalizaciones del sistema.
- Separar la pantalla de informes médicos y otros paneles grandes de `MainActivity` para reducir regresiones de scroll y layout en móvil.

## Ultima actualizacion registrada
### Mayo 2026
- **Problema persistente resuelto**: el formulario de medicamentos solo mostraba la mitad inferior de la pantalla en todos los dispositivos probados.
  - Causa raíz: `MetallicMedicationCard` usaba `weight(1f)` dentro del Column `!mostrarEscritorio` que a su vez tenía `weight(1f)` en el Column raíz. La doble anidación de `weight` provocaba que el algoritmo de distribución de espacio de Compose asignara solo la mitad del área disponible al formulario.
  - Intentos fallidos documentados: eliminar `BoxWithConstraints` + `height(maxHeight)`, cambiar `fillMaxSize()` por `fillMaxWidth()` en el Column interno, añadir `fillMaxHeight()` explícito — ninguno surtió efecto.
  - Solución definitiva: cambiar el modifier de `MetallicMedicationCard` de `Modifier.fillMaxWidth().weight(1f)` a `Modifier.fillMaxSize()`. Con `fillMaxSize()`, la card rellena directamente las restricciones máximas del Column padre sin pasar por el algoritmo de peso, eliminando el conflicto de medición.
  - El `Column` interno del formulario conserva `verticalScroll(panelInternoScrollState)` y `fillMaxWidth()` para el scroll propio del contenido.
  - `expandVertically = true` se mantiene en `MetallicMedicationCard` para que el gradiente azul cubra toda la altura de la tarjeta.
- Formulario de medicamentos: añadido `Spacer` de 48dp arriba y abajo del contenido para evitar que el título y el botón "Volver al escritorio" queden pegados a los bordes.
- Lista de medicamentos guardados: añadido `Spacer` de 48dp al final para que el botón "Volver al escritorio" no quede detrás de la barra de navegación del sistema.
- Instalacion debug verificada el 8 de mayo de 2026 en `SM-X115 - Android 16` con `BUILD SUCCESSFUL`.
- Formulario de medicamentos envuelto en `MetallicMedicationCard` con fondo azul metálico, título "Formulario de medicamento" y checkbox `¿Alarma activa?` añadido entre `¿Activo?` y `¿Controlar existencias?`.
- `mostrarFormulario` añadido a la condición del gradiente de fondo del `Column` raíz para que el fondo verde metálico se aplique también al abrir el formulario.
- El `Column` scrollable de los paneles tiene `weight(1f)`, `statusBarsPadding()`, `navigationBarsPadding()` e `imePadding()` en el orden correcto para que el scroll funcione.
- El formulario tiene su propio `Column` con `weight(1f)` + `verticalScroll(formScrollState)` independiente del resto de paneles, con padding 24dp arriba/abajo y 16dp laterales.
- Cuando `mostrarFormulario` está activo, tanto el `Column` exterior (verde) como el `Column` scrollable de paneles tienen padding 0 para no interferir con el padding propio del formulario.
- Padding uniforme en todos los demás paneles: 24dp arriba y abajo, 16dp laterales.
- Pendiente de revisión visual: el fondo verde todavía puede verse en la parte superior al abrir el formulario dependiendo del dispositivo — continuar mañana.
- Instalacion debug verificada el 7 de mayo de 2026 en `SM-X115 - Android 16` con `BUILD SUCCESSFUL`.
- El escritorio quedó funcional y completado para el alcance definido en esta fase, con cabecera fija, resumen de fecha animado en `Hoy`, selector superior sincronizado y paginas diarias operativas.
- El escritorio principal se consolidó con selector de fecha tipo carrusel horizontal en la parte superior, sin selector de calendario tradicional.
- La fecha seleccionada del escritorio queda siempre centrada y magnificada visualmente durante el scroll lateral.
- El viewport del carrusel ahora tiene desvanecido lateral para reforzar el efecto visual y dejar caer los extremos con suavidad.
- Las flechas laterales del escritorio ya no saltan semanas: avanzan o retroceden un dia por pulsacion.
- Los textos visibles del escritorio se mantuvieron en blanco sobre el fondo azul metálico para sostener contraste en cabecera, resumen de fecha y tarjetas.
- Las tarjetas de horarios del dia muestran la hora programada en rojo cuando sigue pendiente y en verde cuando ya fue tomada.
- Debajo de cada horario tomado se muestra la hora real registrada de la medicacion usando el dato persistido en `MedicationIntake.acceptedAt`.
- Se validó el estado actual con `./gradlew :app:compileDebugKotlin` y `./gradlew :app:installDebug` sin errores bloqueantes.
- La instalacion real mas reciente quedó verificada el 7 de mayo de 2026 en el dispositivo `SM-X115 - Android 16`.

### Junio 2026 — Migración a modelo gratuito con publicidad (AdMob)

#### Paso 1: Limpieza total de sistemas de pago
**Objetivo:** Eliminar completamente el sistema de billing para convertir la app en gratuita.

**Cambios realizados:**
- **Eliminadas dependencias de billing** (`app/build.gradle.kts`):
  - Eliminada dependencia Samsung IAP SDK (`libs/samsungiap.aar`)
  - Eliminada referencia a almacenamiento cifrado para tokens de suscripción

- **Archivos eliminados**:
  - `BillingManager.kt` — Gestor de pagos Samsung IAP
  - `SubscriptionManager.kt` — Gestor de tokens y período de prueba
  - `SubscriptionScreen.kt` — Pantalla de suscripción/muro de pago
  - `SubscriptionState.kt` — Entidad Room de estado premium
  - `SubscriptionStateDao.kt` — DAO de suscripción

- **Base de datos actualizada** (`AppDatabase.kt`):
  - Eliminada entidad `SubscriptionState::class` de la lista de entidades
  - Eliminado DAO `subscriptionStateDao()`
  - Versión incrementada de 40 a 41
  - Agregada migración `MIGRATION_40_41` que elimina la tabla `subscription_state`

- **MainActivity.kt simplificada**:
  - Eliminadas propiedades `subscriptionManager` y `billingManager`
  - Eliminado todo el bloque de lógica de suscripción (verificación de prueba, pagos, restauración)
  - Eliminados estados `isSubscribed`, `daysRemaining`, `mostrarPantallaSubscripcion`, `mostrarRecordatorio`
  - Eliminada integración de `BillingManager` y `SubscriptionScreen`
  - Ahora la app muestra directamente el **contenido principal gratuito** sin restricciones

#### Paso 2: Integración del SDK de Anuncios (Google Mobile Ads)
**Objetivo:** Preparar la estructura para recibir anuncios de AdMob.

**Cambios realizados:**
- **Dependencia añadida** (`app/build.gradle.kts`):
  ```kotlin
  // Google Mobile Ads SDK (AdMob)
  implementation("com.google.android.gms:play-services-ads:23.0.0")
  ```

- **AndroidManifest.xml actualizado**:
  - Eliminado permiso obsoleto `com.samsung.android.iap.permission.BILLING`
  - Agregado meta-data de AdMob con ID de pruebas oficial de Google:
    ```xml
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-3940256099942544~3347511713" />
    ```

- **MainActivity.kt inicializada**:
  - Import añadido: `com.google.android.gms.ads.MobileAds`
  - Inicialización del SDK en `onCreate()`:
    ```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ── Inicializar Google Mobile Ads SDK ────────────────────────
        MobileAds.initialize(this)
        // ...
    }
    ```

#### Paso 3: Colocación del banner de anuncios
**Objetivo:** Mostrar un banner discreto en la parte inferior de la pantalla principal.

**Cambios realizados:**
- **Imports añadidos** (`MainActivity.kt`):
  ```kotlin
  import com.google.android.gms.ads.AdRequest
  import com.google.android.gms.ads.AdSize
  import com.google.android.gms.ads.AdView
  import androidx.compose.ui.viewinterop.AndroidView
  ```

- **Composable `AdMobBanner` creado** (`MainActivity.kt` líneas 378-398):
  ```kotlin
  @Composable
  private fun AdMobBanner(modifier: Modifier = Modifier) {
      val adUnitId = "ca-app-pub-3940256099942544/6300978111" // ID de pruebas
      AndroidView(
          modifier = modifier,
          factory = { context ->
              AdView(context).apply {
                  setAdSize(AdSize.BANNER)
                  setAdUnitId(adUnitId)
                  loadAd(AdRequest.Builder().build())
              }
          },
          update = { adView ->
              adView.loadAd(AdRequest.Builder().build())
          }
      )
  }
  ```

- **UI reorganizada** (`MainActivity.kt`):
  - Estructura cambiada de `Box` a `Column` con distribución de espacio:
    - Contenido principal con `weight(1f)` — ocupa todo el espacio disponible
    - Banner de AdMob fijo en la parte inferior con `fillMaxWidth()`
  - El banner no interfiere con la usabilidad de los botones principales

**IDs de pruebas utilizados:**
- **App ID:** `ca-app-pub-3940256099942544~3347511713`
- **Banner Ad Unit ID:** `ca-app-pub-3940256099942544/6300978111`

**Build verificado:**
- `BUILD SUCCESSFUL` con `gradlew.bat build --no-daemon -x lint`
- 191 actionable tasks: 38 executed, 153 up-to-date
- Sin errores de compilación

**Nota para producción:** Reemplazar los IDs de pruebas con los IDs reales de AdMob antes de publicar en Play Store.

### Abril 2026
- El formulario de informes médicos ahora se alinea arriba y ocupa toda la pantalla, evitando que quede centrado o recortado y asegurando que los campos no se oculten tras el teclado.
- Los botones del visor de imágenes se subieron para que nunca queden cortados por la pantalla o el teclado.
- Eliminado el campo y botón de "Análisis IA" del formulario de informes médicos.
- Los botones de "Fotografiar" y "Galería" siempre quedan visibles, sin necesidad de hacer scroll.
- Mejorada la visibilidad y accesibilidad de las acciones principales en formularios y visores.
- Se verificó la instalación real con `./gradlew :app:installDebug` en el dispositivo `SM-S918U - Android 16` con resultado satisfactorio.
- Tambien se detecto que parte del final del contenido podia quedar oculto por la barra inferior del sistema; se corrigio reforzando el scroll raiz y anadiendo espacio inferior adicional al panel de informes.
- Al seleccionar `Facultativo del informe`, los campos resumen del profesional seguian empujando el formulario hacia abajo; se corrigio sustituyendolos por un resumen compacto y dejando `Guardar`/`Cerrar` en la parte superior del formulario.
- Sigue habiendo advertencias deprecadas en `MainActivity` relacionadas con `LocalLifecycleOwner`, `menuAnchor`, `Locale` e `Icons.Filled.DirectionsWalk`, pero no bloquean compilacion ni instalacion.
- Se aplico estilo metalico azul a las tarjetas de medicamentos.
- Se aplico estilo metalico azul a la tarjeta de perfil de la cabecera.
- Se implementaron alertas críticas con sonido doble, acceso a No molestar y reintentos configurables de 5, 10 o 15 minutos.
- La configuración de alertas críticas se movió a un panel global dentro del menú hamburguesa y dejó de depender del formulario de cada medicamento.
- Se agregó un panel de signos vitales para registrar presión arterial, latidos y glucemia.
- Se añadieron teclado numerico, decimal y de telefono en el formulario de medicamentos segun el tipo de campo.
- Se eliminó el pedido inmediato por medicamento y se reemplazó por una lista de pedidos tipo carrito con envio unico por WhatsApp.
- La lista de pedidos y las tarjetas de medicamentos ahora tienen scroll vertical interno para no bloquear el acceso a otros botones cuando el contenido crece.
- Se corrigió la apertura manual de WhatsApp para admitir tanto WhatsApp normal como WhatsApp Business.
- Se añadió una base de datos de pedidos enviados con historial por paciente y gasto estimado acumulado.
- Al añadir un medicamento al carrito ahora se pregunta cuantas unidades se desean pedir y esa cantidad se usa en el mensaje y en el historial guardado.
- Se amplió el panel de signos vitales para registrar temperatura y comentarios por cada bloque clinico.
- La base de datos se amplió para guardar glucemia y los backups ahora también conservan ese valor.
- La base de datos y los backups ahora tambien conservan comentarios y temperatura de signos vitales.
- Se aplicó estilo metálico rojo al panel de signos vitales y a sus botones de acción.
- Se añadió exportacion a Word `.docx` para signos vitales con filtro por hoy, rango personalizado y seleccion manual de registros guardados.
- Se añadió visualizacion previa de registros seleccionados antes de exportarlos.
- Se implementó una portada inicial animada con expansion tipo latido y salida inmediata al tocar.
- Se eliminó el texto inferior visible de la portada inicial para dejar solo la ilustracion y la entrada por toque.
- Se ajustó a amarillo el texto dentro de las tarjetas rojas del panel de signos vitales para mejorar legibilidad.
- Se compactó el listado seleccionable de signos vitales para mostrar solo checkbox y fecha/hora en cada registro.
- Se añadió titulo superior y credito inferior a la portada inicial.
- Se cambió la apertura de la portada a un revelado con forma de corazon y al menos 6 latidos.
- Se ajustó el tamaño final del corazon para evitar una expansion excesiva.
- Se corrigió la animacion para que, tras los latidos, se vea la ilustracion completa antes de tocar la pantalla.
- Se restauró el fondo azul metalico del escritorio principal tras una regresion visual.
- Se ajustó el titulo `Control de medicamentos` a negro y en dos lineas para que se vea con mas claridad en la portada.
- Se añadió una agenda de citas medicas con recordatorios por alarma dentro del panel de pacientes.
- La agenda de citas medicas se separó del panel de pacientes y ahora funciona como modulo propio reutilizando la configuracion global de alertas.
- Se extendió el fondo azul del escritorio tambien al panel y ficha de pacientes.
- Se reajustó el fondo del panel de signos vitales a un rojo metalico cepillado en diagonal, manteniendo ese modulo fuera del fondo azul general.
- Se dejo documentada la limitacion real del sistema Android para alertas criticas fuera del control total de la app.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras separar el modulo de citas medicas y reajustar el fondo de signos vitales.
- Se reparó `MainActivity` tras una corrupcion en la cabecera del archivo: se restauraron imports, la clase `MainActivity` y la portada inicial como overlay Compose valido.
- Se recompilo el modulo `app` correctamente con `:app:compileDebugKotlin`.
- Se instaló la version `debug` con `:app:installDebug` en el dispositivo `SM-S918U - 16`.
- Se reinstaló la version `debug` en el dispositivo `SM-S918U - 16` tras los ultimos ajustes visuales.
- Se reinstaló nuevamente la version `debug` en `SM-S918U - 16` tras compactar el listado de registros de signos vitales.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras los ajustes finales de la animacion de portada.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras recuperar el fondo del escritorio y corregir la estructura de `MainActivity`.
- Se ajustaron a blanco los textos del escritorio principal para mejorar legibilidad sobre el fondo azul.
- Se unificó el formulario de medicamentos con el estilo visual del escritorio, usando fondo azul metalico y textos blancos.
- Se habilitaron `Formato` y `Concentracion` con opciones de respaldo del vademecum incluso sin coincidencia exacta del medicamento.
- Se separó el listado de signos vitales en una pantalla propia con navegacion independiente y compatibilidad con el boton atras nativo.
- Se movio el historial de citas a una pantalla independiente para evitar una agenda excesivamente larga.
- Se añadió una accion `Aceptar` en la notificacion de citas para marcarlas como realizadas y conservarlas en el historial.
- Se ajustaron a blanco los textos de `Medicamentos guardados`.
- Se aplicó fondo negro tanto a la tarjeta como al exterior del panel de configuracion de alertas.
- Se aplicó un fondo naranja degradado al panel de `Copias de seguridad`, manteniendo intactas sus acciones de programacion, exportacion e importacion.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras el ajuste visual del panel de copias de seguridad.
- Se añadió un registro de facultativos en el menu hamburguesa, enlazado con citas medicas mediante `practitionerId` y con migracion Room a la version `14`.
- Los backups ahora conservan tambien el catalogo de facultativos junto con las citas relacionadas.
- Se añadió un modulo de vacunacion con catalogo base de vacunas habituales, historial por paciente y alarma opcional de proxima dosis o refuerzo.
- El catalogo de vacunacion se adapto al esquema base de Nicaragua por edades y ahora propone dosis o refuerzos segun la edad registrada del paciente.
- El escritorio de medicamentos ahora muestra la hora real de cada toma marcada y pide confirmacion antes de borrar un registro manual existente.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras ajustar el registro real de tomas y la confirmacion irreversible al desmarcar.
- El versionado de la app se movio a `gradle.properties` con `APP_VERSION_CODE=2` y `APP_VERSION_NAME=1.1.0` para preparar futuras actualizaciones en Play Store.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras integrar facultativos, versionado de Play Store y recompilar el proyecto correctamente.
- Las citas medicas ahora exigen seleccionar un facultativo del registro cuando ese catalogo ya tiene datos y el nombre visible de la cita se rellena desde `Facultativos`.
- El panel de facultativos tambien absorbe historial previo por coincidencia con `doctorName` para que las citas antiguas sigan apareciendo bajo ese profesional.
- Se reinstaló la version `debug` en `SM-S918U - 16` tras reforzar el enlace entre `Facultativos` y `Citas medicas`.

### 8 de mayo de 2026
- Formulario de medicamentos: añadido `Spacer` de 48dp arriba y abajo del contenido para evitar que el título y el botón "Volver al escritorio" queden pegados a los bordes.
- Lista de medicamentos guardados: añadido `Spacer` de 48dp al final para que el botón "Volver al escritorio" no quede detrás de la barra de navegación del sistema.
- Formulario de medicamentos envuelto en `MetallicMedicationCard` con fondo azul metálico, título "Formulario de medicamento" y checkbox `¿Alarma activa?` añadido entre `¿Activo?` y `¿Controlar existencias?`.
- `mostrarFormulario` añadido a la condición del gradiente de fondo del `Column` raíz para que el fondo verde metálico se aplique también al abrir el formulario.
- El `Column` scrollable de los paneles tiene `weight(1f)`, `statusBarsPadding()`, `navigationBarsPadding()` e `imePadding()` en el orden correcto para que el scroll funcione.
- El formulario tiene su propio `Column` con `weight(1f)` + `verticalScroll(formScrollState)` independiente del resto de paneles, con padding 24dp arriba/abajo y 16dp laterales.
- Cuando `mostrarFormulario` está activo, tanto el `Column` exterior (verde) como el `Column` scrollable de paneles tienen padding 0 para no interferir con el padding propio del formulario.
- Padding uniforme en todos los demás paneles: 24dp arriba y abajo, 16dp laterales.
- Instalacion debug verificada el 8 de mayo de 2026 en `SM-X115 - Android 16` con `BUILD SUCCESSFUL`.
- Selector de fecha del escritorio pasó a carrusel horizontal centrado con `LazyRow`, snap al centro, flechas día a día y desvanecido lateral; la cabecera fija y el resumen de fecha animado quedaron operativos.
- El escritorio quedó funcional y completado para el alcance definido en esta fase.

### 9 de mayo de 2026
- Sincronización automática del peso entre `SignosVitales` y `PatientProfile`:
  - Al guardar un registro de signos vitales que incluya el campo Peso, se actualiza automáticamente el campo `peso` y `pesoUnidad` del paciente activo en `patient_profile`.
  - El estado en memoria `pesoPaciente` y `pesoUnidadPaciente` se refleja de inmediato para que la ficha del paciente muestre el dato nuevo sin recargar.
  - El peso registrado en Signos vitales es el que manda y siempre sobreescribe el del perfil.
- Campo Peso en la ficha del paciente convertido a solo lectura y sincronizado:
  - Los campos `Peso` y `Unidad de peso` son siempre de solo lectura, incluso en modo edición del perfil.
  - Se eliminó el desplegable de unidad de peso editable de la ficha (ya no tiene sentido editar esos campos manualmente).
  - Al tocar cualquiera de los dos campos se muestra un `AlertDialog` con el mensaje "El dato de este campo se sincroniza desde Signos vitales." y un botón "Aceptar".
  - Se añadió el estado `mostrarDialogoPesoSincronizado` para controlar el diálogo.
- Se extrajo `CarritoPendienteItem` a su propio archivo en `data/local`; tabla `carrito_pendiente` con `patientId`, `medicationId` y `unidadesSolicitadas`.
- Se creó `HelpersExportModels.kt` con `VitalSignsExportRange`, `IntakeExportRange` y `MedicationIntakeExportRow` para centralizar los modelos de exportación.

### 10 de mayo de 2026
- `MedicationOrderDao` ampliado con `observarPorPaciente`, `obtenerTodosLista` y operaciones de borrado individual y por paciente para soportar el historial completo de pedidos enviados.
- Se creó `AlarmAlertActivity`: pantalla completa de alarma que aparece sobre la pantalla de bloqueo con `FLAG_KEEP_SCREEN_ON` y `FLAG_TURN_SCREEN_ON`; muestra nombre del medicamento o cita, acciones `Tomar ahora` / `Posponer` y `Silenciar`, y llama a `AlarmReceiver`/`NotificacionHelper` para registrar la toma o reprogramar según la acción elegida.

### 12 de mayo de 2026
- `Medication.kt` ampliado con `colorMedicamento2` para degradados de dos tonos en las tarjetas y `presentacion` para el texto libre de presentación farmacéutica.
- Se añadió el objeto `RestockSource` con las constantes `WHATSAPP_NUMBER`, `WHATSAPP_CONTACT` e `INSS` para identificar el canal de reposición de cada medicamento.

### 13 de mayo de 2026
- Se creó `NuevaVacunaActivity`: actividad independiente de Compose para el formulario de nueva vacuna, con selector de fecha nativo `DatePickerDialog`, lógica de proxima dosis calculada y guardado directo en `VaccinationRecord` via Room.

### 14 de mayo de 2026
- **Sistema de suscripción y seguridad implementado:**
  - `SecurityManager`: detección anti-debug (`Debug.isDebuggerConnected`), anti-tampering (hash SHA-256 del certificado APK), detección informativa de root y emulador. La comprobación de firma queda desactivada mientras `EXPECTED_CERT_SHA256 == "CONFIGURE_ME"` para no bloquear builds de debug.
  - `SubscriptionManager`: periodo de prueba de 30 días; fecha de instalación cifrada con `EncryptedSharedPreferences` y copia ofuscada con máscara XOR en prefs planas para resistir manipulaciones del reloj del sistema; comprueba consistencia entre ambas copias antes de calcular días restantes.

### 15 de mayo de 2026
- `SignosVitales` ampliado con `peso`, `pesoUnidad` e `imc` para registrar el índice de masa corporal junto con los signos clínicos habituales; el IMC se calcula automáticamente al guardar si hay peso y talla disponibles.
- **Módulo de actividad física y podómetro implementado:**
  - `PhysicalActivity`: entidad Room con tipo (`caminar`/`bicicleta`), fechas, pasos, distancia, calorías, duración, frecuencia cardíaca media, altitud máxima y fuente (GPS / podómetro / manual).
  - `ActivityTrackingService`: servicio en primer plano con notificación persistente; cuenta pasos por acelerómetro (`TYPE_STEP_COUNTER`), sigue la ruta con `FusedLocationProviderClient`, lee frecuencia cardíaca cuando el sensor está disponible, captura altitud máxima GPS y anuncia estadísticas cada minuto por síntesis de voz (TTS).
  - `PodometroScreen`: pantalla Compose con botones de inicio/pausa/fin, métricas en tiempo real y diálogo de resumen al terminar la sesión.
- `BillingManager`: integración con Google Play Billing; gestiona productos `mensual_premium` y `anual_premium`, verifica el estado de la suscripción activa y actualiza `SubscriptionManager` cuando se completa una compra.

### 16 de mayo de 2026
- `PhysicalActivityDao` y `CarritoPendienteDao` añadidos como archivos independientes con operaciones de inserción, consulta por paciente, listado completo, guardado en bloque y eliminación para cubrir backup y sync.
- `SubscriptionScreen`: paywall en Compose con tarjetas de plan mensual y anual, botón de compra que lanza el flujo de Google Play Billing, enlace a política de privacidad y botón de restauración de compras.
- `PatientProfile` y `AndroidSyncSnapshotMapper` actualizados para reflejar los nuevos campos de la app (peso sincronizado, IMC, campos de salud reproductiva).
- **Módulo de salud reproductiva – Ciclo menstrual:**
  - `CicloMenstrual`: entidad con `fechaInicioPeriodo`, `duracionPeriodoDias`, `duracionCicloDias`, `intensidadFlujo`, `sintomas`, `dolorIntensidad`, `estadoAnimo` y `retraso`.
  - `CicloMenstrualDao`: CRUD completo con Flow por paciente.
  - `SintomaInterPeriodo`: entidad para síntomas entre períodos con tipo, fecha, intensidad y nota.
  - `SintomaInterPeriodoDao`: CRUD.
  - `MenstruacionScreen`: pantalla Compose de seguimiento del ciclo con calendario visual, registro de síntomas inter-período, predicción de próximo período y vista de historial.
- **Módulo de anticonceptivos:**
  - `MetodoAnticonceptivo`: entidad con tipo (píldora combinada, minipíldora, parche transdérmico, anillo vaginal, inyección mensual/trimestral, DIU hormonal/cobre, implante, preservativo, anticoncepción de emergencia), fecha de inicio, fecha de fin, ciclo activo, hora de toma diaria e intervalo para métodos periódicos.
  - `TipoMetodoAnticonceptivo` (objeto compañero): constantes de tipo, helpers `esPildora`, `esMetodoDiario`, `esMetodoPeriodico` e `intervaloDefault`.
  - `RegistroTomaAnticonceptivo`: registro de cada toma o aplicación del método activo.
  - `MetodoAnticonceptivoDao`, `RegistroTomaAnticonceptivoDao`: CRUD con Flow.
  - `AnticonceptivoScheduler`: programa alarmas diarias para píldoras y alertas periódicas para parches, anillos e inyecciones.
  - `AnticonceptivosScreen`: pantalla Compose con alta del método activo, historial de tomas, marcado de toma del día y acceso al historial de ciclos.
- `NotificacionHelper`, `AlarmReceiver` y `BootReceiver` actualizados para gestionar las notificaciones y alarmas de anticonceptivos junto con las de medicamentos, citas y vacunas.
- **Asistente IA – chat por mensajes:**
  - `MensajeChat`: entidad Room con `patientId`, `sesionId` (UUID), `rol` (`usuario`/`asistente`), `contenido` y `timestamp`; tabla `mensajes_chat`.
  - `MensajeChatDao`: CRUD con Flow por paciente y por sesión, borrado por sesión y borrado total.
- `AppDatabase` actualizado a **versión 27** con las nuevas entidades: `PhysicalActivity`, `CarritoPendienteItem`, `CicloMenstrual`, `MetodoAnticonceptivo`, `RegistroTomaAnticonceptivo`, `SintomaInterPeriodo` y `MensajeChat`; migraciones incrementales desde la versión 20 hasta la 27.
- `BackupManager` ampliado para exportar e importar las nuevas tablas: ciclos menstruales, síntomas inter-período, métodos anticonceptivos, tomas de anticonceptivos y mensajes de chat.

### 17 de mayo de 2026 — Primera parte (mañana)
- **Módulo de asistente MediAI con Gemini:**
  - `MediAIMotor` (`ai/`): motor Kotlin que envuelve `GenerativeModel` de `com.google.ai.client.generativeai` con el modelo `gemini-2.0-flash`; la API key se guarda en `SharedPreferences` y se configura desde la propia app; expone `StateFlow<EstadoMotor>` (`Inactivo`, `Cargando`, `Listo`, `Error`) y funciones `inicializar()`, `enviarMensaje()` y `reiniciar()`.
  - `AsistenteContextoProvider` (`ai/`): construye el bloque de contexto del paciente que se inyecta en el system prompt; incluye perfil, medicamentos activos, citas próximas, vacunas, signos vitales recientes, ciclos menstruales, método anticonceptivo activo y actividad física reciente.
  - `AsistenteScreen`: pantalla chat tipo burbuja con `LazyColumn` de mensajes, campo de texto con acción de envío, indicador de carga animado, botón de borrar historial de sesión y diálogo para configurar la API key; historial de la conversación se persiste en Room (`mensajes_chat`) por sesión UUID para poder retomarlo.
- `MainActivity` actualizado para integrar los nuevos módulos (menstruación, anticonceptivos, asistente MediAI) en el menú hamburguesa y la navegación de pantallas.
- `@OptIn(ExperimentalMaterial3Api::class)` añadido a `FormularioMetodoDialog` en `AnticonceptivosScreen.kt` para resolver los errores de API experimental de Material3.
- `PhysicalActivityDao` ampliado con `obtenerPorPacienteLista` (versión suspend) para uso en el contexto del asistente.
- Instalación debug verificada el 17 de mayo de 2026 en `SM-X115 - Android 16` con `BUILD SUCCESSFUL` tras recuperación del corte de luz.

### 17 de mayo de 2026 — Segunda parte (tarde)
- **Módulo de control de embarazo — Fase 1 (modelo de datos + pantalla):**
  - `ControlEmbarazo.kt`: nueva entidad Room con tabla `control_embarazo`; campos: `id`, `patientId`, `fechaUltimaRegla`, `fechaProbableParto`, `activo` (1 = en curso), `checklistRealizado`, `semanasParto`, `fechaParto`, `tipoTerminacion`, `pesoRnGramos`, `tallaRnCm`, `nombreBebe`, `sexoBebe`, `notasParto`, `notas`.
  - `ControlEmbarazoDao.kt`: `observarPorPaciente` y `observarActivo` (Flow), `guardar`, `actualizar`, `eliminar`.
  - `VisitaPrenatal.kt`: nueva entidad Room con tabla `visita_prenatal`; campos: `id`, `embarazoId`, `patientId`, `fecha`, `semanasGestacion`, `peso`, `presionArterial`, `alturaUterina`, `fcf`, `movimientosFetales`, `edema`, `analisis`, `ecografia`, `medicacion`, `notas`.
  - `VisitaPrenatalDao.kt`: `observarPorEmbarazo` (Flow), `guardar`, `actualizar`, `eliminar`.
  - `AppDatabase.kt` actualizado a **versión 25** con `ControlEmbarazo::class` y `VisitaPrenatal::class`; `MIGRATION_24_25` crea las dos nuevas tablas; DAOs abstractos `controlEmbarazoDao()` y `visitaPrenatalDao()`.
  - `EmbarazoScreen.kt` (~500 líneas): pantalla Compose con degradado morado (`Color(0xFF1A0030)` → `Color(0xFF6A1B9A)`); tarjeta resumen con semanas de gestación y fecha probable de parto; checklist trimestral colapsable por trimestre según protocolo del Ministerio de Salud (T1/T2/T3, 8 ítems cada uno); listado de visitas prenatales con ficha completa; diálogo `IniciarEmbarazoDialog` (FUR + notas); diálogo `FormularioVisitaPrenatalDialog` (peso, PA, altura uterina, FCF, movimientos fetales, edema, análisis, ecografía, medicación); diálogo `RegistrarPartoDialog` (5 tipos de parto, datos del recién nacido: peso, talla, nombre y sexo); historial de embarazos previos al final.
  - `MenstruacionScreen.kt` actualizado con banner de embarazo activo (muestra semanas de gestación) y parámetro `onVerEmbarazo` para navegar a `EmbarazoScreen`.
  - `MainActivity.kt` actualizado: flag `mostrarPanelEmbarazo`, incluido en `cerrarPanelesSecundarios()`, en la condición `mostrarEscritorio`, y bloque de visualización `if (mostrarPanelEmbarazo && pacienteActivo != null && esPacienteMujer)` que llama a `EmbarazoScreen`.
- **Rediseño completo de `MenstruacionScreen.kt`** para coincidir con capturas de referencia de app de seguimiento de ciclo:
  - Degradado de fondo rosa oscuro (`Color(0xFF880E4F)` → `Color(0xFFAD1457)`).
  - Top bar con flecha de volver e icono de ajustes (⚙).
  - Tarjeta principal blanca: estado vacío con 🌸 y texto "Empieza a seguir tu ciclo" + botón de registro; cuando hay datos muestra próximo período (días restantes + fecha), duración de ciclo/período y fase del día resaltada con punto de color.
  - Calendario semanal horizontal (−3 a +4 días desde hoy): punto de fase del ciclo bajo cada día, día de hoy en círculo rosa, leyenda de fases (Periodo / Folicular / Ovulación / Lútea).
  - Tarjeta de registro rápido de síntoma con 5 botones emoji (🩸 Sangrado, 💧 Flujo, ⚡ Dolor, 😰 Molestia, 😊 Estado de ánimo).
  - Banner morado cuando hay embarazo activo con semanas y acceso a control de embarazo.
  - Botón primario "El período empezó hoy" (rosa), botón secundario "Registrar con fecha y detalles" (outlined blanco) y botón "Registrar un embarazo" (solo si no hay embarazo activo).
  - Historial de períodos colapsable con edición y eliminación por ítem.
  - Historial de síntomas colapsable (hasta 20 registros visibles).
  - Diálogo `AjustesCicloDialog`: campos duración del ciclo y duración del sangrado con botones Guardar / Cancelar.
  - Diálogo `RegistrarSintomaDialog`: selector de fecha nativo, chips de tipo en 2 columnas, selector de intensidad segmentado (Leve / Moderada / Intensa), cuadrícula de estado de ánimo con emojis.
  - Diálogo `FormularioCicloDialog`: registro y edición completa de un período (fecha, duración, intensidad de flujo, síntomas con checkboxes, dolor 0–10, estado de ánimo, retraso/adelanto y notas).
- **Menú hamburguesa dinámico según estado de embarazo:**
  - Añadida variable `embarazoActivoMenu` que observa `database.controlEmbarazoDao().observarActivo(patientId)` con `remember(pacienteActivo?.id)` y `collectAsState`.
  - Sin embarazo activo: el bloque `esPacienteMujer` muestra **Ciclo menstrual** 🌸 y **Anticonceptivos** 💊.
  - Con embarazo activo: ese mismo bloque muestra únicamente **Control de embarazo** 🤰; los dos ítems anteriores desaparecen del menú.
- Instalación debug verificada el 17 de mayo de 2026 (tarde) en `SM-X115 - Android 16` con `BUILD SUCCESSFUL`.

### 18 de mayo de 2026 — Rediseño completo EmbarazoScreen + celebraciones + parto múltiple
- **Rediseño visual completo de `EmbarazoScreen.kt`** (~1400 líneas):
  - Paleta de colores pastel propia: `EmbCardFondo`, `EmbPurple`, `EmbPink`, `EmbLavender`, `EmbTextoOscuro`, `EmbTextoMedio`, `EmbTextoClaro`.
  - Fondo blanco suave con tarjetas blancas elevadas; se abandona el degradado oscuro anterior.
  - `TarjetaBebe`: arco de progreso animado con canvas, emoji de tamaño del bebé según semanas (comparación con frutas/alimentos), días restantes, FPP formateada, notas rápidas editables.
  - `TrimestralChecklist`: checkboxes circulares con tick por trimestre (T1/T2/T3), colapsables, conteo de realizados / total.
  - `VisitaPrenatalCard`: tarjeta de cada visita con todos los campos médicos, miniatura de ecografías y acceso a vídeo.
  - `SectionHeader`: cabecera de sección con color primario configurable.
  - Botón de papelera para eliminar embarazos terminados del historial, con diálogo de confirmación.
- **`PregnancyCelebrationOverlay`** (en `MainActivity.kt`):
  - Se muestra automáticamente al iniciar un nuevo seguimiento de embarazo.
  - Fondo degradado rosa oscuro/púrpura, fuegos artificiales rosas animados, texto `¡Enhorabuena! 🤰` + nombre del paciente.
  - Auto-cierre tras 10 segundos o toque.
  - Encadenado mediante `onEmbarazoIniciado: (pacienteName: String) -> Unit` en la cadena `EmbarazoScreen → MedicamentoForm → MainActivity`.
- **`BirthCelebrationOverlay`** (en `MainActivity.kt`):
  - Se muestra automáticamente al registrar un parto.
  - Paleta dinámica basada en el sexo del bebé: rosa para niña, azul para niño, lavanda si no se indica.
  - Texto `¡Bienvenida/o al mundo, nombre!` + `Mamá: pacienteName`, fuegos artificiales del color elegido.
  - Auto-cierre tras 12 segundos o toque.
  - Encadenado mediante `onPartoRegistrado: (sexoBebe, nombreBebe, pacienteName) -> Unit`.
- **`FormularioVisitaPrenatalDialog`** — selector de imágenes y vídeo para ecografías:
  - `LazyRow` de miniaturas con botón ✕ por imagen; se abre galería con `rememberLauncherForActivityResult(GetMultipleContents)`.
  - Campo de vídeo con URI seleccionada y botón para quitar.
  - Carga de imágenes con `BitmapFactory.decodeStream` + `asImageBitmap` (sin Coil).
  - Nuevos campos en `VisitaPrenatal`: `imagenesEcografia: String` (URIs separadas por `|`) y `videoEcografia: String`.
  - `AppDatabase` actualizado a **versión 27** con `MIGRATION_25_26` (campos `notasParto` y `notas` en `control_embarazo`) y `MIGRATION_26_27` (campos `imagenesEcografia` y `videoEcografia` en `visita_prenatal`).
- **Soporte de parto múltiple (gemelos, trillizos…):**
  - `ControlEmbarazo` ampliado con `partoMultiple: Int` (0 = sencillo, 1 = múltiple) y `neonatosJson: String` (JSON de la lista de neonatos).
  - `AppDatabase` actualizado a **versión 28** con `MIGRATION_27_28` (añade `partoMultiple` e `neonatosJson` a `control_embarazo`).
  - `DatosNeonato(nombre, sexo, pesoG, tallaCm)`: data class que representa un recién nacido.
  - `serializarNeonatos` / `deserializarNeonatos`: serialización JSON manual sin dependencias externas.
  - `gramosALbOz(g: Int): String`: convierte gramos a libras + onzas (ej. `"3 lb 4.5 oz"`).
  - `cmAPulgadas(cm: Int): String`: convierte centímetros a pulgadas (ej. `"19.7 in"`).
- **`RegistrarPartoDialog` rediseñado:**
  - Toggle **Sencillo / Múltiple** visible desde el principio del formulario.
  - Modo sencillo: tarjeta `NeonatoCard` única (sexo con tarjetas visuales, nombre, peso en g con equivalencia lb/oz en el suffix, talla en cm con equivalencia pulgadas en el suffix, textos en negro `Color(0xFF212121)`).
  - Modo múltiple: `LazyColumn` de `NeonatoCard`s dinámicas (mínimo 2), botón `+ Añadir bebé`, botón ✕ para eliminar (cuando hay más de 2).
  - `NeonatoCard`: componable privado reutilizable con sexo, nombre, peso+unidades duales y talla+unidades duales.
  - Al guardar en modo múltiple: `partoMultiple = 1`, `neonatosJson = serializarNeonatos(neonatos)`, campos heredados del primer neonato para compatibilidad con historial simple.
- **Historial de embarazos terminados — display mejorado:**
  - Si `partoMultiple == 1`: muestra una línea por neonato con emoji de sexo, nombre, peso en g + lb/oz, talla en cm + pulgadas.
  - Si `partoMultiple == 0`: muestra fila única pero ahora también incluye conversión a sistema imperial para peso y talla.
- **`TarjetaRecienNacidos`** — nueva composable post-parto:
  - Aparece en la pantalla de embarazo cuando `embarazoActivo == null` y existe un parto con `fechaParto > 0` en el historial.
  - Cabecera con degradado del color del primer bebé + fecha de nacimiento.
  - Una `Card` por neonato con: emoji de sexo, nombre, peso (g + lb/oz), talla (cm + pulgadas).
  - Placeholder "⏳ Próximamente: seguimiento del desarrollo neonatal".
  - La `TarjetaBienvenidaEmbarazo` sigue apareciendo debajo para poder iniciar un nuevo seguimiento.
- **Correcciones de colores de texto:**
  - Todos los `OutlinedTextField` del `RegistrarPartoDialog` y `NeonatoCard` usan `unfocusedTextColor = Color(0xFF212121)` y `focusedTextColor = Color(0xFF212121)` para evitar texto gris claro sobre fondo blanco.
  - Etiquetas de `RadioButton` con `color = Color(0xFF212121)`.
- **Compilación verificada:** `BUILD SUCCESSFUL` con `.\gradlew compileDebugKotlin`.

---

### 20 de mayo de 2026 — Rediseño y correcciones de `CicloMenstrualScreen`

#### Contexto
`CicloMenstrualScreen` es el composable dentro de `MainActivity.kt` (≈ línea 7391) que gestiona el seguimiento del ciclo menstrual. Es independiente de `MenstruacionScreen.kt` (que es la pantalla antigua basada en `SintomaInterPeriodo`). Este composable usa directamente la entidad `CicloMenstrual` (`data/local/CicloMenstrual.kt`) con los campos: `id`, `patientId`, `fechaInicio`, `duracionDias`, `duracionCicloDias`, `sintomas`, `notas`.

#### Cambios realizados

**1. Pantalla de bienvenida antes del primer registro**
- Cuando `ultimoCiclo == null` se muestra una tarjeta blanca con: emoji 🌸, título "Empieza a seguir tu ciclo", descripción y botón "Registrar primer periodo".
- El botón "Registrar primer periodo" abre `DatePickerDialog` y al confirmar llama directamente a `guardarPeriodo(fechaSeleccionada)`.
- Antes el botón solo abría el selector sin guardar — esto se corrigió añadiendo el callback `onFechaSeleccionada` a `abrirSelectorFecha()`.
- Los síntomas ya **no crean un ciclo nuevo** si `ultimoCiclo == null`; muestran el toast "Registra primero el inicio de tu periodo" y cancelan sin persistir.

**2. Días de menstruación coloreados en el calendario**
- El `LazyRow` del "Calendario del ciclo" calcula `esPeriodo` por cada día: comprueba si `fechaInicio` está dentro del rango `[inicioDelDia(ultimoCiclo.fechaInicio) .. inicio + (duracionDias-1) * 86400000]`.
- Días de periodo: fondo `#FFC1DC` (rosa claro), borde `#E91E63`.
- Día seleccionado: fondo `#E91E63` (rosa), texto blanco.
- Resto: fondo transparente, borde `#E7BED3`.

**3. Iconos corregidos**
- Tarjeta de "Menstruacion": el texto `"i"` (signo de admiración corrupto) se sustituyó por el emoji 🩸 (`fontSize = 40.sp`).
- Historial de ciclos: el texto `"??"` (emoji corrupto) se sustituyó por 🩸 (`fontSize = 32.sp`).
- Separador roto `?` en el subtexto del historial corregido a `·`.

**4. Posición de la rueda dentada en el header**
- El `Row` del header pasó de `padding(horizontal = 16.dp)` a `padding(start = 16.dp, end = 24.dp, top = 18.dp, bottom = 18.dp)` para que la ⚙ quede 24dp separada del menú hamburguesa global.
- El título "Ciclo menstrual" ya no tiene `weight(1f)`; la ⚙ queda inmediatamente después del título con `spacedBy(8.dp)`.
- Se añadió `Spacer(Modifier.weight(1f))` tras la ⚙ para empujar ambos elementos a la izquierda.

**5. Síntomas se acumulan en el ciclo activo (no crean registros nuevos)**
- `RegistrarSintomaDialog.onGuardar`: en vez de llamar a `guardar()` con un `CicloMenstrual` nuevo, ahora llama a `actualizar()` con `ultimoCiclo.copy(sintomas = sintomasActualizados, notas = notasActualizadas)`.
- Los síntomas se concatenan con `, `; las notas con `\n`.
- El historial muestra los síntomas acumulados del ciclo con 🩺 y las notas con 📝 directamente en cada tarjeta.

**6. Botón "El sangrado terminó hoy"**
- Función `finalizarSangrado()` añadida justo antes de `abrirDialogoSintoma()`.
  - Calcula días reales: `(inicioDelDia(now) - inicioDelDia(cicloActual.fechaInicio)) / 86400000 + 1`.
  - Llama a `actualizar(cicloActual.copy(duracionDias = diasReales.coerceAtLeast(1)))`.
  - Muestra toast "Sangrado finalizado: X días".
- Botón granate (`#880E4F`, `RoundedCornerShape(28.dp)`) con texto "⏹ El sangrado terminó hoy" visible **solo cuando `ultimoCiclo != null`**, colocado encima del botón rosa "El periodo empezó hoy".

#### Flujo completo resultante
1. Sin ciclos registrados → solo tarjeta 🌸 + calendario + síntomas + botón rosa "El periodo empezó hoy".
2. Al pulsar "Registrar primer periodo" o "El periodo empezó hoy" → se crea el primer `CicloMenstrual`.
3. Con ciclo activo → aparece el botón granate "El sangrado terminó hoy" (ajusta `duracionDias` real) + el botón rosa (inicia nuevo ciclo).
4. Al registrar síntomas → se actualizan `sintomas` y `notas` del ciclo activo; nunca se crea un registro nuevo.
5. El historial muestra síntomas y notas acumulados bajo cada entrada de ciclo.

#### Archivos modificados
- `app/src/main/java/com/carlos/controlmedicamentos/MainActivity.kt` — único archivo afectado (≈ líneas 7391–7870).

#### Compilación verificada
- `BUILD SUCCESSFUL` con `.\gradlew.bat :app:assembleDebug` (solo warnings de deprecación preexistentes, sin errores nuevos).

### 27 de mayo de 2026 — Sesión tarde

#### Pérdida / interrupción del embarazo
- Botón discreto en `ControlEmbarazoScreen`: "Registrar pérdida o interrupción del embarazo" (texto gris 13sp, centrado).
- Diálogo con `ExposedDropdownMenuBox` con 9 opciones:
  - Aborto espontáneo, Embarazo ectópico, Pérdida por enfermedad materna, Interrupción por malformación fetal, Interrupción con metotrexato, Interrupción farmacológica (misoprostol/mifepristona), Interrupción quirúrgica (legrado/aspiración), Muerte fetal intrauterina, Otro.
- Campos adicionales: tratamiento/procedimiento (opcional), notas clínicas (opcional).
- Nota de privacidad visible en el diálogo.
- Al guardar: `activo = false`, `estadoEmbarazo = "INTERRUMPIDO"`, `fechaFin`, `tipoInterrupcion`, `metodoInterrupcion`, `notasInterrupcion`.
- El `ReporteClinicoExporter` ya reconoce `EMBARAZO_INTERRUMPIDO` y exporta la info de interrupción.

#### Historial de embarazos
- Cuando no hay embarazo activo, `ControlEmbarazoScreen` muestra debajo del botón de iniciar embarazo un listado "Historial de embarazos" con tarjetas que muestran:
  - FUR, fecha de fin o de parto.
  - Tipo de interrupción con icono ⚠️ (si fue pérdida/aborto).
  - Tipo de parto con icono 👶 (si terminó en parto).
  - Tratamiento y notas si aplica.
  - Duración en semanas.
- Se observa `controlEmbarazoDao().observarTodos(patientId)` y se filtran los no activos.

#### Registro de parto (estadoEmbarazo)
- `ControlEmbarazoDao.registrarParto()` ahora también establece `estadoEmbarazo = 'FINALIZADO'`.

#### Pantalla de Anticonceptivos restaurada
- Archivo: `AnticonceptivosScreen.kt` (composable separado).
- Funcionalidades:
  - Tarjeta de método activo (tipo, fecha inicio, días de uso, hora de toma, frecuencia, próxima cita, notas).
  - Botón "Marcar toma de hoy" que registra en `anticonceptivo_intakes`.
  - Historial de tomas (últimas 15).
  - Pantalla vacía con invitación a registrar método.
  - Historial de métodos anteriores con opción de eliminar.
  - Diálogo "Nuevo método anticonceptivo" con dropdown de tipos, fecha de inicio, hora de toma (solo diarios), notas.
  - Desactivar método (pasa al historial).
- Integrado en `MainActivity.kt`: variable `mostrarPanelAnticonceptivos`, `cerrarPanelesSecundarios()`, `mostrarEscritorio`, `panelUsaScrollInterno`, `BackHandler`, bloque de renderizado.

#### Menú hamburguesa (paciente mujer)
- **Sin embarazo activo**: 🌸 Ciclo menstrual + 💊 Anticonceptivos.
- **Con embarazo activo**: 🤰 Control de embarazo + 💊 Anticonceptivos.
- 💊 Anticonceptivos siempre visible para mujeres (no se oculta durante embarazo).
- 🤰 Control de embarazo solo aparece cuando `embarazoActivo != null`.

#### Base de datos — versión 31
- Entidades añadidas: `AnticonceptivoIntake`, `BebeRecienNacido`.
- DAOs añadidos: `AnticonceptivoIntakeDao`, `BebeRecienNacidoDao`.
- Migración 29→30: tabla `anticonceptivo_intakes`.
- Migración 30→31: tabla `bebes_recien_nacidos` (id, embarazoId, patientId, nombre, sexo, fechaNacimiento, pesoAlNacer, tallaAlNacer, notas, fechaRegistro).

#### Registro de parto con datos neonatales (EN PROGRESO)
- `RegistrarPartoDialog` pendiente de reescribir con soporte para:
  - Parto sencillo / múltiple.
  - Selector de número de bebés (2-6 para múltiple).
  - Datos por bebé: nombre, sexo (Niño/Niña/No definido), peso al nacer, talla al nacer, observaciones.
  - Pestañas (`ScrollableTabRow`) para navegar entre bebés en parto múltiple.
  - Al registrar: inserta cada bebé en `bebes_recien_nacidos` y llama `registrarParto()`.
- Referencia: proyecto roto `E:\Controlmedicamentos completo ROTO` líneas 13166-13470 del MainActivity.kt.

### 2 de junio de 2026 — Requisitos Play Console (terminología médica → personal)

#### Motivación
Play Console rechazó la app por parecer una aplicación médica/clínica profesional (requiere cuenta de organización). Se cambiaron términos médicos por términos de uso personal/productividad.

#### Cambios realizados
1. **Nombre de la app**: `Control medicamentos v2.5.2026` → `Recordatorio de Medicación` (`strings.xml`)
2. **Menú hamburguesa** (`MainActivity.kt`):
   - "Especialistas" + icono LocalHospital → "Contactos" + emoji 👤
   - "Calendario de inmunizaciones" → "Registro de salud infantil" + 📅
   - "Citas" + LocalHospital → "Citas" + 📅
   - "Seguimiento diario" + LocalHospital → 📊
   - "Configuracion de alertas" + LocalHospital → ⚙️
   - "Actividad física" + LocalHospital → 🏃
   - "📋 Exportar reporte clínico" → "📋 Exportar resumen de registros"
   - "📝 Diario de salud" → "📝 Diario personal"
   - "👶 Control pediátrico" → "👶 Seguimiento infantil"
3. **Embarazo** (`MainActivity.kt`):
   - `contactosOMS()` → `contactosPrenatales()`
   - "8 contactos prenatales recomendados (OMS)" → "8 etapas de seguimiento recomendadas"
   - "Visitas prenatales" → "Visitas de seguimiento"
   - "Añadir visita prenatal" → "Añadir visita de seguimiento"
   - Textos internos de las 8 etapas: eliminados términos clínicos como "Analítica inicial: Hb, grupo/Rh", "Cribado de anemia", "proteínas en orina"
4. **AI Chat** (`AIChatScreen.kt`): reescritos todos los textos de respuesta del asistente:
   - Eliminadas palabras: "pacientes" → "usuarios/perfiles", "citas médicas" → "citas", "mediciones de presión arterial" → "tus valores de presión", "Signos Vitales" → "Registros de Salud", "Pediátrico" → "Infantil", etc.
5. **Exportador** (`ReporteClinicoExporter.kt`):
   - Título: "REPORTE CLÍNICO MÉDICO – PACIENTE..." → "RESUMEN DE REGISTROS PERSONALES"
   - "DATOS DEL PACIENTE" / "Paciente:" → "DATOS DEL USUARIO" / "Usuario:"
   - "VISITAS PRENATALES" → "VISITAS DE SEGUIMIENTO"
   - "Contacto OMS" → "Etapa"
   - Disclaimer médico eliminado → "Información de uso personal"
6. **Dashboard niño** (`DashboardNinoScreen.kt`): "Enfermedades" → "Afecciones"
7. **ReporteClinicoScreen.kt**: "Datos del paciente" → "Datos del usuario", "Signos vitales" → "Registros de salud", "Alertas clínicas" → "Alertas de salud", nombre de archivo `reporte_clinico` → `resumen_registros`

#### Pendiente (no crítico para Play Console)
- Refactor de `CitasMedicasActivity` → `CitasActivity` (cambio de nombre de clase)

#### Archivos modificados
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/carlos/controlmedicamentos/MainActivity.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/ui/screens/AIChatScreen.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/ReporteClinicoExporter.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/ReporteClinicoScreen.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/ui/DashboardNinoScreen.kt`

#### Compilación verificada
- `BUILD SUCCESSFUL` con `.\gradlew.bat :app:assembleDebug`

### 5 de junio de 2026 — Perfil: edición de fecha de nacimiento + fondo azul metálico

#### 1. Desbloqueo de fecha de nacimiento en edición de perfil
**Motivación:** al editar un perfil existente (incluidos perfiles restaurados desde backup), el campo "Fecha de nacimiento" aparecía bloqueado sin opción de modificación.

**Cambios en `MainActivity.kt`:**
- **Eliminada** la variable de estado `fechaNacimientoBloqueada` que comprobaba si existía una fecha guardada en `SharedPreferences` (`loadPersistedBirthday`). Esta variable impedía la edición en cualquier perfil que ya tuviera fecha registrada.
- **Simplificado** `mostrarDatePickerNacimiento`: ahora solo verifica `editandoFichaPaciente` para abrir el `DatePickerDialog`. Se eliminó la rama que mostraba el `Toast` "La fecha de nacimiento ya quedo fijada en este perfil".
- **Actualizado** el `trailingIcon` del campo `OutlinedTextField` de fecha de nacimiento: siempre muestra `Icons.Default.Edit` con contentDescription "Editar fecha de nacimiento", eliminando la lógica condicional que mostraba `Icons.Default.Save` cuando la fecha estaba "bloqueada".

**Resultado:** cualquier perfil, ya sea nuevo o existente, permite tocar el campo "Fecha de nacimiento" (o su icono de calendario/lápiz) y cambiar la fecha. La nueva fecha se guarda correctamente en Room y en `SharedPreferences` vía `savePersistedBirthday`.

#### 2. Fondo verde metálico cambiado a azul metálico
**Motivación:** el fondo general de la app mostraba un gradiente verde metálico en lugar del azul metálico deseado.

**Cambios en `MainActivity.kt`:**
- **Gradiente base** (`baseGradient` dentro del `.drawWithCache` del `Column` raíz):
  - Antes: `Color(0xFF04130B)`, `Color(0xFF0A4B2C)`, `Color(0xFF17A35E)`, `Color(0xFF0C512F)`, `Color(0xFF030D08)` (tonos verdes)
  - Después: `Color(0xFF030D1F)`, `Color(0xFF0A2A4B)`, `Color(0xFF1768A3)`, `Color(0xFF0C3451)`, `Color(0xFF030D1F)` (tonos azules metálicos)
- **Líneas de cuadrícula** (`softLineColor`):
  - Antes: `Color(0xFFA7FFD0).copy(alpha = 0.08f)` (verde claro)
  - Después: `Color(0xFFA7D0FF).copy(alpha = 0.08f)` (azul claro metálico)

**Resultado:** el fondo de la aplicación ahora presenta un acabado azul metálico coherente con el resto de la paleta visual (tarjetas, cabecera, escritorio).

#### Archivos modificados
- `app/src/main/java/com/carlos/controlmedicamentos/MainActivity.kt`

---

### 5 de junio de 2026 (sesión tarde) — Tabla completa de renombrados (terminología médica → personal)

#### Motivación
Play Console rechazó la app por parecer una aplicación médica/clínica profesional. Se realizó una revisión exhaustiva de todas las cadenas visibles para el usuario en toda la app.

#### Base de datos y arquitectura

| Elemento | Antes | Después |
|---|---|---|
| Nombre de BD | `control_medicamentos_db` | `control_insumos_db` |
| Tabla Room | `medicamentos` | `insumos` (migración 38→39) |
| Columna | `formaMedicamento` | `formaInsumo` |
| Columna | `colorMedicamento` | `colorInsumo` |
| Columna | `colorMedicamento2` | `colorInsumo2` |
| Tema Android | `Theme.ControlMedicamentos` | `Theme.ControlInsumos` |

#### MainActivity.kt — Menú hamburguesa (directorio / agenda)

| Antes | Después |
|---|---|
| "Médicos" | "Directorio" |
| "medicos habituales" | "contactos habituales" |
| "Selecciona un medico primero" | "Selecciona un contacto primero" |
| "Medico eliminado" | "Contacto eliminado" |
| "no hay medicos guardados" | "no hay contactos guardados" |
| "Especialidad:" | "Área:" |
| "Especialidad" (label campo) | "Área" |
| "Próxima cita:" | "Agendado para:" |
| "Informes sincronizados:" | "Documentos asociados:" |
| "Ver informes" | "Ver documentos" |
| "Nuevo medico" | "Nuevo contacto" |
| "Editar medico" | "Editar contacto" |
| "Próxima cita" (sección) | "Próxima reunión" |
| "cita futura sincronizada para este medico" | "reunión futura sincronizada para este contacto" |
| "Vista de informes" | "Vista de documentos" |
| "informes medicos" | "panel de documentos" |
| "Hola, necesito los siguientes medicamentos:" | "Hola, necesito los siguientes insumos:" |

#### MainActivity.kt — Iconos del menú hamburguesa

| Ítem | Antes | Después |
|---|---|---|
| Nuevo Registro | `Icons.Default.Medication` | `Icons.Filled.Add` |
| Contactos | `Icons.Default.LocalHospital` | `Icons.Default.AccountCircle` |
| Registro Preventivo | `Icons.Default.Edit` | `Icons.Filled.CalendarToday` |
| Agenda | `Icons.Default.LocalHospital` | `Icons.Filled.CalendarToday` |
| Inventario | `Icons.Default.Medication` | `Icons.Filled.List` |
| Métricas Diarias | `Icons.Default.LocalHospital` | `Icons.Filled.BarChart` |
| Recordatorios | `Icons.Default.LocalHospital` | `Icons.Filled.Notifications` |
| Asistente IA | `Icons.Default.AccountCircle` | `Icons.Filled.AutoAwesome` |
| Actividad Física | `Icons.Default.LocalHospital` | `Icons.Filled.DirectionsRun` |
| Historial de Compras | `Icons.Default.Medication` | `Icons.Filled.ShoppingCart` |
| Exportar Resumen | emoji | `Icons.Filled.Download` |
| Diario Personal | emoji | `Icons.Filled.Note` |
| Ciclo menstrual | emoji | `Icons.Filled.DateRange` |

#### MainActivity.kt — Módulo de seguimiento reproductivo (antes "Embarazo")

| Antes | Después |
|---|---|
| "Ciclo menstrual" (tab nav) | "Ciclo" |
| "Anticonceptivos" (tab nav) | "Métodos de planificación" |
| "Embarazo" (tab nav) | "Seguimiento reproductivo" |
| "Seguimiento prenatal" | "Seguimiento de etapa" |
| "Iniciar seguimiento de embarazo" | "Iniciar seguimiento de etapa" |
| "seguimiento de embarazo y ver...visitas prenatales." | "seguimiento de etapa y ver...visitas." |
| "🤰  Iniciar seguimiento de embarazo" | "🤰  Iniciar seguimiento de etapa" |
| "Historial de embarazos" | "Historial de seguimientos" |
| "🤰  Embarazo en curso" | "🤰  Seguimiento reproductivo" |
| "Fecha probable de parto" | "Fecha probable de finalización" |
| "Faltan X días para el parto" | "Faltan X días para la finalización" |
| "Registrar parto / finalizar embarazo" | "Registrar finalización del seguimiento" |
| "Eliminar seguimiento de embarazo" | "Eliminar seguimiento de etapa" |
| "Registrar pérdida o interrupción del embarazo" | "Registrar pérdida o interrupción del proceso" |
| "seguimiento de embarazo y todas las visitas prenatales" | "seguimiento de etapa y todas las visitas de seguimiento" |
| "Parto:" | "Finalización:" |
| "Tipo de parto" | "Tipo de finalización" |
| "Notas del parto" | "Notas de la finalización" |
| "historial de embarazos?..." | "historial de seguimientos?..." |
| "¡Que sea un embarazo maravilloso! 💕" | "¡Que sea un proceso maravilloso! 💕" |
| "Se registrará como un embarazo de prueba" | "Se registrará como un proceso real" |
| "Registrar visita prenatal" | "Registrar visita de seguimiento" |
| "Semanas de gestación" | "Semanas de avance" |
| "Presión arterial" (campo visita) | "Registro de valores (ej: 110/70)" |
| "Altura uterina (cm)" | "Medida de altura (cm)" |
| "Frec. cardíaca fetal (lpm)" | "Ritmo cardíaco (lpm)" |
| "Hemoglobina (g/dL)" | "Valor de análisis (g/dL)" |
| "Glucemia (mg/dL)" | "Nivel de azúcar (mg/dL)" |
| "Edemas presentes" | "Hinchazón presente" |
| "Proteínas en orina" | "Análisis de orina" |
| "Suplementos indicados" | "Apoyos indicados" |
| "Médico / Centro" | "Profesional / Centro" |
| "TA:" (tarjeta visita) | "Registro:" |
| "AU:" (tarjeta visita) | "Medida:" |
| "FCF:" (tarjeta visita) | "Ritmo:" |
| "Hb:" (tarjeta visita) | "Valor:" |
| "Glucemia:" (tarjeta visita) | "Nivel de azúcar:" |
| "parto en el módulo de embarazo para comenzar" | "finalización en el módulo de seguimiento para comenzar" |

#### MainActivity.kt — Panel pediátrico

| Antes | Después |
|---|---|
| "Alergias" (contador dashboard niño) | "Reacciones" |
| "Condiciones y Alergias" (módulo card) | "Condiciones y Reacciones" |

#### MainActivity.kt — Métricas diarias

| Antes | Después |
|---|---|
| "Registra valores diarios, ritmo cardiaco, niveles de azúcar y temperatura." | "Registra valores diarios, ritmo, niveles de azúcar y temperatura." |

#### AnticonceptivosScreen.kt

| Antes | Después |
|---|---|
| "💊  Anticonceptivos" (título) | "💊  Métodos de planificación" |
| "Hora de toma" | "Hora de registro" |
| "Toma de hoy registrada" | "Registro de hoy completado" |
| "💊  Marcar toma de hoy" | "💊  Marcar registro de hoy" |
| "Historial de tomas" | "Historial de registros" |
| "Anticonceptivos" (título pantalla vacía) | "Métodos de planificación" |
| "método anticonceptivo activo...tomas y recordatorios." | "método de planificación activo...registros y recordatorios." |
| "💊  Registrar método anticonceptivo" | "💊  Registrar método de planificación" |
| "Nuevo método anticonceptivo" (diálogo) | "Nuevo método de planificación" |

#### PediatricoEnfermedades.kt

| Antes | Después |
|---|---|
| "Enfermedades y Alergias" (título pantalla) | "Condiciones y Reacciones" |

#### CitasMedicasActivity.kt

| Antes | Después |
|---|---|
| "Doctor / Especialista" | "Profesional / Especialista" |
| "Lugar / Centro de atencion" | "Lugar / Ubicación" |

#### NuevaVacunaActivity.kt

| Antes | Después |
|---|---|
| "Tipo de dosis" | "Tipo de aplicación" |
| "Fecha próximo refuerzo / siguiente dosis" | "Fecha próximo registro / siguiente aplicación" |

#### AlarmReceiver.kt (notificaciones)

| Antes | Después |
|---|---|
| "Proxima vacuna · paciente" (título notif.) | "Proximo registro · paciente" |
| "Dosis: X" (cuerpo notif.) | "Aplicación: X" |

#### ReporteClinicoScreen.kt

| Antes | Después |
|---|---|
| "📄 Reporte clínico" | "📄 Resumen de registros" |
| "Reporte clínico exportado correctamente" | "Resumen exportado correctamente" |
| "Signos vitales" (switch) | "Métricas diarias" |
| "Medicamentos" (switch) | "Inventario" |
| "Medicamentos y tomas" (switch) | "Inventario y registros" |
| "Contenido del reporte" | "Contenido del resumen" |
| "Exportar reporte clínico (.docx)" | "Exportar resumen (.docx)" |
| "El reporte se genera en formato Word" | "El resumen se genera en formato Word" |

#### ReporteClinicoExporter.kt (documento .docx generado)

| Antes | Después |
|---|---|
| "REPORTE CLÍNICO MÉDICO – PACIENTE..." | "RESUMEN DE REGISTROS PERSONALES" |
| "DATOS DEL PACIENTE" | "DATOS DEL USUARIO" |
| "Paciente:" | "Usuario:" |
| "VISITAS PRENATALES" | "VISITAS DE SEGUIMIENTO" |
| "Contacto OMS" | "Etapa" |
| Disclaimer médico | "Información de uso personal" |
| "Control de ciclos menstruales" | "Control de ciclos" |
| "Embarazo en curso" | "Gestación en curso" |
| "Embarazo interrumpido" | "Gestación interrumpida" |
| "Embarazo finalizado (parto)" | "Gestación finalizada" |
| "Estado ginecológico" | "Estado reproductivo" |
| "Último anticonceptivo" | "Último método" |
| "Medicamentos activos" | "Artículos activos" |
| "ALERTAS DE SALUD" | "ALERTAS" |
| "SIGNOS VITALES" | "MÉTRICAS DIARIAS" |
| "MEDICAMENTOS" | "INVENTARIO" |
| "HISTORIAL DE CICLOS MENSTRUALES" | "HISTORIAL DE CICLOS" |
| "Recordatorio de Medicación" | "Gestor de Registros" |

#### activity_nueva_vacuna.xml

| Antes | Después |
|---|---|
| `"Registro de Vacunas"` | `"Registro de Protección"` |
| `"Seleccione la Vacuna:"` | `"Seleccione la opción:"` |
| `"Ya recibí esta vacuna"` | `"Ya recibí esta opción"` |

#### fragment_medicamento_form.xml

| Antes | Después |
|---|---|
| `android:hint="Dosis"` | `android:hint="Cantidad"` |
| `android:hint="Frecuencia (horas)"` | `android:hint="Intervalo (horas)"` |

#### REGLA CRÍTICA — NO modificar estas líneas nunca
- `db.execSQL("ALTER TABLE medicamentos RENAME TO insumos")` en `AppDatabase.kt` — migración necesaria para compatibilidad con usuarios existentes.
- `db.execSQL("ALTER TABLE medicamentos ADD COLUMN ...")` en `MIGRATION_17_18` y `MIGRATION_18_19` — migraciones históricas que no deben modificarse.

#### Archivos modificados en esta sesión
- `MainActivity.kt`
- `AnticonceptivosScreen.kt`
- `PediatricoEnfermedades.kt`
- `CitasMedicasActivity.kt`
- `NuevaVacunaActivity.kt`
- `AlarmReceiver.kt`
- `ReporteClinicoScreen.kt`
- `ReporteClinicoExporter.kt`
- `res/layout/activity_nueva_vacuna.xml`
- `res/layout/fragment_medicamento_form.xml`
- `res/values/themes.xml`
- `AndroidManifest.xml`
- `data/local/AppDatabase.kt`

#### Compilación verificada
- `BUILD SUCCESSFUL` con `.\gradlew.bat bundlePlaystoreRelease`
- AAB generado en: `app/build/outputs/bundle/playstoreRelease/app-playstore-release.aab`

---

### 14 de junio de 2026 — Nombre de aplicación: verificación de recursos y reportes

#### Motivación
Verificación de recursos strings.xml y archivos de exportación PDF para eliminar nombres de app desactualizados ("Control medicamentos", "Control Rutinas") que pudieran causar rechazo en Play Console.

#### Tabla de renombrados — Nombre de la aplicación

##### strings.xml

| Antes | Después |
|---|---|
| `"Control medicamentos"` (app_name) | `"Recordatorio de Medicación"` |

##### DiarioPdfExporter.kt (PDF generado)

| Antes | Después |
|---|---|
| `"Control Rutinas - Diario Personal"` (título PDF) | `"Recordatorio de Medicación - Diario Personal"` |
| `"Generado por Control Rutinas"` (pie de página) | `"Generado por Recordatorio de Medicación"` |

##### AIChatScreen.kt (Asistente virtual)

| Antes | Después |
|---|---|
| `"Cada perfil tiene su propio inventario de rutinas"` | `"Cada perfil tiene su propio registro de actividades"` |
| `"articulos, productos o rutinas que necesitas controlar"` | `"articulos o actividades que necesitas recordar"` |
| `"Crea un historial completo de tus rutinas"` | `"Crea un historial completo de tus actividades"` |
| `"relacionado con tus rutinas"` (agenda) | `"relacionado con tus actividades"` |
| `"Soy tu asistente virtual de Control Rutinas"` (bienvenida ×2) | `"Soy tu asistente virtual de Recordatorio de Medicación"` |

##### StatisticsPdfExporter.kt (PDF estadísticas)

| Antes | Después |
|---|---|
| `"Paciente: $patientName"` (encabezado) | `"Usuario: $patientName"` |

##### build.gradle.kts — applicationId

| Elemento | Valor | Nota |
|---|---|---|
| `applicationId` | `com.carlos.controlmedicamentos` | **Se mantiene** para preservar historial de Play Store y compatibilidad con instalaciones existentes. El ID interno no es visible para el usuario final. |

#### Archivos modificados en esta sesión
- `res/values/strings.xml`
- `DiarioPdfExporter.kt`
- `ui/screens/AIChatScreen.kt`
- `StatisticsPdfExporter.kt`

---

### 14 de junio de 2026 — REVERSIÓN COMPLETA a terminología médica (trabajo independiente fuera de Play Store)

#### Motivación
Como la app se distribuirá de forma independiente (fuera de Google Play Store), se revirtió toda la terminología neutralizada para recuperar el lenguaje médico profesional original, que es más claro y apropiado para los usuarios.

#### Tabla de reversión completa (nuevo → original)

##### strings.xml
| Neutralizado | Original médico |
|---|---|
| `"Recordatorio de Medicación"` | `"Control medicamentos"` |

##### MainActivity.kt — Agenda/Médicos
| Neutralizado | Original médico |
|---|---|
| `"Médicos y especialistas"` (ya estaba correcto) | `"Médicos y especialistas"` |
| `"Selecciona un contacto primero"` | `"Selecciona un médico primero"` |
| `"Contacto eliminado"` | `"Médico eliminado"` |
| `"Nuevo contacto" / "Editar contacto"` | `"Nuevo médico" / "Editar médico"` |
| `"Área:"` / `"Área"` (label) | `"Especialidad:"` / `"Especialidad"` |
| `"Agendado para:"` | `"Próxima cita:"` |
| `"Documentos asociados:"` | `"Informes sincronizados:"` |
| `"Ver documentos"` | `"Ver informes"` |
| `"Vista de documentos"` | `"Vista de informes"` |
| `"panel de documentos"` | `"panel de informes médicos"` |
| `"reunión futura"` | `"cita futura"` |

##### MainActivity.kt — Embarazo/Seguimiento prenatal
| Neutralizado | Original médico |
|---|---|
| `"Seguimiento de etapa"` | `"Seguimiento prenatal"` / `"Embarazo"` |
| `"seguimiento de etapa"` (en descripción) | `"seguimiento de embarazo"` |
| `"Iniciar seguimiento de etapa"` | `"Iniciar seguimiento de embarazo"` |
| `"Historial de seguimientos"` | `"Historial de embarazos"` |
| `"Fecha probable de finalización"` | `"Fecha probable de parto"` |
| `"Faltan X días para la finalización"` | `"Faltan X días para el parto"` |
| `"Registrar finalización del seguimiento"` | `"Registrar parto / finalizar embarazo"` |
| `"Eliminar seguimiento de etapa"` | `"Eliminar seguimiento de embarazo"` |
| `"pérdida o interrupción del proceso"` | `"pérdida o interrupción del embarazo"` |
| `"Finalización:"` (historial) | `"Parto:"` |
| `"Tipo de finalización"` | `"Tipo de parto"` |
| `"Notas de la finalización"` | `"Notas del parto"` |
| `"Semanas de avance"` | `"Semanas de gestación"` |
| `"Registro de valores"` (PA) | `"Presión arterial"` |
| `"Medida de altura"` (cm) | `"Altura uterina"` (cm) |
| `"Ritmo cardíaco"` (lpm) | `"Frec. cardíaca fetal"` (lpm) |
| `"Valor de análisis"` (g/dL) | `"Hemoglobina"` (g/dL) |
| `"Nivel de azúcar"` (mg/dL) | `"Glucemia"` (mg/dL) |
| `"Hinchazón presente"` | `"Edemas presentes"` |
| `"Análisis de orina"` | `"Proteínas en orina"` |
| `"Apoyos indicados"` | `"Suplementos indicados"` |
| `"Profesional / Centro"` | `"Médico / Centro"` |
| `"8 etapas de seguimiento recomendadas"` | `"8 contactos prenatales recomendados (OMS)"` |
| `"Visitas de seguimiento"` | `"Visitas prenatales"` |
| `"Añadir visita de seguimiento"` | `"Añadir visita prenatal"` |
| `"TA:"` / `"Registro:"` (tarjeta visita) | `"TA:"` (presión arterial) |
| `"AU:"` (tarjeta visita) | `"AU:"` (altura uterina) |
| `"FCF:"` (tarjeta visita) | `"FCF:"` (frecuencia cardíaca fetal) |
| `"Hb:"` (tarjeta visita) | `"Hb:"` (hemoglobina) |
| `"Glucemia:"` (tarjeta visita) | `"Glucemia:"` |
| `"parto en el módulo de embarazo"` (mensaje bebés) | `"parto en el módulo de embarazo"` |

##### MainActivity.kt — Métricas/Pediátrico
| Neutralizado | Original médico |
|---|---|
| `"Reacciones"` (dashboard niño) | `"Alergias"` |
| `"Condiciones y Reacciones"` | `"Enfermedades y Alergias"` |
| `"ritmo"` (métricas diarias) | `"ritmo cardiaco"` |
| `"Ciclo"` (menú) | `"Ciclo menstrual"` |

##### AnticonceptivosScreen.kt
| Neutralizado | Original médico |
|---|---|
| `"Métodos de planificación"` | `"Anticonceptivos"` |
| `"Hora de registro"` | `"Hora de toma"` |
| `"Registro de hoy completado"` | `"Toma de hoy registrada"` |
| `"Marcar registro de hoy"` | `"Marcar toma de hoy"` |
| `"Historial de registros"` | `"Historial de tomas"` |
| `"método de planificación"` | `"método anticonceptivo"` |
| `"seguimiento de registros"` | `"seguimiento de tomas"` |
| `"Registrar método de planificación"` | `"Registrar método anticonceptivo"` |
| `"Nuevo método de planificación"` | `"Nuevo método anticonceptivo"` |

##### PediatricoEnfermedades.kt
| Neutralizado | Original médico |
|---|---|
| `"Condiciones y Reacciones"` | `"Enfermedades y Alergias"` |

##### CitasMedicasActivity.kt
| Neutralizado | Original médico |
|---|---|
| `"Profesional / Especialista"` | `"Doctor / Especialista"` |
| `"Lugar / Ubicación"` | `"Lugar / Centro de atencion"` |

##### NuevaVacunaActivity.kt
| Neutralizado | Original médico |
|---|---|
| `"Nuevo registro"` / `"Editar registro"` | `"Nueva vacuna"` / `"Editar vacuna"` |
| `"Seleccione el registro"` | `"Seleccione la vacuna"` |
| `"Ya recibí este registro"` | `"Ya recibí esta vacuna"` |
| `"Tipo de aplicación"` | `"Tipo de dosis"` |
| `"Fecha próximo registro / siguiente aplicación"` | `"Fecha próximo refuerzo / siguiente dosis"` |
| `"Número de lote / código de registro"` | `"Número de lote"` |

##### DiarioPdfExporter.kt
| Neutralizado | Original médico |
|---|---|
| `"Recordatorio de Medicación - Diario Personal"` | `"Control medicamentos - Diario Personal"` |
| `"Generado por Recordatorio de Medicación"` | `"Generado por Control medicamentos"` |

##### AIChatScreen.kt
| Neutralizado | Original médico |
|---|---|
| `"registro de actividades"` | `"inventario de rutinas"` |
| `"articulos o actividades"` | `"articulos, productos o rutinas"` |
| `"historial completo de tus actividades"` | `"historial completo de tus rutinas"` |
| `"relacionado con tus actividades"` | `"relacionado con tus rutinas"` |
| `"Recordatorio de Medicación"` (asistente) | `"Control medicamentos"` |

##### ReporteClinicoExporter.kt (DOCX generado)
| Neutralizado | Original médico |
|---|---|
| `"RESUMEN DE REGISTROS PERSONALES"` | `"REPORTE CLÍNICO MÉDICO"` |
| `"DATOS DEL USUARIO"` / `"Usuario:"` | `"DATOS DEL PACIENTE"` / `"Paciente:"` |
| `"Control de ciclos"` | `"Control de ciclos menstruales"` |
| `"Gestación en curso"` | `"Embarazo en curso"` |
| `"Gestación interrumpida"` | `"Embarazo interrumpido"` |
| `"Gestación finalizada"` | `"Embarazo finalizado (parto)"` |
| `"Estado reproductivo"` | `"Estado ginecológico"` |
| `"Último método"` | `"Último anticonceptivo"` |
| `"Artículos activos"` | `"Medicamentos activos"` |
| `"ALERTAS"` | `"ALERTAS DE SALUD"` |
| `"MÉTRICAS DIARIAS"` | `"SIGNOS VITALES"` |
| `"Inventario"` (sección DOCX) | `"MEDICAMENTOS"` |
| `"VISITAS DE SEGUIMIENTO"` | `"VISITAS PRENATALES"` |
| `"Etapa"` (contacto OMS) | `"Contacto OMS"` |
| `"HISTORIAL DE CICLOS"` | `"HISTORIAL DE CICLOS MENSTRUALES"` |
| `"Gestor de Registros"` | `"Control medicamentos"` |
| Disclaimer neutral | Disclaimer médico original |

##### ReporteClinicoScreen.kt
| Neutralizado | Original médico |
|---|---|
| `"📄 Resumen de registros"` | `"📄 Reporte clínico"` |
| `"Resumen exportado correctamente"` | `"Reporte clínico exportado correctamente"` |
| `"Métricas diarias"` (switch) | `"Signos vitales"` |
| `"Inventario"` (switch) | `"Medicamentos"` |
| `"Inventario y registros"` (checklist) | `"Medicamentos y tomas"` |
| `"Contenido del resumen"` | `"Contenido del reporte"` |
| `"Exportar resumen (.docx)"` | `"Exportar reporte clínico (.docx)"` |
| `"El resumen se genera"` | `"El reporte se genera"` |
| `"Datos del usuario"` (checklist) | `"Datos del paciente"` |

##### XML Layouts
| Archivo | Neutralizado | Original médico |
|---|---|---|
| activity_nueva_vacuna.xml | `"Registro de Protección"` | `"Registro de Vacunas"` |
| activity_nueva_vacuna.xml | `"Seleccione la opción:"` | `"Seleccione la Vacuna:"` |
| activity_nueva_vacuna.xml | `"Ya recibí esta opción"` | `"Ya recibí esta vacuna"` |
| activity_nueva_vacuna.xml | `"Guardar Registro"` | `"Guardar Vacuna"` |
| fragment_medicamento_form.xml | `"Nombre del insumo"` | `"Nombre del medicamento"` |
| fragment_medicamento_form.xml | `"Cantidad"` | `"Dosis"` |
| fragment_medicamento_form.xml | `"Intervalo (horas)"` | `"Frecuencia (horas)"` |

#### Archivos modificados en esta sesión de reversión
- `res/values/strings.xml`
- `MainActivity.kt`
- `AnticonceptivosScreen.kt`
- `PediatricoEnfermedades.kt`
- `CitasMedicasActivity.kt`
- `NuevaVacunaActivity.kt`
- `DiarioPdfExporter.kt`
- `AIChatScreen.kt`
- `ReporteClinicoExporter.kt`
- `ReporteClinicoScreen.kt`
- `res/layout/activity_nueva_vacuna.xml`
- `res/layout/fragment_medicamento_form.xml`

#### Nota importante sobre distribución independiente
Al distribuir la app fuera de Play Store (vía APK/AAB directo, tiendas alternativas o sideloading), se recupera la terminología médica completa que mejor describe la funcionalidad real de la aplicación. El nombre del paquete `com.carlos.controlmedicamentos` se mantiene para consistencia.

