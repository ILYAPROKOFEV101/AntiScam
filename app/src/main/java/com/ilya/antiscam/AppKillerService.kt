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
                Log.i(TAG, "Начинаю мгновенную агрессивную блокировку MAx...")
                
                // Способ 1: Мгновенное принудительное закрытие через ActivityManager
                forceKillApp()
                
                // Способ 2: Мгновенная попытка через UsageStatsManager
                killThroughUsageStats()
                
                // Способ 3: Мгновенная попытка через системные команды
                killThroughSystemCommands()
                
                // Способ 4: Мгновенная проверка и повторная попытка БЕЗ ЗАДЕРЖКИ
                handler.post {
                    checkAndRepeat()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в мгновенной агрессивной блокировке: ${e.message}")
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
