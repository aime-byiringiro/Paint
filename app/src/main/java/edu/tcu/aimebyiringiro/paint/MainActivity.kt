package edu.tcu.aimebyiringiro.paint

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
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

        val drawingView = findViewById<DrawingView>(R.id.drawing_view)
        setUpPallet(drawingView)
        setUpPathWidthSelector(drawingView)
        val backgroundIv = findViewById<ImageView>(R.id.background_iv)
        setUpBackgroundPicker(backgroundIv)
        setUpUndo(drawingView)
        setUpSave()
        findViewById<ImageView>(R.id.undo_iv).setOnClickListener{
            drawingView.undoPath()
        }
    }

    private fun setUpPallet(drawingView: DrawingView) {
        val palletLayout: LinearLayout = findViewById(R.id.pallet_ll)

        // Use for loop to set up the color palette
        for (i in 0 until palletLayout.childCount) {
            val colorView = palletLayout.getChildAt(i) as ImageView

            // Use setImageResource for the initial drawable state
            colorView.setImageResource(R.drawable.path_color_normal)

            // Set click listener to handle color selection
            colorView.setOnClickListener {
                // Reset all color views to normal drawable
                for (j in 0 until palletLayout.childCount) {
                    val view = palletLayout.getChildAt(j) as ImageView
                    view.setImageResource(R.drawable.path_color_normal)  // Reset drawable
                }

                // Highlight the selected color view
                colorView.setImageResource(R.drawable.path_color_selected)  // Set selected drawable

                // Access the background color of the selected ImageView and convert it to Int
                val color = when (i) {
                    0 -> resources.getColor(R.color.black, null)  // Black
                    1 -> resources.getColor(R.color.red, null)    // Red
                    2 -> resources.getColor(R.color.green, null)  // Green
                    3 -> resources.getColor(R.color.blue, null)   // Blue
                    4 -> resources.getColor(R.color.tcu_purple, null) // Purple
                    5 -> resources.getColor(R.color.off_white, null) // Off-White
                    else -> Color.BLACK  // Default color
                }

                drawingView.setPathColor(color)  // Update the path color in DrawingView
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

        }

        //dialog.dismiss()
    }

    private fun setUpBackgroundPicker(backgroundIv: ImageView){
        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
            it?.let{Glide.with(this).load(it).into(backgroundIv)}
        }
//use a listener to launch the sheet
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun setUpSave() {
        //if backgroundIv is null, assign a color to the backgroundIv
        val dialog = showInProgress()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(5000)
            val bitmap = findViewById<FrameLayout>(R.id.drawing_fl).drawToBitmap()
            val values = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    System.currentTimeMillis().toString().substring(2, 11) + ".jpeg)"
                )
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            }

            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let {
                contentResolver.openOutputStream(it).use { stream ->
                    stream?.let { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream) }
                }
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                //making the sharing sheet, test whether ur app is free of bugs 
            }
        }
    }

    private fun showInProgress(): Dialog {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.in_progress)
        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }

}