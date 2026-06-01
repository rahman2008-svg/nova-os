package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.LauncherRepository
import com.example.ui.LauncherViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.screens.NovaHomeLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local Room Database & Repositories
        val database = AppDatabase.getDatabase(this)
        val dao = database.launcherDao()
        val repository = LauncherRepository(dao)
        val factory = ViewModelFactory(repository)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Main architecture tying viewmodel structure together
                val viewModel: LauncherViewModel = viewModel(factory = factory)
                
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NovaHomeLayout(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
