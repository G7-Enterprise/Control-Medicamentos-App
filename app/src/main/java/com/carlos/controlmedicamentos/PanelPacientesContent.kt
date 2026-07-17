package com.carlos.controlmedicamentos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.controlmedicamentos.data.local.AppDatabase
import com.carlos.controlmedicamentos.data.local.PatientProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun PanelPacientesContent(
    mostrarPanelPacientes: Boolean,
    perfilesPacientes: List<PatientProfile>,
    pacienteActivo: PatientProfile?,
    perfilPendienteDeEliminarState: MutableState<PatientProfile?>,
    database: AppDatabase,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onCerrarPanelesSecundarios: () -> Unit,
    onAbrirNuevaFichaPaciente: () -> Unit,
    onAbrirFichaPaciente: (PatientProfile, Boolean) -> Unit,
    onMostrarFormularioInformeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var perfilPendienteDeEliminar by perfilPendienteDeEliminarState

    if (!mostrarPanelPacientes) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xB8143468),
            contentColor = Color(0xFFF3FBFF)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Usuarios",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            if (perfilesPacientes.isEmpty()) {
                Text("Todavía no hay usuarios registrados.")
            } else {
                Text("Perfil: ${pacienteActivo?.nombre.orEmpty()} ${pacienteActivo?.apellidos.orEmpty()}")
                Text("Edad: ${pacienteActivo?.edad.orEmpty()} | Peso: ${pacienteActivo?.peso.orEmpty()} ${pacienteActivo?.pesoUnidad.orEmpty()} | Estatura: ${pacienteActivo?.estatura.orEmpty()} ${pacienteActivo?.estaturaUnidad.orEmpty()}")
                Text("Condiciones: ${pacienteActivo?.enfermedades.orEmpty()}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onCerrarPanelesSecundarios()
                            onAbrirNuevaFichaPaciente()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nuevo perfil")
                    }
                    Button(
                        onClick = {
                            if (pacienteActivo == null) {
                                Toast.makeText(context, "Selecciona primero un perfil activo", Toast.LENGTH_SHORT).show()
                            } else {
                                perfilPendienteDeEliminar = pacienteActivo
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2430))
                    ) {
                        Text("Eliminar perfil")
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(perfilesPacientes, key = { it.id }) { paciente ->
                        Card(
                            modifier = Modifier.width(260.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (paciente.isActive) Color(0xCC1E61B5) else Color(0xB4142D59),
                                contentColor = Color(0xFFF3FBFF)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    Text("${paciente.nombre} ${paciente.apellidos}")
                                }
                                Text("Edad: ${paciente.edad} | Peso: ${paciente.peso} ${paciente.pesoUnidad}")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                database.patientProfileDao().desactivarTodos()
                                                database.patientProfileDao().activarPaciente(paciente.id)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (paciente.isActive) "Activo" else "Seleccionar")
                                    }
                                    Button(
                                        onClick = {
                                            onAbrirFichaPaciente(paciente, true)
                                            onMostrarFormularioInformeChange(false)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Editar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onCerrarPanelesSecundarios() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al escritorio", color = Color.Black)
            }
        }
    }
}
