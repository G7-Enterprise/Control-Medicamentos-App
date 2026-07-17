package com.carlos.controlmedicamentos.data.local

object ProtocoloVacunacion {
    fun generarEsquemaBase(ninoId: Long): List<VacunaEntity> {
        return listOf(
            // RECIÉN NACIDO
            VacunaEntity(ninoId = ninoId, nombre = "BCG", descripcion = "Previene la tuberculosis miliar y meníngea.", edadRecomendada = "Recién Nacido"),
            VacunaEntity(ninoId = ninoId, nombre = "Hepatitis B (Dosis RN)", descripcion = "Previene la transmisión vertical del virus de la Hepatitis B.", edadRecomendada = "Recién Nacido"),

            // 2 MESES
            VacunaEntity(ninoId = ninoId, nombre = "Pentavalente (1ª Dosis)", descripcion = "Difteria, Tétanos, Tos Ferina, Hep B, Haemophilus influenzae b.", edadRecomendada = "2 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Polio Inactiva - IPV (1ª Dosis)", descripcion = "Previene la poliomielitis y parálisis asociada.", edadRecomendada = "2 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Rotavirus (1ª Dosis)", descripcion = "Previene formas graves de diarrea deshidratante.", edadRecomendada = "2 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Neumocócica Conjugada (1ª Dosis)", descripcion = "Previene neumonías, meningitis y otitis por neumococo.", edadRecomendada = "2 meses"),

            // 4 MESES
            VacunaEntity(ninoId = ninoId, nombre = "Pentavalente (2ª Dosis)", descripcion = "Segunda dosis del esquema de protección bacteriana.", edadRecomendada = "4 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Polio Inactiva - IPV (2ª Dosis)", descripcion = "Refuerzo de protección contra poliomielitis.", edadRecomendada = "4 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Rotavirus (2ª Dosis)", descripcion = "Segunda dosis protectora contra el rotavirus.", edadRecomendada = "4 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Neumocócica Conjugada (2ª Dosis)", descripcion = "Segunda dosis protectora contra infecciones neumocócicas.", edadRecomendada = "4 meses"),

            // 6 MESES
            VacunaEntity(ninoId = ninoId, nombre = "Pentavalente (3ª Dosis)", descripcion = "Completa el esquema básico de protección de cinco enfermedades.", edadRecomendada = "6 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Polio Oral - OPV (3ª Dosis)", descripcion = "Inmunización comunitaria activa contra poliomielitis.", edadRecomendada = "6 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Influenza Estacional (1ª Dosis)", descripcion = "Protección anual contra cepas corrientes de influenza.", edadRecomendada = "6 meses"),

            // 12 MESES (1 AÑO)
            VacunaEntity(ninoId = ninoId, nombre = "SPR / Triple Viral (1ª Dosis)", descripcion = "Previene Sarampión, Parotiditis (Paperas) y Rubéola.", edadRecomendada = "12 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Neumocócica Conjugada (Refuerzo)", descripcion = "Consolida la inmunidad contra neumococos a largo plazo.", edadRecomendada = "12 meses"),

            // 18 MESES (1 AÑO Y MEDIO)
            VacunaEntity(ninoId = ninoId, nombre = "DPT (1er Refuerzo)", descripcion = "Triple bacteriana: Difteria, Tétanos y Tos Ferina.", edadRecomendada = "18 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "Polio Oral - OPV (1er Refuerzo)", descripcion = "Primer refuerzo general contra la poliomielitis.", edadRecomendada = "18 meses"),
            VacunaEntity(ninoId = ninoId, nombre = "SPR / Triple Viral (2ª Dosis)", descripcion = "Cierre de inmunidad contra Sarampión, Paperas y Rubéola.", edadRecomendada = "18 meses"),

            // 4 A 6 AÑOS (REFUERZO ESCOLAR)
            VacunaEntity(ninoId = ninoId, nombre = "DPT (2º Refuerzo)", descripcion = "Refuerzo obligatorio antes del ingreso a la etapa escolar.", edadRecomendada = "4-6 años"),
            VacunaEntity(ninoId = ninoId, nombre = "Polio Oral - OPV (2º Refuerzo)", descripcion = "Último refuerzo sistemático contra la poliomielitis.", edadRecomendada = "4-6 años"),

            // 11 A 12 AÑOS (PRE-ADOLESCENCIA)
            VacunaEntity(ninoId = ninoId, nombre = "VPH (Virus del Papiloma Humano)", descripcion = "Previene el cáncer de cuello uterino y lesiones genitales.", edadRecomendada = "11-12 años"),
            VacunaEntity(ninoId = ninoId, nombre = "dTpa o Td (Refuerzo)", descripcion = "Refuerzo definitivo contra Tétanos y Difteria tipo adulto.", edadRecomendada = "11-12 años")
        )
    }
}
