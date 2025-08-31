package com.ilya.antiscam

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AppAccessibilityService"
        private const val TARGET_APP = "ru.oneme.app"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var isBlocking = false
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService подключен")
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.packageNames = arrayOf(TARGET_APP)
        
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = it.packageName?.toString()
                    Log.d(TAG, "Обнаружено изменение окна: $packageName")
                    
                    if (packageName == TARGET_APP && !isBlocking) {
                        Log.w(TAG, "Обнаружено приложение MAx! Блокирую...")
                        isBlocking = true
                        blockTargetApp()
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    val packageName = it.packageName?.toString()
                    if (packageName == TARGET_APP && !isBlocking) {
                        Log.w(TAG, "Обнаружено изменение содержимого MAx! Блокирую...")
                        isBlocking = true
                        blockTargetApp()
                    }
                }
            }
        }
    }
    
    private fun blockTargetApp() {
        try {
            Log.i(TAG, "Начинаю мгновенную блокировку приложения MAx...")
            
            // Способ 1: Мгновенное закрытие через системные команды
            forceCloseTargetApp()
            
            // Способ 2: Мгновенный запуск AppKillerService для агрессивной блокировки
            val killerIntent = Intent(this, AppKillerService::class.java)
            startService(killerIntent)
            
            // Способ 3: Мгновенный запуск MainActivity поверх MAx
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            
            Log.i(TAG, "MainActivity мгновенно запущена поверх MAx")
            
            // Способ 4: Мгновенный возврат на главный экран
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            
            // Способ 5: Мгновенное действие "Назад" (несколько раз для надежности)
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.post {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            
            // Способ 6: Мгновенная проверка и повторная блокировка БЕЗ ЗАДЕРЖКИ
            handler.post {
                val currentApp = getCurrentApp()
                if (currentApp == TARGET_APP) {
                    Log.w(TAG, "MAx все еще активен, мгновенная повторная блокировка...")
                    repeatBlocking()
                } else {
                    isBlocking = false
                    Log.i(TAG, "MAx мгновенно заблокирован")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при мгновенной блокировке: ${e.message}")
            isBlocking = false
        }
    }
    
    private fun forceCloseTargetApp() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(TARGET_APP)
            
            Log.i(TAG, "Выполнена принудительная остановка процессов MAx")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при принудительном закрытии: ${e.message}")
        }
    }
    
    private fun repeatBlocking() {
        try {
            // Мгновенная повторная попытка закрытия
            forceCloseTargetApp()
            
            // Мгновенная попытка найти и нажать кнопку "Закрыть" или "Назад"
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                findAndClickCloseButton(rootNode)
                rootNode.recycle()
            }
            
            // Мгновенный возврат на главный экран
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            
            // Мгновенная проверка БЕЗ ЗАДЕРЖКИ
            handler.post {
                val currentApp = getCurrentApp()
                if (currentApp == TARGET_APP) {
                    Log.e(TAG, "MAx не удалось заблокировать мгновенно, финальная попытка...")
                    // Финальная мгновенная попытка
                    forceCloseTargetApp()
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                isBlocking = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при мгновенной повторной блокировке: ${e.message}")
            isBlocking = false
        }
    }
    
    private fun findAndClickCloseButton(rootNode: AccessibilityNodeInfo) {
        try {
            // Поиск кнопок закрытия по тексту
            val closeButtons = rootNode.findAccessibilityNodeInfosByText("Закрыть")
            closeButtons.addAll(rootNode.findAccessibilityNodeInfosByText("Close"))
            closeButtons.addAll(rootNode.findAccessibilityNodeInfosByText("Отмена"))
            closeButtons.addAll(rootNode.findAccessibilityNodeInfosByText("Cancel"))
            
            for (button in closeButtons) {
                if (button.isClickable) {
                    Log.i(TAG, "Найдена кнопка закрытия, нажимаю...")
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    break
                }
            }
            
            // Поиск кнопки "Назад"
            val backButtons = rootNode.findAccessibilityNodeInfosByText("Назад")
            backButtons.addAll(rootNode.findAccessibilityNodeInfosByText("Back"))
            
            for (button in backButtons) {
                if (button.isClickable) {
                    Log.i(TAG, "Найдена кнопка назад, нажимаю...")
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    break
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске кнопок закрытия: ${e.message}")
        }
    }
    
    private fun getCurrentApp(): String? {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                val topTask = tasks[0]
                topTask.topActivity?.packageName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении текущего приложения: ${e.message}")
            null
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService прерван")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccessibilityService уничтожен")
        isBlocking = false
    }
}
