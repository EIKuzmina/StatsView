package ru.netology.statsview.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.netology.statsview.R

@SuppressLint("SetTextI18n")
class AppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)
        val view = findViewById<StatsView>(R.id.statsView)
        view.data = listOf(
            0.25F,
            0.25F,
            0.25F,
            0.25F
        )
    }
}