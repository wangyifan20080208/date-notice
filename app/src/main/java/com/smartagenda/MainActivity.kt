package com.smartagenda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smartagenda.ui.navigation.AgendaNavHost
import com.smartagenda.ui.theme.SmartAgendaTheme

class MainActivity : ComponentActivity() {

    private var pendingTaskId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingTaskId = intent.extractTaskId()

        setContent {
            SmartAgendaTheme {
                AgendaNavHost(
                    container = (application as AgendaApp).container,
                    pendingTaskId = pendingTaskId,
                    onPendingTaskConsumed = { pendingTaskId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.extractTaskId()?.let { pendingTaskId = it }
    }

    private fun Intent.extractTaskId(): Long? {
        val id = getLongExtra(EXTRA_TASK_ID, -1L)
        return id.takeIf { it > 0L }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}