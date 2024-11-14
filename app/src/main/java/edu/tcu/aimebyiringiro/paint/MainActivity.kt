package edu.tcu.aimebyiringiro.paint

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
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

            //dialog.dismiss()

        }

        //dialog.dismiss()
    }


    private fun setUpBackgroundPicker(backgroundIv: ImageView){
//        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
//            it?.let{Glide.with(this).load(it).into(backgroundIv)}

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

//use a listener to launch the sheet

    }

    /**
     * Shows a progress dialog while the app is saving the image. The function
     * takes a lambda which is called after the delay.
     */


    private fun setUpSave() {
        findViewById<ImageView>(R.id.save_iv).setOnClickListener {
            val dialog = Dialog(this).apply {
                setContentView(R.layout.in_progress)
                setCancelable(false)
                show()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Capture the drawing as a bitmap
                    val bitmap = findViewById<FrameLayout>(R.id.drawing_view).drawToBitmap()

                    // Define the metadata for saving the image
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "drawing_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    // Insert the image into the external content URI
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                    // Save the bitmap to the OutputStream
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        // Dismiss the dialog and show a success message
                        dialog.dismiss()
                        if (uri != null) {
                            Toast.makeText(this@MainActivity, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to save image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SaveImage", "Error saving image", e)
                }
            }
        }
    }



}