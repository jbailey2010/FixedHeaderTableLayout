package com.github.zardozz.fixedheadertablelayoutsample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_basic).setOnClickListener {
            startActivity(Intent(this, BasicTableActivity::class.java))
        }
        findViewById<Button>(R.id.btn_large).setOnClickListener {
            startActivity(Intent(this, LargeTableActivity::class.java))
        }
        findViewById<Button>(R.id.btn_multi).setOnClickListener {
            startActivity(Intent(this, MultiTableActivity::class.java))
        }
    }
}
