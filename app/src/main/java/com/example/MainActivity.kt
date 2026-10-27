package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.TodoRepository
import com.example.data.local.PreferencesManager
import com.example.data.local.TodoDatabase
import com.example.ui.TodoViewModel
import com.example.ui.TodoViewModelFactory
import com.example.ui.UltimateToDoApp
import com.example.util.PlatformHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TodoDatabase.getInstance(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)
        val repository = TodoRepository(database.todoDao(), preferencesManager)
        val factory = TodoViewModelFactory(
            repository = repository,
            onSoundEffect = { soundEnabled -> PlatformHelper.playBeepIfEnabled(soundEnabled) }
        )
        val viewModel = ViewModelProvider(this, factory)[TodoViewModel::class.java]

        setContent {
            UltimateToDoApp(viewModel = viewModel)
        }
    }
}
