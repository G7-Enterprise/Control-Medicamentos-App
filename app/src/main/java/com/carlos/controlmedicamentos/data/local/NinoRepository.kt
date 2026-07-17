package com.carlos.controlmedicamentos.data.local

import kotlinx.coroutines.flow.Flow

class NinoRepository(
    private val ninoDao: NinoDao,
    private val vacunaDao: VacunaDao,
    private val controlPediatricoDao: ControlPediatricoDao,
    private val enfermedadDao: EnfermedadDao
) {
    fun getNinosByPatient(patientId: Int): Flow<List<NinoEntity>> = ninoDao.getNinosByPatient(patientId)

    suspend fun getNinoById(id: Long): NinoEntity? = ninoDao.getNinoById(id)

    suspend fun insertNino(nino: NinoEntity): Long = ninoDao.insertNino(nino)

    suspend fun insertNinoConVacunas(nino: NinoEntity) {
        val ninoId = ninoDao.insertNino(nino)
        // TODO: Generar esquema de vacunación base cuando se implemente ProtocoloVacunacion
        // Por ahora se inserta el niño sin vacunas predefinidas
    }

    fun getVacunasByNino(ninoId: Long): Flow<List<VacunaEntity>> = vacunaDao.getVacunasByNino(ninoId)

    suspend fun updateVacuna(vacuna: VacunaEntity) = vacunaDao.updateVacuna(vacuna)

    suspend fun insertControl(control: ControlPediatricoEntity) = controlPediatricoDao.insertControl(control)
    fun getControlesByNino(ninoId: Long): Flow<List<ControlPediatricoEntity>> = controlPediatricoDao.getControlesByNino(ninoId)

    suspend fun insertEnfermedad(enfermedad: EnfermedadEntity) = enfermedadDao.insertEnfermedad(enfermedad)
    fun getEnfermedadesByNino(ninoId: Long): Flow<List<EnfermedadEntity>> = enfermedadDao.getEnfermedadesByNino(ninoId)
    suspend fun updateEnfermedad(enfermedad: EnfermedadEntity) = enfermedadDao.updateEnfermedad(enfermedad)

    suspend fun deleteNino(nino: NinoEntity) = ninoDao.deleteNino(nino)
}
