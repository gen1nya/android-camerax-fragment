package com.example.camerax_fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File

class CameraFragment : Fragment() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 45627
    }

    interface OnPictureTakenListener {
        fun onImageTaken(image: ByteArray)
        fun onError()
        fun onPermissionDenied()
    }

    @IntRange(from = 0, to = 100)
    var zoom: Int = 0
        set(value) {
            cameraControl.setLinearZoom(value.toFloat() / 100.toFloat())
            field = value
        }

    var flash: Boolean = false

    private val targetResolution = Size(720, 1280)

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraControl: CameraControl

    var imageTakeListener: OnPictureTakenListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isPermissionGranted(Manifest.permission.CAMERA)) {
            view_finder.post {
                bindCameraUseCases()
            }
        } else {
            requestPermissions()
        }
        setUpTapToFocus()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTapToFocus() {
        view_finder.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val factory = object: MeteringPointFactory() {
                override fun convertPoint(x: Float, y: Float): PointF = PointF(x, y)
            }
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            cameraControl.startFocusAndMetering(action)
            return@setOnTouchListener true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) return
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            bindCameraUseCases()
        } else {
            imageTakeListener?.onPermissionDenied()
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun isPermissionGranted(permission: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requireContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        else
            true

    fun takePicture() {
        val photoFile = File(requireContext().filesDir, "temp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        if (flash) cameraControl.enableTorch(true)
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cameraControl.enableTorch(false)
                    imageTakeListener?.onImageTaken(photoFile.readBytes())
                }

                override fun onError(exception: ImageCaptureException) {
                    cameraControl.enableTorch(false)
                    imageTakeListener?.onError()
                    exception.printStackTrace()
                }
            })
    }

    private fun bindCameraUseCases() {
        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val rotation = view_finder.display.rotation
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(rotation)
                .build()
            preview.setSurfaceProvider(view_finder.previewSurfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(targetResolution)
                //.setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(rotation)
                .build()

            cameraProvider.unbindAll()
            try {
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
}