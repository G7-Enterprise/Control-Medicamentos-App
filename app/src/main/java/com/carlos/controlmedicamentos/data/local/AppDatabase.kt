package com.carlos.controlmedicamentos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Medication::class,
        MedicationIntake::class,
        PatientProfile::class,
        MedicalReport::class,
        MedicalAppointment::class,
        MedicalPractitioner::class,
        VaccinationRecord::class,
        SignosVitales::class,
        MedicationOrder::class,
        PhysicalActivity::class,
        CarritoPendienteItem::class,
        CicloMenstrual::class,
        RegistroDiarioCiclo::class,
        ControlEmbarazo::class,
        VisitaPrenatal::class,
        MetodoAnticonceptivo::class,
        AnticonceptivoIntake::class,
        BebeRecienNacido::class,
        NinoEntity::class,
        VacunaEntity::class,
        ControlPediatricoEntity::class,
        EnfermedadEntity::class,
        DiarioEntry::class,
        FallAlert::class,
        RegistroHidratacion::class,
        Dentista::class,
        VisitaDentista::class,
        DiagnosticoDental::class,
        ProcedimientoDental::class,
        PrescripcionDental::class,
        RegistroSedentarismo::class,
        ConfigSedentarismo::class,
        DienteEstado::class,
        ImagenDental::class,
        TransaccionDental::class,
        Ortodoncia::class,
        AjusteOrtodoncia::class,
        IncidenciaOrtodoncia::class,
        ElasticoOrtodoncia::class
    ],
    version = 58,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationIntakeDao(): MedicationIntakeDao
    abstract fun patientProfileDao(): PatientProfileDao
    abstract fun medicalReportDao(): MedicalReportDao
    abstract fun medicalAppointmentDao(): MedicalAppointmentDao
    abstract fun medicalPractitionerDao(): MedicalPractitionerDao
    abstract fun vaccinationRecordDao(): VaccinationRecordDao
    abstract fun signosVitalesDao(): SignosVitalesDao
    abstract fun medicationOrderDao(): MedicationOrderDao
    abstract fun physicalActivityDao(): PhysicalActivityDao
    abstract fun carritoPendienteDao(): CarritoPendienteDao
    abstract fun cicloMenstrualDao(): CicloMenstrualDao
    abstract fun registroDiarioCicloDao(): RegistroDiarioCicloDao
    abstract fun controlEmbarazoDao(): ControlEmbarazoDao
    abstract fun visitaPrenatalDao(): VisitaPrenatalDao
    abstract fun metodoAnticonceptivoDao(): MetodoAnticonceptivoDao
    abstract fun anticonceptivoIntakeDao(): AnticonceptivoIntakeDao
    abstract fun bebeRecienNacidoDao(): BebeRecienNacidoDao
    abstract fun ninoDao(): NinoDao
    abstract fun vacunaDao(): VacunaDao
    abstract fun controlPediatricoDao(): ControlPediatricoDao
    abstract fun enfermedadDao(): EnfermedadDao
    abstract fun diarioEntryDao(): DiarioEntryDao
    abstract fun fallAlertDao(): FallAlertDao
    abstract fun hidratacionDao(): HidratacionDao
    abstract fun dentistaDao(): DentistaDao
    abstract fun visitaDentistaDao(): VisitaDentistaDao
    abstract fun diagnosticoDentalDao(): DiagnosticoDentalDao
    abstract fun procedimientoDentalDao(): ProcedimientoDentalDao
    abstract fun prescripcionDentalDao(): PrescripcionDentalDao
    abstract fun dienteEstadoDao(): DienteEstadoDao
    abstract fun imagenDentalDao(): ImagenDentalDao
    abstract fun transaccionDentalDao(): TransaccionDentalDao
    abstract fun ortodonciaDao(): OrtodonciaDao
    abstract fun ajusteOrtodonciaDao(): AjusteOrtodonciaDao
    abstract fun incidenciaOrtodonciaDao(): IncidenciaOrtodonciaDao
    abstract fun elasticoOrtodonciaDao(): ElasticoOrtodonciaDao
    abstract fun sedentarismoDao(): SedentarismoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS carrito_pendiente (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        medicationId INTEGER NOT NULL,
                        unidadesSolicitadas INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE insumos ADD COLUMN formaInsumo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE insumos ADD COLUMN colorInsumo TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE insumos ADD COLUMN colorInsumo2 TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE physical_activities ADD COLUMN altitudMaxMetros REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presion_arterial ADD COLUMN imc REAL")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE physical_activities ADD COLUMN altitudInicioMetros REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE physical_activities ADD COLUMN desnivelPositivoMetros REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE physical_activities ADD COLUMN desnivelNegativoMetros REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patient_profile ADD COLUMN sexo TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS ciclos_menstruales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        fechaInicio INTEGER NOT NULL,
                        duracionDias INTEGER NOT NULL DEFAULT 5,
                        duracionCicloDias INTEGER NOT NULL DEFAULT 28,
                        sintomas TEXT NOT NULL DEFAULT '',
                        notas TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS control_embarazo (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        fechaUltimaRegla INTEGER NOT NULL,
                        fechaProbableParto INTEGER NOT NULL,
                        notas TEXT NOT NULL DEFAULT '',
                        activo INTEGER NOT NULL DEFAULT 1,
                        fechaRegistro INTEGER NOT NULL DEFAULT 0,
                        fechaParto INTEGER,
                        tipoPartoRegistrado TEXT NOT NULL DEFAULT '',
                        notasParto TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_control_embarazo_patientId ON control_embarazo(patientId)")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE control_embarazo ADD COLUMN estadoEmbarazo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE control_embarazo ADD COLUMN fechaFin INTEGER")
                db.execSQL("ALTER TABLE control_embarazo ADD COLUMN tipoInterrupcion TEXT")
                db.execSQL("ALTER TABLE control_embarazo ADD COLUMN metodoInterrupcion TEXT")
                db.execSQL("ALTER TABLE control_embarazo ADD COLUMN notasInterrupcion TEXT")
                db.execSQL("ALTER TABLE visita_prenatal ADD COLUMN contactoOMS INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metodos_anticonceptivos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        tipo TEXT NOT NULL,
                        fechaInicio INTEGER NOT NULL,
                        horaToma TEXT NOT NULL DEFAULT '08:00',
                        activo INTEGER NOT NULL DEFAULT 1,
                        notas TEXT NOT NULL DEFAULT '',
                        fechaRegistro INTEGER NOT NULL,
                        duracionCicloDias INTEGER,
                        diasDescanso INTEGER,
                        proximaCita INTEGER,
                        recordatorioDiasAntes INTEGER NOT NULL DEFAULT 3
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS visita_prenatal")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS control_embarazo_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        fechaUltimaRegla INTEGER NOT NULL,
                        fechaProbableParto INTEGER NOT NULL,
                        notas TEXT NOT NULL,
                        activo INTEGER NOT NULL,
                        fechaRegistro INTEGER NOT NULL,
                        fechaParto INTEGER,
                        tipoPartoRegistrado TEXT NOT NULL,
                        notasParto TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """INSERT INTO control_embarazo_new (id, patientId, fechaUltimaRegla, fechaProbableParto, notas, activo, fechaRegistro, fechaParto, tipoPartoRegistrado, notasParto)
                    SELECT id, patientId, fechaUltimaRegla, fechaProbableParto, notas, activo, 0, CASE WHEN fechaParto = 0 OR fechaParto IS NULL THEN NULL ELSE fechaParto END, '', notasParto
                    FROM control_embarazo"""
                )
                db.execSQL("DROP TABLE control_embarazo")
                db.execSQL("ALTER TABLE control_embarazo_new RENAME TO control_embarazo")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS visita_prenatal (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        embarazoId INTEGER NOT NULL,
                        fecha INTEGER NOT NULL,
                        semanasGestacion INTEGER NOT NULL,
                        peso REAL,
                        presionArterial TEXT NOT NULL,
                        alturaUterina REAL,
                        frecuenciaCardiacaFetal INTEGER,
                        edemas INTEGER NOT NULL,
                        hemoglobina REAL,
                        glucemia REAL,
                        proteinasOrina INTEGER NOT NULL,
                        suplementos TEXT NOT NULL,
                        observaciones TEXT NOT NULL,
                        proximaVisitaSemanas INTEGER,
                        facultativo TEXT NOT NULL,
                        FOREIGN KEY(embarazoId) REFERENCES control_embarazo(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visita_prenatal_embarazoId ON visita_prenatal(embarazoId)")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS visita_prenatal (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        embarazoId INTEGER NOT NULL,
                        fecha INTEGER NOT NULL,
                        semanasGestacion INTEGER NOT NULL,
                        peso REAL,
                        presionArterial TEXT NOT NULL DEFAULT '',
                        alturaUterina REAL,
                        frecuenciaCardiacaFetal INTEGER,
                        edemas INTEGER NOT NULL DEFAULT 0,
                        hemoglobina REAL,
                        glucemia REAL,
                        proteinasOrina INTEGER NOT NULL DEFAULT 0,
                        suplementos TEXT NOT NULL DEFAULT '',
                        observaciones TEXT NOT NULL DEFAULT '',
                        proximaVisitaSemanas INTEGER,
                        facultativo TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(embarazoId) REFERENCES control_embarazo(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visita_prenatal_embarazoId ON visita_prenatal(embarazoId)")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS registros_diarios_ciclo (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cicloId INTEGER NOT NULL,
                        fecha INTEGER NOT NULL,
                        tipoSintoma TEXT NOT NULL,
                        valorSintoma TEXT NOT NULL,
                        intensidad INTEGER,
                        notas TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(cicloId) REFERENCES ciclos_menstruales(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_diarios_ciclo_cicloId ON registros_diarios_ciclo(cicloId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_diarios_ciclo_cicloId_fecha ON registros_diarios_ciclo(cicloId, fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_diarios_ciclo_tipoSintoma ON registros_diarios_ciclo(tipoSintoma)")
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bebes_recien_nacidos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        embarazoId INTEGER NOT NULL,
                        patientId INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        sexo TEXT NOT NULL,
                        fechaNacimiento INTEGER NOT NULL,
                        pesoAlNacer TEXT NOT NULL DEFAULT '',
                        tallaAlNacer TEXT NOT NULL DEFAULT '',
                        notas TEXT NOT NULL DEFAULT '',
                        fechaRegistro INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE control_embarazo ADD COLUMN esPrueba INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ninos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        embarazoId INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        fechaNacimiento TEXT NOT NULL,
                        sexo TEXT NOT NULL,
                        notasParto TEXT,
                        fechaRegistro INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vacunas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ninoId INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT NOT NULL,
                        edadRecomendada TEXT NOT NULL,
                        estaAplicada INTEGER NOT NULL DEFAULT 0,
                        fechaAplicacion TEXT,
                        FOREIGN KEY(ninoId) REFERENCES ninos(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS controles_pediatricos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ninoId INTEGER NOT NULL,
                        fechaControl TEXT NOT NULL,
                        pesoKg REAL,
                        tallaCm REAL,
                        perimetroCefalicoCm REAL,
                        observaciones TEXT,
                        FOREIGN KEY(ninoId) REFERENCES ninos(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS enfermedades_alergias (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ninoId INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        fechaInicio TEXT NOT NULL,
                        fechaFin TEXT,
                        sintomas TEXT,
                        planPersonal TEXT,
                        esAlergia INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(ninoId) REFERENCES ninos(id) ON DELETE CASCADE
                    )
                """)
            }
        }

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ninos ADD COLUMN esPrueba INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patient_profile ADD COLUMN pais TEXT NOT NULL DEFAULT 'Nicaragua'")
                db.execSQL("ALTER TABLE patient_profile ADD COLUMN moneda TEXT NOT NULL DEFAULT 'C$'")
            }
        }

        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diario_entradas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL DEFAULT 0,
                        fecha INTEGER NOT NULL,
                        texto TEXT NOT NULL DEFAULT '',
                        rutaImagen TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diario_patientId ON diario_entradas(patientId)")
            }
        }

        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presion_arterial ADD COLUMN patientId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_presion_arterial_patientId ON presion_arterial(patientId)")
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS anticonceptivo_intakes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        metodoId INTEGER NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        acceptedAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_anticonceptivo_intakes_metodoId_scheduledAt ON anticonceptivo_intakes(metodoId, scheduledAt)")
            }
        }

        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicamentos RENAME TO insumos")
            }
        }

        private val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS subscription_state")
            }
        }

        private val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patient_profile ADD COLUMN fotoPerfil TEXT")
            }
        }

        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_intakes ADD COLUMN medicationName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medication_intakes ADD COLUMN dosis TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE medication_intakes SET medicationName = " +
                    "(SELECT i.nombre FROM insumos i WHERE i.id = medication_intakes.medicationId), " +
                    "dosis = " +
                    "(SELECT i.dosis FROM insumos i WHERE i.id = medication_intakes.medicationId) " +
                    "WHERE EXISTS (SELECT 1 FROM insumos i WHERE i.id = medication_intakes.medicationId)"
                )
            }
        }

        private val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_intakes ADD COLUMN status TEXT NOT NULL DEFAULT 'TAKEN'")
            }
        }

        private val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS fall_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        confirmedAt INTEGER,
                        latitude REAL,
                        longitude REAL,
                        impactMagnitude REAL,
                        status TEXT NOT NULL DEFAULT 'DETECTED',
                        notes TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_patientId ON fall_alerts(patientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_detectedAt ON fall_alerts(detectedAt)")
            }
        }

        private val MIGRATION_46_47 = object : Migration(46, 47) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_fall_alerts_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_fall_alerts_detectedAt")
                db.execSQL("DROP TABLE IF EXISTS fall_alerts")
                db.execSQL(
                    """CREATE TABLE fall_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        confirmedAt INTEGER,
                        latitude REAL,
                        longitude REAL,
                        impactMagnitude REAL,
                        status TEXT NOT NULL DEFAULT 'DETECTED',
                        notes TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_patientId ON fall_alerts(patientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_detectedAt ON fall_alerts(detectedAt)")
            }
        }

        private val MIGRATION_47_48 = object : Migration(47, 48) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_fall_alerts_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_fall_alerts_detectedAt")
                db.execSQL("DROP TABLE IF EXISTS fall_alerts")
                db.execSQL(
                    """CREATE TABLE fall_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        confirmedAt INTEGER,
                        latitude REAL,
                        longitude REAL,
                        impactMagnitude REAL,
                        status TEXT NOT NULL DEFAULT 'DETECTED',
                        notes TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_patientId ON fall_alerts(patientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_detectedAt ON fall_alerts(detectedAt)")
            }
        }

        private val MIGRATION_52_53 = object : Migration(52, 53) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dentistas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        especialidad TEXT NOT NULL DEFAULT '',
                        telefono TEXT NOT NULL DEFAULT '',
                        direccion TEXT NOT NULL DEFAULT '',
                        notas TEXT NOT NULL DEFAULT '',
                        fechaRegistro INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS visitas_dentista (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        dentistaId INTEGER,
                        fechaHora INTEGER NOT NULL,
                        motivo TEXT NOT NULL DEFAULT '',
                        estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                        notas TEXT NOT NULL DEFAULT '',
                        recordatorio24h INTEGER NOT NULL DEFAULT 1,
                        recordatorio2h INTEGER NOT NULL DEFAULT 1,
                        seguimientoPostConsulta INTEGER NOT NULL DEFAULT 0,
                        fechaRegistro INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(dentistaId) REFERENCES dentistas(id) ON DELETE SET NULL
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visitas_dentista_dentistaId ON visitas_dentista(dentistaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visitas_dentista_patientId ON visitas_dentista(patientId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diagnosticos_dentales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        visitaId INTEGER NOT NULL,
                        patientId INTEGER NOT NULL,
                        numeroDiente INTEGER NOT NULL DEFAULT 0,
                        zona TEXT NOT NULL DEFAULT '',
                        descripcion TEXT NOT NULL,
                        estado TEXT NOT NULL DEFAULT 'ACTIVO',
                        fechaRegistro INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(visitaId) REFERENCES visitas_dentista(id) ON DELETE CASCADE
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diagnosticos_dentales_visitaId ON diagnosticos_dentales(visitaId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS procedimientos_dentales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diagnosticoId INTEGER NOT NULL,
                        patientId INTEGER NOT NULL,
                        tipo TEXT NOT NULL,
                        descripcion TEXT NOT NULL DEFAULT '',
                        fecha INTEGER NOT NULL,
                        completado INTEGER NOT NULL DEFAULT 0,
                        costo REAL,
                        notas TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(diagnosticoId) REFERENCES diagnosticos_dentales(id) ON DELETE CASCADE
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_procedimientos_dentales_diagnosticoId ON procedimientos_dentales(diagnosticoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_procedimientos_dentales_patientId ON procedimientos_dentales(patientId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS prescripciones_dentales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        visitaId INTEGER NOT NULL,
                        patientId INTEGER NOT NULL,
                        medicamento TEXT NOT NULL,
                        dosis TEXT NOT NULL DEFAULT '',
                        frecuencia TEXT NOT NULL DEFAULT '',
                        duracionDias INTEGER NOT NULL DEFAULT 0,
                        sincronizadaConAlarma INTEGER NOT NULL DEFAULT 0,
                        fechaRegistro INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(visitaId) REFERENCES visitas_dentista(id) ON DELETE CASCADE
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prescripciones_dentales_visitaId ON prescripciones_dentales(visitaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prescripciones_dentales_patientId ON prescripciones_dentales(patientId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS registros_sedentarismo (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        tipoEvento TEXT NOT NULL DEFAULT 'MOVIMIENTO',
                        minutosInactivo INTEGER NOT NULL DEFAULT 0,
                        notas TEXT NOT NULL DEFAULT ''
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_sedentarismo_patientId ON registros_sedentarismo(patientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_sedentarismo_timestamp ON registros_sedentarismo(timestamp)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS config_sedentarismo (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        activado INTEGER NOT NULL DEFAULT 0,
                        limiteInactividadMinutos INTEGER NOT NULL DEFAULT 60,
                        horaInicioMonitoreo INTEGER NOT NULL DEFAULT 7,
                        horaFinMonitoreo INTEGER NOT NULL DEFAULT 22,
                        diasActivos TEXT NOT NULL DEFAULT '1,2,3,4,5'
                    )""".trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_config_sedentarismo_patientId ON config_sedentarismo(patientId)")
            }
        }

        private val MIGRATION_53_54 = object : Migration(53, 54) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE registro_hidratacion ADD COLUMN tipoBebida TEXT NOT NULL DEFAULT 'Agua'")
            }
        }

        private fun crearTablasDentales(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS diente_estado")
            db.execSQL("DROP TABLE IF EXISTS imagenes_dentales")
            db.execSQL("DROP TABLE IF EXISTS transacciones_dentales")
            db.execSQL("DROP TABLE IF EXISTS ortodoncias")
            db.execSQL("DROP TABLE IF EXISTS ajustes_ortodoncia")
            db.execSQL("DROP TABLE IF EXISTS incidencias_ortodoncia")
            db.execSQL("DROP TABLE IF EXISTS elasticos_ortodoncia")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS diente_estado (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId INTEGER NOT NULL,
                    numeroDiente INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    notas TEXT NOT NULL,
                    fechaActualizacion INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_diente_estado_patientId_numeroDiente ON diente_estado(patientId, numeroDiente)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS imagenes_dentales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId INTEGER NOT NULL,
                    numeroDiente INTEGER NOT NULL,
                    ortodonciaId INTEGER,
                    uri TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    etapa TEXT NOT NULL,
                    notas TEXT NOT NULL,
                    fecha INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_imagenes_dentales_patientId ON imagenes_dentales(patientId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_imagenes_dentales_numeroDiente ON imagenes_dentales(numeroDiente)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_imagenes_dentales_ortodonciaId ON imagenes_dentales(ortodonciaId)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS transacciones_dentales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId INTEGER NOT NULL,
                    concepto TEXT NOT NULL,
                    categoria TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    monto REAL NOT NULL,
                    fecha INTEGER NOT NULL,
                    numeroDiente INTEGER NOT NULL,
                    visitaId INTEGER,
                    procedimientoId INTEGER,
                    ortodonciaId INTEGER,
                    notas TEXT NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_dentales_patientId ON transacciones_dentales(patientId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_dentales_numeroDiente ON transacciones_dentales(numeroDiente)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_dentales_visitaId ON transacciones_dentales(visitaId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_dentales_procedimientoId ON transacciones_dentales(procedimientoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_dentales_ortodonciaId ON transacciones_dentales(ortodonciaId)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS ortodoncias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    fechaInicio INTEGER NOT NULL,
                    fechaFinEstimada INTEGER,
                    activo INTEGER NOT NULL,
                    notas TEXT NOT NULL,
                    costoTotal REAL NOT NULL,
                    abonoTotal REAL NOT NULL,
                    fechaRegistro INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ortodoncias_patientId ON ortodoncias(patientId)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS ajustes_ortodoncia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ortodonciaId INTEGER NOT NULL,
                    fecha INTEGER NOT NULL,
                    descripcion TEXT NOT NULL,
                    dolor TEXT NOT NULL,
                    notas TEXT NOT NULL,
                    FOREIGN KEY(ortodonciaId) REFERENCES ortodoncias(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ajustes_ortodoncia_ortodonciaId ON ajustes_ortodoncia(ortodonciaId)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS incidencias_ortodoncia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ortodonciaId INTEGER,
                    patientId INTEGER NOT NULL,
                    numeroDiente INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    resuelto INTEGER NOT NULL,
                    FOREIGN KEY(ortodonciaId) REFERENCES ortodoncias(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_incidencias_ortodoncia_ortodonciaId ON incidencias_ortodoncia(ortodonciaId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_incidencias_ortodoncia_patientId ON incidencias_ortodoncia(patientId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_incidencias_ortodoncia_numeroDiente ON incidencias_ortodoncia(numeroDiente)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS elasticos_ortodoncia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ortodonciaId INTEGER NOT NULL,
                    dienteOrigen INTEGER NOT NULL,
                    dienteDestino INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    activo INTEGER NOT NULL,
                    notas TEXT NOT NULL,
                    FOREIGN KEY(ortodonciaId) REFERENCES ortodoncias(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_elasticos_ortodoncia_ortodonciaId ON elasticos_ortodoncia(ortodonciaId)")
        }

        private val MIGRATION_54_55 = object : Migration(54, 55) {
            override fun migrate(db: SupportSQLiteDatabase) {
                crearTablasDentales(db)
            }
        }

        private val MIGRATION_55_56 = object : Migration(55, 56) {
            override fun migrate(db: SupportSQLiteDatabase) {
                crearTablasDentales(db)
            }
        }

        private val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transacciones_dentales ADD COLUMN reciboUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_57_58 = object : Migration(57, 58) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medical_practitioners ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_51_52 = object : Migration(51, 52) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("DROP INDEX IF EXISTS index_registro_hidratacion_patientId") } catch (_: Exception) { }
                try { db.execSQL("DROP INDEX IF EXISTS index_registro_hidratacion_timestamp") } catch (_: Exception) { }
                try { db.execSQL("DROP TABLE IF EXISTS registro_hidratacion") } catch (_: Exception) { }
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS registro_hidratacion (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL DEFAULT 0,
                        cantidadMl INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registro_hidratacion_patientId ON registro_hidratacion(patientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registro_hidratacion_timestamp ON registro_hidratacion(timestamp)")
            }
        }

        private val MIGRATION_50_51 = object : Migration(50, 51) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS registro_hidratacion (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            patientId INTEGER NOT NULL DEFAULT 0,
                            cantidadMl INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL
                        )"""
                    )
                } catch (_: Exception) { }
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_registro_hidratacion_patientId ON registro_hidratacion(patientId)") } catch (_: Exception) { }
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_registro_hidratacion_timestamp ON registro_hidratacion(timestamp)") } catch (_: Exception) { }
            }
        }

        private val MIGRATION_49_50 = object : Migration(49, 50) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS registro_hidratacion (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            patientId INTEGER NOT NULL DEFAULT 0,
                            cantidadMl INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL
                        )"""
                    )
                } catch (_: Exception) { }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_registro_hidratacion_patientId ON registro_hidratacion(patientId)")
                } catch (_: Exception) { }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_registro_hidratacion_timestamp ON registro_hidratacion(timestamp)")
                } catch (_: Exception) { }
            }
        }

        private val MIGRATION_48_49 = object : Migration(48, 49) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_fall_alerts_patientId")
                db.execSQL("DROP INDEX IF EXISTS index_fall_alerts_detectedAt")
                db.execSQL("DROP TABLE IF EXISTS fall_alerts")
                db.execSQL(
                    """CREATE TABLE fall_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        confirmedAt INTEGER,
                        latitude REAL,
                        longitude REAL,
                        impactMagnitude REAL,
                        status TEXT NOT NULL DEFAULT 'DETECTED',
                        notes TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_patientId ON fall_alerts(patientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fall_alerts_detectedAt ON fall_alerts(detectedAt)")
            }
        }

        private val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_intakes ADD COLUMN patientId INTEGER NOT NULL DEFAULT 0")
                try {
                    db.execSQL(
                        "UPDATE medication_intakes SET patientId = " +
                        "(SELECT i.patientId FROM insumos i WHERE i.id = medication_intakes.medicationId) " +
                        "WHERE EXISTS (SELECT 1 FROM insumos i WHERE i.id = medication_intakes.medicationId)"
                    )
                } catch (_: Exception) { }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val oldDbFile = context.getDatabasePath("control_medicamentos_db")
                val newDbFile = context.getDatabasePath("control_insumos_db")
                if (oldDbFile.exists() && !newDbFile.exists()) {
                    oldDbFile.renameTo(newDbFile)
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_insumos_db_v2"
                )
                .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_38_39, MIGRATION_40_41, MIGRATION_41_42, MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45, MIGRATION_45_46, MIGRATION_46_47, MIGRATION_47_48, MIGRATION_48_49, MIGRATION_49_50, MIGRATION_50_51, MIGRATION_51_52, MIGRATION_52_53, MIGRATION_53_54, MIGRATION_54_55, MIGRATION_55_56, MIGRATION_56_57, MIGRATION_57_58)
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
