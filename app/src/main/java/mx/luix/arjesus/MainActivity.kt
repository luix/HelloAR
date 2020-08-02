package mx.luix.arjesus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.filament.utils.Utils

//import com.google.ar.sceneform

class MainActivity : AppCompatActivity() {

    companion object {
        init { Utils.init() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}