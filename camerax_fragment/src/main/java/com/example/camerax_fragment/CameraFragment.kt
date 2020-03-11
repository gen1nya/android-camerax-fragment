package com.example.camerax_fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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

        fun newInstance(
            cameraListener: CameraListener
        ) : CameraFragment = CameraFragment().apply {
            this.listener = cameraListener
        }
    }

    /**
     * listener to receive camera events
     * */
    interface CameraListener {
        fun onImageTaken(image: ByteArray)
        fun onError(exception: Exception)
        fun onPermissionDenied()
    }

    /**
     * set linear zoom in range from 0 to 100. Can be used to zoom seekbar.
     * to pinch-to-zoom you need [CameraControl.setZoomRatio]
     * */
    @IntRange(from = 0, to = 100)
    var zoom: Int = 0
        set(value) {
            cameraControl?.setLinearZoom(value.toFloat() / 100.toFloat())
            field = value
        }

    /**
     * flashlight
     * */
    var flash: Boolean = false

    //TODO WIP
    private val targetResolution = Size(720, 1280)

    /**
     * image capture usecase
     * */
    private lateinit var imageCapture: ImageCapture

    /**
     * camera control can be used to set zoom, focus and other camera features
     * */
    private var cameraControl: CameraControl? = null

    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    private var listener: CameraListener? = null

    fun setCameraListener(listener: CameraListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onResume() {
        super.onResume()
        if (Manifest.permission.CAMERA.isPermissionGranted()) {
            view_finder.post {
                bindCameraUseCases()
                setUpTapToFocus()
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTapToFocus() {
        view_finder.setOnTouchListener { view, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val factory = DisplayOrientedMeteringPointFactory(
                view.display, // TODO testing required
                cameraSelector,
                view.width.toFloat(),
                view.height.toFloat()
            )
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            cameraControl?.startFocusAndMetering(action)
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
            setUpTapToFocus()
        } else {
            listener?.onPermissionDenied()
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }
    // so, this work only on permission strings (Manifest.permission). Ну а что вы хотели?
    private fun String.isPermissionGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requireContext().checkSelfPermission(this) == PackageManager.PERMISSION_GRANTED
        else
            true

    fun takePicture() {
        val photoFile = File(requireContext().filesDir, "temp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        if (flash) cameraControl?.enableTorch(true)
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cameraControl?.enableTorch(false)
                    listener?.onImageTaken(photoFile.readBytes())
                }

                override fun onError(exception: ImageCaptureException) {
                    cameraControl?.enableTorch(false)
                    listener?.onError(exception)
                }
            })
    }

    private fun bindCameraUseCases() {
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
                listener?.onError(e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
}