# Windows y Sincronizacion

Este documento fija el primer corte tecnico para llevar Control medicamentos a Windows con sincronizacion bidireccional.

## Objetivos

- Mantener Android estable mientras se prepara la reutilizacion de datos.
- Definir un contrato de snapshot comun para Android y Windows.
- Separar modelo compartido de Android y Room.
- Preparar una futura sincronizacion incremental por Wi-Fi.

## Base creada en este cambio

- Nuevo modulo reutilizable `sync-core`.
- Modelos compartidos de snapshot en `sync-core/src/main/kotlin/com/carlos/controlmedicamentos/sync/model/SyncModels.kt`.
- Mappers Android en `app/src/main/java/com/carlos/controlmedicamentos/sync/AndroidSyncSnapshotMapper.kt`.

## Direccion tecnica

### Fase 1

- Usar `SyncSnapshot` como contrato comun entre plataformas.
- Reutilizar el snapshot para exportacion, importacion y futuras pruebas de escritorio.
- Mantener Room y alarmas dentro de Android mientras el resto se desacopla.

### Fase 2

- Crear modulo o aplicacion de escritorio para Windows.
- Cargar y mostrar `SyncSnapshot` en Windows antes de meter escritura completa.
- Añadir almacenamiento local de escritorio separado del Android.

### Fase 3

- Agregar metadatos de sincronizacion por entidad: `updatedAt`, `deletedAt`, `deviceId`, `syncVersion`.
- Implementar merge bidireccional y resolucion de conflictos.
- Publicar sincronizacion local por Wi-Fi con emparejamiento entre dispositivos.

## Decision de transporte

- Prioridad: Wi-Fi local en la misma red.
- Cable: segunda fase, probablemente apoyada en un canal asistido por escritorio, no como mecanismo principal.

## Riesgos actuales

- `MainActivity` sigue concentrando mucha logica y acceso directo a base de datos.
- El backup actual restaura por reemplazo completo; eso sirve para respaldo, no para merge bidireccional.
- Antes de sincronizacion real hay que introducir metadatos de cambios por registro.