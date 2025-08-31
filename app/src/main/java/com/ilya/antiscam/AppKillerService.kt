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
        handler.postDelayed({
            try {
                Log.i(TAG, "Начинаю агрессивную блокировку MAx...")
                
                // Способ 1: Принудительное закрытие через ActivityManager
                forceKillApp()
                
                // Способ 2: Попытка через UsageStatsManager
                killThroughUsageStats()
                
                // Способ 3: Попытка через системные команды
                killThroughSystemCommands()
                
                // Способ 4: Проверка и повторная попытка
                handler.postDelayed({
                    checkAndRepeat()
                }, 300)
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в агрессивной блокировке: ${e.message}")
            }
        }, 100)
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при killThroughSystemCommands: ${e.message}")
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
                    Log.w(TAG, "MAx все еще активен после агрессивной блокировки, повторная попытка...")
                    
                    // Финальная попытка
                    forceKillApp()
                    
                    // Запуск MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    
                } else {
                    Log.i(TAG, "MAx успешно заблокирован агрессивным методом")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при checkAndRepeat: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppKillerService уничтожен")
        handler.removeCallbacksAndMessages(null)
    }
}
