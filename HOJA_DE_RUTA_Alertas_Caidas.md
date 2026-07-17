# Hoja de Ruta - Módulo de Alerta de Caídas

## Objetivo

Incorporar un módulo de detección de caídas para personas mayores dentro de la aplicación ControlMedicamentos. El módulo debe detectar caídas mediante sensores del dispositivo, activar una alarma local con pantalla de emergencia y guardar el evento en la base de datos local.

## Fases de implementación

### Fase 1: Modelo de datos y configuración

- Crear entidad `FallAlert` en `data/local/FallAlert.kt`.
- Crear `FallAlertDao` en `data/local/FallAlertDao.kt`.
- Agregar `FallAlert` y `FallAlertDao` a `AppDatabase.kt` (aumentar versión de base de datos a 46).
- Crear migración `MIGRATION_45_46` para crear la tabla `fall_alerts`.

### Fase 2: Permisos y manifiesto

- Agregar permisos en `AndroidManifest.xml`:
  - `BODY_SENSORS` (acelerómetro/giroscopio).
  - `HIGH_SAMPLING_RATE_SENSORS` (frecuencia alta de muestreo en Android 12+).
  - `FOREGROUND_SERVICE_DATA_SYNC` o `FOREGROUND_SERVICE_SPECIAL_USE` (tipo de servicio para detección).
  - `SEND_SMS` (opcional, para alerta a contacto).
- Declarar servicio `FallDetectionService` con `foregroundServiceType` adecuado.
- Declarar actividad `FallAlertActivity` con `showOnLockScreen`, `turnScreenOn`, `showWhenLocked` y `launchMode="singleInstance"`.

### Fase 3: Servicio de detección de caídas

- Crear `FallDetectionService.kt` (servicio en primer plano).
- Registrar `SensorManager` para `TYPE_ACCELEROMETER` y `TYPE_GYROSCOPE`.
- Implementar algoritmo de detección en `FallDetectionAlgorithm.kt`:
  - Detectar pico de aceleración > umbral (caída libre + impacto).
  - Detectar cambio de orientación.
  - Esperar periodo de inmovilidad para confirmar.
  - Notificar al servicio cuando se confirma una caída.
- Mostrar notificación persistente mientras el servicio está activo.
- Iniciar `FallAlertActivity` con pantalla completa e intent explícito.

### Fase 4: Alarma y pantalla de emergencia

- Crear `FallAlertActivity.kt` con Compose o XML.
- Mostrar contador de cuenta regresiva (por ejemplo, 10 segundos).
- Reproducir sonido de alarma usando `RingtoneManager` o `MediaPlayer`.
- Botones: "Estoy bien / Cancelar" y "Sí, necesito ayuda".
- Si se confirma la caída, guardar `FallAlert` en Room y opcionalmente enviar SMS/notificación.

### Fase 5: Integración con MainActivity

- Agregar opción en el menú del escritorio: "Alerta de caídas".
- Crear pantalla `AlertaCaidasScreen` para:
  - Activar/desactivar monitoreo.
  - Configurar contacto de emergencia.
  - Ver historial de caídas detectadas.
- Iniciar/detener `FallDetectionService` desde esta pantalla.
- Solicitar permisos necesarios en tiempo de ejecución.

### Fase 6: Pruebas

- Verificar compilación (`gradlew.bat :app:compileDebugKotlin`).
- Instalar en dispositivo físico (`SM-S918U`).
- Probar detección simulada (agitando el teléfono bruscamente).
- Verificar notificación, sonido, contador y guardado en base de datos.
- Probar cancelación de falsa alarma.

## Notas técnicas

- Frecuencia de muestreo recomendada: `SENSOR_DELAY_GAME` o `SENSOR_DELAY_FASTEST` para detección rápida, aunque consume más batería.
- Umbral de impacto: típicamente entre 2.5g y 4g (ajustable).
- Umbral de caída libre: aceleración cercana a 0g durante breve periodo, seguida de impacto.
- Inmovilidad posterior: verificar que la aceleración se estabilice cerca de 1g tras el impacto.
- Considerar falsos positivos: sentarse rápido, dejar caer el teléfono, correr. Se mitiga con orientación e inmovilidad.

## Archivos a crear/modificar

### Nuevos archivos

- `app/src/main/java/com/carlos/controlmedicamentos/data/local/FallAlert.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/FallAlertDao.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/fall/FallDetectionAlgorithm.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/fall/FallDetectionService.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/FallAlertActivity.kt`
- `app/src/main/res/drawable/ic_fall_alert.xml` (icono de notificación, opcional)

### Archivos a modificar

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/carlos/controlmedicamentos/data/local/AppDatabase.kt`
- `app/src/main/java/com/carlos/controlmedicamentos/MainActivity.kt` (menú y pantalla)

## Criterios de aceptación

- El usuario puede activar/desactivar el monitoreo de caídas.
- El servicio muestra una notificación persistente cuando está activo.
- Al detectar una caída, suena una alarma y aparece una pantalla de emergencia.
- El usuario puede cancelar la alarma en un tiempo configurable.
- Si no se cancela, se guarda el evento en la base de datos.
- El historial de caídas es visible desde la pantalla de configuración.
- La app compila e instala correctamente.
