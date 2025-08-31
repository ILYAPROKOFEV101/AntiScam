package com.ilya.antiscam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity

class AppBlockService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val targetApp = "ru.oneme.app" // Пакет приложения MAx
    private val TAG = "AppBlockService"
    
    companion object {
        private const val CHECK_INTERVAL = 500L // Проверка каждые 500мс для более быстрой реакции
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Сервис запущен")
        startForeground(1, createNotification())
        handler.post(checkRunnable)
    }

    private fun createNotification(): Notification {
        val channelId = "anti_scam_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AntiScam Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Мониторинг и блокировка приложения MAx"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AntiScam активен")
            .setContentText("Мониторинг приложения MAx")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                val topApp = getTopApp()
                Log.d(TAG, "Текущее активное приложение: $topApp")
                
                if (topApp == targetApp) {
                    Log.w(TAG, "Foreground Service: Обнаружено приложение MAx! Блокирую...")
                    blockTargetApp()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке: ${e.message}")
            }
            
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }

    private fun getTopApp(): String? {
        return try {
            // Способ 1: Через ActivityManager (более надежно)
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                val topTask = tasks[0]
                val packageName = topTask.topActivity?.packageName
                Log.d(TAG, "Top activity: ${topTask.topActivity}, Package: $packageName")
                packageName
            } else {
                // Способ 2: Через getRecentTasks (альтернатива)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val recentTasks = am.getRecentTasks(1, ActivityManager.RECENT_WITH_EXCLUDED)
                    if (recentTasks.isNotEmpty()) {
                        val recentTask = recentTasks[0]
                        val packageName = recentTask.baseIntent?.component?.packageName
                        Log.d(TAG, "Recent task package: $packageName")
                        packageName
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении активного приложения: ${e.message}")
            null
        }
    }

    private fun blockTargetApp() {
        try {
            Log.i(TAG, "Foreground Service: Начинаю блокировку приложения MAx...")
            
            // Способ 1: Запуск нашего приложения поверх MAx
            val blockIntent = Intent(this@AppBlockService, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(blockIntent)
            
            Log.i(TAG, "MainActivity запущена поверх MAx через Foreground Service")
            
            // Способ 2: Попытка закрыть MAx через системные команды
            closeTargetApp()
            
            // Способ 3: Дополнительная задержка и повторная проверка
            handler.postDelayed({
                val currentApp = getTopApp()
                if (currentApp == targetApp) {
                    Log.w(TAG, "Foreground Service: MAx все еще активен, повторная блокировка...")
                    forceCloseTargetApp()
                    
                    // Еще одна попытка через 2 секунды
                    handler.postDelayed({
                        val finalCheck = getTopApp()
                        if (finalCheck == targetApp) {
                            Log.e(TAG, "Foreground Service: MAx не удалось заблокировать окончательно")
                        }
                    }, 200)
                }
            }, 100)
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при блокировке: ${e.message}")
        }
    }

    private fun closeTargetApp() {
        try {
            // Попытка вернуться на главный экран
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            
            Log.i(TAG, "Выполнена команда возврата на главный экран")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при закрытии приложения: ${e.message}")
        }
    }

    private fun forceCloseTargetApp() {
        try {
            // Попытка принудительно закрыть приложение
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(targetApp)
            
            Log.i(TAG, "Выполнена принудительная остановка процессов MAx")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при принудительном закрытии: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Сервис остановлен")
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
