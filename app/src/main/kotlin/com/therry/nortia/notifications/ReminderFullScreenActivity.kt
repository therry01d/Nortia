package com.therry.nortia.notifications

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.therry.nortia.R
import com.therry.nortia.data.Category
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.data.Repeat
import com.therry.nortia.ui.theme.NortiaTheme
import com.therry.nortia.util.DateTimeUtils

/**
 * Se lanza vía fullScreenIntent: debe despertar la pantalla y mostrarse
 * sobre el lock screen, igual que una alarma.
 */
class ReminderFullScreenActivity : ComponentActivity() {

    private var current by mutableStateOf<Item?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreenAndWake()

        current = itemFromIntent(intent)

        setContent {
            NortiaTheme {
                current?.let { item ->
                    ReminderScreen(
                        item = item,
                        onDismiss = { dismiss(item) },
                        onSnooze = { snooze(item) }
                    )
                }
            }
        }
    }

    /**
     * Un segundo recordatorio que dispara mientras esta Activity (singleInstance)
     * sigue visible llega por acá, NO por onCreate. Sin esto se mostraría el
     * recordatorio viejo con datos del primero.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showOverLockScreenAndWake()
        current = itemFromIntent(intent)
    }

    /**
     * Reconstruye el Item desde los extras leyendo el MISMO conjunto que
     * ReminderReceiver.itemFromIntent: incluir repeat/date/remindBefore es lo
     * que mantiene viva la recurrencia al posponer desde la pantalla completa.
     */
    private fun itemFromIntent(intent: Intent): Item {
        val id = intent.getIntExtra(NotificationScheduler.EXTRA_ITEM_ID, -1)
        val type = runCatching {
            ItemType.valueOf(
                intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TYPE) ?: ItemType.RECORDATORIO.name
            )
        }.getOrDefault(ItemType.RECORDATORIO)
        val repeat = runCatching {
            Repeat.valueOf(
                intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_REPEAT) ?: Repeat.NINGUNO.name
            )
        }.getOrDefault(Repeat.NINGUNO)
        val date = if (intent.hasExtra(NotificationScheduler.EXTRA_ITEM_DATE)) {
            intent.getLongExtra(NotificationScheduler.EXTRA_ITEM_DATE, DateTimeUtils.today())
        } else {
            DateTimeUtils.today()
        }
        return Item(
            id = id,
            type = type,
            title = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TITLE).orEmpty(),
            date = date,
            time = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TIME),
            category = Category.PERSONAL,
            priority = null,
            note = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_NOTE).orEmpty(),
            remind = true,
            remindBeforeMinutes = intent.getIntExtra(NotificationScheduler.EXTRA_ITEM_REMIND_BEFORE, 10),
            repeat = repeat
        )
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

    private fun dismiss(item: Item) {
        if (item.id != -1) {
            NotificationManagerCompat.from(this).cancel(item.id)
        }
        finish()
    }

    private fun snooze(item: Item) {
        if (item.id != -1) {
            NotificationManagerCompat.from(this).cancel(item.id)
        }
        NotificationScheduler.schedule(
            this,
            item,
            triggerAtMillisOverride = System.currentTimeMillis() + NotificationScheduler.SNOOZE_MILLIS
        )
        finish()
    }
}

private fun typeLabel(type: ItemType): String = when (type) {
    ItemType.EVENTO -> "Evento"
    ItemType.TAREA -> "Tarea"
    ItemType.RECORDATORIO -> "Recordatorio"
}

@Composable
private fun ReminderScreen(
    item: Item,
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
                text = typeLabel(item.type),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            if (item.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            if (!item.time.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.titleLarge
                )
            }
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
