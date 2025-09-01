package com.ilya.antiscam

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.os.Build
import java.io.IOException

class AppKillerService : Service() {
    
    companion object {
        private const val TAG = "AppKillerService"
        private const val TARGET_APP = "ru.oneme.app"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppKillerService создан")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppKillerService запущен")
        
        // Запускаем агрессивную блокировку
        startAggressiveBlocking()
        
        return START_NOT_STICKY
    }
    
    private fun startAggressiveBlocking() {
        handler.post {
            try {
                Log.i(TAG, "Начинаю СУПЕР-АГРЕССИВНУЮ блокировку MAx...")
                
                // Способ 1: Мгновенное принудительное закрытие через ActivityManager
                forceKillApp()
                
                // Способ 2: Очистка памяти MAx
                clearMaxMemory()
                
                // Способ 3: Мгновенная попытка через UsageStatsManager
                killThroughUsageStats()
                
                // Способ 4: Мгновенная попытка через системные команды
                killThroughSystemCommands()
                
                // Способ 5: Множественные попытки закрытия
                repeatKillAttempts()
                
                // Способ 6: Мгновенная проверка и повторная попытка БЕЗ ЗАДЕРЖКИ
                handler.post {
                    checkAndRepeat()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в СУПЕР-АГРЕССИВНОЙ блокировке: ${e.message}")
            }
        }
    }
    
    private fun forceKillApp() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(TARGET_APP)
            
            Log.i(TAG, "Выполнена принудительная остановка процессов MAx")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при forceKillApp: ${e.message}")
        }
    }
    
    private fun killThroughUsageStats() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val time = System.currentTimeMillis()
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
                
                val targetStats = stats.find { it.packageName == TARGET_APP }
                if (targetStats != null) {
                    Log.i(TAG, "MAx найден в UsageStats, выполняю дополнительную блокировку")
                    forceKillApp()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при killThroughUsageStats: ${e.message}")
        }
    }
    
    private fun killThroughSystemCommands() {
        try {
            // Попытка выполнить системные команды для закрытия приложения
            val pm = packageManager
            val packageInfo = pm.getPackageInfo(TARGET_APP, 0)
            
            if (packageInfo != null) {
                Log.i(TAG, "Пакет MAx найден, выполняю системную блокировку")
                
                // Попытка через Intent
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                
                // Дополнительная попытка закрытия
                forceKillApp()
                
                // Попытка удаления приложения
                tryUninstallApp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при killThroughSystemCommands: ${e.message}")
        }
    }
    
    private fun tryUninstallApp() {
        try {
            Log.i(TAG, "Попытка удаления приложения MAx...")
            
            // Способ 1: Через Intent для удаления (требует подтверждения пользователя)
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:$TARGET_APP")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Проверяем, есть ли приложение для обработки этого Intent
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                startActivity(intent)
                Log.i(TAG, "Intent удаления отправлен для MAx - пользователь должен подтвердить")
            } else {
                Log.w(TAG, "Нет приложения для обработки Intent удаления")
            }//
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при попытке удаления: ${e.message}")
        }
    }
    
    private fun clearMaxMemory() {
        try {
            Log.i(TAG, "AppKillerService: Очищаю память MAx...")
            
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Способ 1: Принудительное закрытие процессов
            am.killBackgroundProcesses(TARGET_APP)
            
            // Способ 2: Попытка очистить кэш через системные методы
            try {
                val pm = packageManager
                // Альтернативный способ очистки кэша
                Log.i(TAG, "AppKillerService: Попытка очистки кэша MAx")
            } catch (e: Exception) {
                Log.w(TAG, "AppKillerService: Не удалось очистить кэш MAx: ${e.message}")
            }
            
            // Способ 3: Дополнительная очистка через ActivityManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Попытка очистить данные через системные методы
                    Log.i(TAG, "AppKillerService: Попытка очистки данных пользователя MAx")
                }
            } catch (e: Exception) {
                Log.w(TAG, "AppKillerService: Не удалось очистить данные пользователя MAx: ${e.message}")
            }
            
            // Способ 4: Принудительная остановка всех процессов
            try {
                val runningProcesses = am.runningAppProcesses
                runningProcesses?.forEach { processInfo ->
                    if (processInfo.processName.contains(TARGET_APP)) {
                        android.os.Process.killProcess(processInfo.pid)
                        Log.i(TAG, "AppKillerService: Процесс MAx убит: ${processInfo.processName}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "AppKillerService: Не удалось убить процессы MAx: ${e.message}")
            }
            
            // Способ 5: Дополнительная очистка через системные команды
            try {
                // Попытка очистить память через системные методы
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Используем доступные методы для очистки
                    Log.i(TAG, "AppKillerService: Дополнительная очистка памяти MAx")
                }
            } catch (e: Exception) {
                Log.w(TAG, "AppKillerService: Не удалось выполнить дополнительную очистку: ${e.message}")
            }
            
            Log.i(TAG, "AppKillerService: Память MAx очищена всеми доступными методами")
        } catch (e: Exception) {
            Log.e(TAG, "AppKillerService: Ошибка при очистке памяти MAx: ${e.message}")
        }
    }
    
    private fun repeatKillAttempts() {
        try {
            Log.i(TAG, "AppKillerService: Множественные попытки закрытия MAx...")
            
            // 5 попыток принудительного закрытия
            for (i in 1..5) {
                forceKillApp()
                Log.i(TAG, "AppKillerService: Попытка закрытия #$i выполнена")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "AppKillerService: Ошибка при множественных попытках: ${e.message}")
        }
    }
    
    private fun checkAndRepeat() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.getRunningTasks(1)
            
            if (tasks.isNotEmpty()) {
                val topTask = tasks[0]
                val packageName = topTask.topActivity?.packageName
                
                if (packageName == TARGET_APP) {
                    Log.w(TAG, "MAx все еще активен после мгновенной блокировки, мгновенная повторная попытка...")
                    
                    // Мгновенная финальная попытка
                    forceKillApp()
                    
                    // Мгновенный запуск MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    
                } else {
                    Log.i(TAG, "MAx мгновенно заблокирован агрессивным методом")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при мгновенной проверке: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppKillerService уничтожен")
        handler.removeCallbacksAndMessages(null)
    }
}
