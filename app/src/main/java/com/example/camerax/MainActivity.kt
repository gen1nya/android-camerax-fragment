package com.example.camerax

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.camerax_fragment.CameraFragment
import com.example.camerax_fragment.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 31
    }

    private var cameraFragment: CameraFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
        }

        if (cameraFragment == null) {
            cameraFragment =
                (supportFragmentManager.findFragmentByTag(CameraFragment::class.java.simpleName) as? CameraFragment)
                    ?: CameraFragment()
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.flFragmentContainer, cameraFragment!!, CameraFragment::class.java.simpleName)
            .commitAllowingStateLoss()

        swFlash.setOnCheckedChangeListener { _, isChecked ->
            cameraFragment?.flash = isChecked
        }

        sbZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                cameraFragment?.zoom = value
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        btTakePhoto.setOnClickListener {
            cameraFragment?.takePicture()
        }

        cameraFragment?.setCameraListener(object : CameraFragment.CameraListener {
            override fun onImageTaken(image: ByteArray) {
                if (READ_EXTERNAL_STORAGE.isPermissionGranted() && WRITE_EXTERNAL_STORAGE.isPermissionGranted()) {
                    val dir =  File(
                        Environment.getExternalStorageDirectory(),
                        "camerax images"
                    )
                    if (!dir.exists()) {
                        dir.mkdir()
                    }
                    File(dir, System.currentTimeMillis().toString() + ".jpg").apply {
                        if (!exists()) {
                            createNewFile()
                        }
                        writeBytes(image)
                    }
                } else {
                    toast(getString(R.string.permission_denied))
                }
            }

            override fun onError(exception: Exception) {
                toast("error ${exception.message}")
                exception.printStackTrace()
            }

            override fun onPermissionDenied() {
                toast(getString(R.string.permission_denied))
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE) return
        if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
            toast(getString(R.string.permission_denied))
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun String.isPermissionGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkSelfPermission(this) == PackageManager.PERMISSION_GRANTED
        else
            true
}
