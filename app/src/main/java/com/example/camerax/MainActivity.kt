package com.example.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.camerax_fragment.CameraFragment
import com.example.camerax_fragment.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction()
                .add(R.id.flFragmentContainer, CameraFragment(), "")
                .commitAllowingStateLoss()
    }
}
