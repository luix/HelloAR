package mx.luix.arjesus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import com.google.android.filament.Skybox
import com.google.android.filament.utils.*
import java.nio.ByteBuffer

//import com.google.ar.sceneform

/**
 * Getting Started with Filament on Android
 *
 * Filament is Google’s open source physically-based renderer. It’s great for when you need to add
 * 3D capabilities to your application without the overhead of an entire game engine.
 *
 * @author Philip Rideout
 *
 * @link   https://medium.com/@philiprideout/getting-started-with-filament-on-android-d10b16f0ec67
 */
class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            // Log.d("Choreographer Animator", "seconds: $seconds")
            choreographer.postFrameCallback(this)
            modelViewer.animator?.apply {
                // Log.d("Choreographer Animator", "animation count: $animationCount")
                if (animationCount > 0) {
                    applyAnimation(0, seconds.toFloat())
                    // CINEMA_4D_Basis
                    // Log.d("Choreographer Animator", "animation name: ${getAnimationName(0)}")
                    // Log.d("Choreographer Animator", "animation duration: ${getAnimationDuration(0)}")
                }
                updateBoneMatrices()
            }
            modelViewer.render(currentTime)

            // Reset the root transform, then rotate it around the Z axis.
            modelViewer.asset?.apply {
                modelViewer.transformToUnitCube()
                val rootTransform = this.root.getTransform()
                val degrees = 20f * seconds.toFloat()
                val zAxis = Float3(0f, 0f, 1f)
                this.root.setTransform(rootTransform * rotation(zAxis, degrees))
            }
        }
    }

    companion object {
        init { Utils.init() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        surfaceView = SurfaceView(this).apply { setContentView(this) }
        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)
        //loadGlb("DamagedHelmet")
        loadGltf("BusterDrone")
        loadEnvironment("venetian_crossroads_2k")
        modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)

        // Hide the floor disk and disable the emissive tail lights on the back of the drone.
        val asset = modelViewer.asset!!
        val rm = modelViewer.engine.renderableManager
        for (entity in asset.entities) {
            val renderable = rm.getInstance(entity)
            if (renderable == 0) {
                continue
            }
            if (asset.getName(entity) == "Scheibe_Boden_0") {
                rm.setLayerMask(renderable, 0xff, 0x00)
            }
            val material = rm.getMaterialInstanceAt(renderable, 0)
            material.setParameter("emissiveFactor", 0f, 0f, 0f)
        }
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.postFrameCallback(frameCallback)
    }

    private fun loadGlb(name: String) {
        val buffer = readAsset("models/${name}.glb")
        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    private fun loadGltf(name: String) {
        val buffer = readAsset("models/${name}.gltf")
        modelViewer.loadModelGltf(buffer) { uri -> readAsset("models/$uri") }
        modelViewer.transformToUnitCube()
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KtxLoader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KtxLoader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }

    private fun Int.getTransform(): Mat4 {
        val tm = modelViewer.engine.transformManager
        return Mat4.of(*tm.getTransform(tm.getInstance(this), null))
    }

    private fun Int.setTransform(mat: Mat4) {
        val tm = modelViewer.engine.transformManager
        tm.setTransform(tm.getInstance(this), mat.toFloatArray())
    }
}