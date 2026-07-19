package com.therry.nortia.notifications

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.ui.theme.NortiaTheme

/**
 * Se lanza vía fullScreenIntent: debe despertar la pantalla y mostrarse
 * sobre el lock screen, igual que una alarma.
 */
class ReminderFullScreenActivity : ComponentActivity() {

    private var eventId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreenAndWake()

        eventId = intent.getIntExtra(NotificationScheduler.EXTRA_EVENT_ID, -1)
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_TITLE).orEmpty()
        val description = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_DESCRIPTION).orEmpty()
        val time = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_TIME).orEmpty()
        val event = Event(id = eventId, title = title, description = description, date = 0L, time = time)

        setContent {
            NortiaTheme {
                ReminderScreen(
                    event = event,
                    onDismiss = { dismiss() },
                    onSnooze = { snooze(event) }
                )
            }
        }
    }

    private fun showOverLockScreenAndWake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun dismiss() {
        if (eventId != -1) {
            NotificationManagerCompat.from(this).cancel(eventId)
        }
        finish()
    }

    private fun snooze(event: Event) {
        if (eventId != -1) {
            NotificationManagerCompat.from(this).cancel(eventId)
        }
        NotificationScheduler.schedule(
            this,
            event,
            triggerAtMillisOverride = System.currentTimeMillis() + 10 * 60 * 1000L
        )
        finish()
    }
}

@Composable
private fun ReminderScreen(
    event: Event,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.notification_title),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.time,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reminder_activity_dismiss))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reminder_activity_snooze))
            }
        }
    }
}
