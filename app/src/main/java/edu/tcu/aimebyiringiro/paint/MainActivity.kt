package edu.tcu.aimebyiringiro.paint

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope

import com.bumptech.glide.Glide
import edu.tcu.brookeratcliff.paint.R

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                Glide.with(this).load(it).into(findViewById<ImageView>(R.id.background_iv))
            } ?: run {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
        val drawingView = findViewById<DrawingView>(R.id.drawing_view)
        setUpPallet(drawingView)
        setUpPathWidthSelector(drawingView)
        val backgroundIv = findViewById<ImageView>(R.id.background_iv)
        setUpBackgroundPicker(backgroundIv)
        setUpUndo(drawingView)
        setUpSave()
        setUpGalleryPicker(pickImageLauncher)
        findViewById<ImageView>(R.id.undo_iv).setOnClickListener{
            drawingView.undoPath()
        }
    }

    private fun setUpPallet(drawingView: DrawingView) {
        val palletLayout: LinearLayout = findViewById(R.id.pallet_ll)
        for (i in 0 until palletLayout.childCount) {
            val colorView = palletLayout.getChildAt(i) as ImageView
            colorView.setImageResource(R.drawable.path_color_normal)
            colorView.setOnClickListener {
                for (j in 0 until palletLayout.childCount) {
                    val view = palletLayout.getChildAt(j) as ImageView
                    view.setImageResource(R.drawable.path_color_normal)
                }
                colorView.setImageResource(R.drawable.path_color_selected)
                val color = when (i) {
                    0 -> resources.getColor(R.color.black, null)
                    1 -> resources.getColor(R.color.red, null)
                    2 -> resources.getColor(R.color.green, null)
                    3 -> resources.getColor(R.color.blue, null)
                    4 -> resources.getColor(R.color.tcu_purple, null)
                    5 -> resources.getColor(R.color.off_white, null)
                    else -> Color.BLACK
                }
                drawingView.setPathColor(color)
            }
        }
    }

    private fun setUpUndo( drawingView: DrawingView) {
        findViewById<ImageView>(R.id.undo_iv).setOnClickListener{
            drawingView.undoPath()
        }
    }
    private fun setUpPathWidthSelector(drawingView: DrawingView) {
        findViewById<ImageView>(R.id.brush_iv).setOnClickListener{
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.path_width_selector)
            dialog.show()
            val smallIv = dialog.findViewById<ImageView>(R.id.small_width_iv)
            val mediumIv = dialog.findViewById<ImageView>(R.id.medium_width_iv)
            val largeIv = dialog.findViewById<ImageView>(R.id.large_width_iv)
            smallIv.setOnClickListener{
                drawingView.setPathWidth(resources.getDimension(R.dimen.small_path_width))
                dialog.dismiss()
            }
            mediumIv.setOnClickListener{
                drawingView.setPathWidth(resources.getDimension(R.dimen.medium_path_width))
                dialog.dismiss()
            }
            largeIv.setOnClickListener{
                drawingView.setPathWidth(resources.getDimension(R.dimen.large_path_width))
                dialog.dismiss()
            }
        }


    }
    private fun setUpGalleryPicker( pickImageLauncher: ActivityResultLauncher<String>) {
        findViewById<ImageView>(R.id.gallery_iv).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }
    private fun setUpBackgroundPicker(backgroundIv: ImageView){
        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
            uri ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                Glide.with(this).load(uri).into(backgroundIv)
                backgroundIv.setImageURI(uri)
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
        backgroundIv.setOnClickListener{
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    private fun setUpSave() {
        findViewById<ImageView>(R.id.save_iv).setOnClickListener {
            val dialog = Dialog(this).apply {
                setContentView(R.layout.in_progress)
                setCancelable(false)
                show()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = findViewById<FrameLayout>(R.id.drawing_view).drawToBitmap()
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "drawing_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    uri?.let {
                        shareImage(it)
                    }
                }
            }
        }
    }
    private fun shareImage(uri: android.net.Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
        }
        startActivity(shareIntent)
    }

}