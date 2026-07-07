package com.therry.nortia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.therry.nortia.screens.AgendaScreen
import com.therry.nortia.ui.theme.NortiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NortiaTheme {
                AgendaScreen()
            }
        }
    }
}
