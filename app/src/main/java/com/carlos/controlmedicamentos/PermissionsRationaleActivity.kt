package com.carlos.controlmedicamentos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carlos.controlmedicamentos.ui.theme.ControlMedicamentosTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControlMedicamentosTheme {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Permisos de Health Connect", style = MaterialTheme.typography.headlineSmall)
                        Text("Esta app solo lee el historial de ejercicio para mostrarlo dentro de tu registro personal.")
                        Text("No modifica tus datos de Samsung Health ni escribe informacion nueva en Health Connect.")
                        Text("Si Samsung Health no esta sincronizando con Health Connect, esta pantalla no mostrara ejercicios aunque esten en Samsung Health.")
                        Button(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }
}