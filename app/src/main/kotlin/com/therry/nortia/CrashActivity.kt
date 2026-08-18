package com.therry.nortia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.ui.theme.NortiaTheme

/**
 * Se muestra cuando la app crashea (ver [NortiaApplication]), en vez de dejar
 * que el sistema simplemente mate el proceso sin explicación. Temporal, para
 * poder diagnosticar sin acceso a logcat.
 */
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = intent.getStringExtra(EXTRA_ERROR).orEmpty()

        setContent {
            NortiaTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Nortia se cerró por un error",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = message, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { shareError(message) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Compartir error")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }

    private fun shareError(message: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir error"))
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
    }
}
