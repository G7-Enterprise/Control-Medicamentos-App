package com.carlos.controlmedicamentos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modelo de datos
 data class Vacuna(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val edadRecomendada: String,
    var estaAplicada: Boolean = false,
    var fechaAplicacion: String? = null
)

data class Nino(
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String,
    val pesoUltimo: String = "",
    val tallaUltima: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardNinoScreen(
    nino: Nino,
    onNavigateToVacunas: () -> Unit,
    onNavigateToControles: () -> Unit,
    onNavigateToEnfermedades: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de ${nino.nombre}", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1A3C))
            )
        },
        containerColor = Color(0xFF120F26)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tarjeta de Resumen
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2450)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Datos Generales", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nacimiento: ${nino.fechaNacimiento}", color = Color.LightGray)
                    Text("Sexo: ${nino.sexo}", color = Color.LightGray)
                    if (nino.pesoUltimo.isNotBlank() || nino.tallaUltima.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Peso: ${nino.pesoUltimo}", color = Color(0xFFE1BEE7), fontSize = 14.sp)
                        Text("Talla: ${nino.tallaUltima}", color = Color(0xFFE1BEE7), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Registro personal infantil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Grid de Opciones
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                item {
                    ModuloCard(titulo = "Vacunas", icono = Icons.Default.Vaccines, onClick = onNavigateToVacunas)
                }
                item {
                    ModuloCard(titulo = "Controles", icono = Icons.Default.MonitorWeight, onClick = onNavigateToControles)
                }
                item {
                    ModuloCard(titulo = "Afecciones", icono = Icons.Default.Healing, onClick = onNavigateToEnfermedades)
                }
            }
        }
    }
}

@Composable
fun ModuloCard(titulo: String, icono: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF382F66)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icono, contentDescription = titulo, tint = Color(0xFFE91E63), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(titulo, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
