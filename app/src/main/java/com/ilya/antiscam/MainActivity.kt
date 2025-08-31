package com.ilya.antiscam

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilya.antiscam.ui.theme.AntiScamTheme
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AntiScamTheme {

                        AntiScamScreen(
                            onStopService = { stopService() },
                            onStartAccessibility = { startAccessibilitySettings() },
                            onUninstallMax = { uninstallMaxApp() }
                        )

            }
        }

        // Запуск Foreground Service
        startAppBlockService()
    }

    private fun startAppBlockService() {
        Intent(this, AppBlockService::class.java).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            } else {
                startService(it)
            }
        }
    }

    private fun stopService() {
        Intent(this, AppBlockService::class.java).also {
            stopService(it)
        }
    }

    private fun startAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    private fun uninstallMaxApp() {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:ru.oneme.app")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Проверяем, есть ли приложение для обработки этого Intent
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                startActivity(intent)
                Log.i("MainActivity", "Intent удаления отправлен для MAx")
            } else {
                Log.w("MainActivity", "Нет приложения для обработки Intent удаления")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при попытке удаления: ${e.message}")
        }
    }
}


@Composable
fun AntiScamScreen(
    onStopService: () -> Unit,
    onStartAccessibility: () -> Unit,
    onUninstallMax: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState), // делаем колонку скроллируемой
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Заголовок //
            Text(
                text = "AntiScam",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Карточки
            ServiceStatusCard()
            BlockingInfoCard()
            InstructionsCard()

            // Кнопки управления
            ServiceControlButtons(
                onStopService = onStopService,
                onStartAccessibility = onStartAccessibility,
                onUninstallMax = onUninstallMax
            )
        }
    }
}
@Composable
fun ServiceStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛡️ Сервис активен",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Мониторинг приложения MAx запущен",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Foreground Service + Accessibility Service",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BlockingInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🎯 Цель блокировки",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Приложение: MAx",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Text(
                text = "Пакет: ru.oneme.app",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Действие: Автоматическое закрытие при обнаружении",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "ℹ️ Как это работает",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            // just
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "• Foreground Service: мониторинг каждые 500мс\n" +
                       "• Accessibility Service: мгновенное обнаружение\n" +
                       "• При обнаружении MAx: запуск MainActivity + возврат на главный\n" +
                       "• Двойная защита для максимальной эффективности",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ServiceControlButtons(onStopService: () -> Unit, onStartAccessibility: () -> Unit, onUninstallMax: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartAccessibility,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🔧 Настроить Accessibility Service",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Button(
            onClick = onUninstallMax,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🗑️ Удалить MAx с устройства",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Button(
            onClick = onStopService,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🛑 Остановить Foreground Service",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = "Для максимальной эффективности включите Accessibility Service в настройках",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AntiScamScreenPreview() {
    AntiScamTheme {
        AntiScamScreen(onStopService = {}, onStartAccessibility = {}, onUninstallMax = {})
    }
}

