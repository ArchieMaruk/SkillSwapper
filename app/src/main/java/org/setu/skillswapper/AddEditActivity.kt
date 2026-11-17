package org.setu.skillswapper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class AddEditActivity : AppCompatActivity() {

    private lateinit var wantedInput: EditText
    private lateinit var offeredInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var imagePreview: ImageView

    private var editingId: Long? = null
    private var selectedImage: String = ""        //The "" means no image

    // NEW: opens the phone's file picker and gets back the chosen image
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // Keep permission to read this image after the app restarts
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some image sources don't allow this, which is fine
                }

                selectedImage = uri.toString()
                imagePreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        wantedInput = findViewById(R.id.wantedInput)
        offeredInput = findViewById(R.id.offeredInput)
        locationInput = findViewById(R.id.locationInput)
        imagePreview = findViewById(R.id.imagePreview) //This will now be found before loadExistingSwap runs, or editing a swap with
        //an image will crash

        editingId = intent.getLongExtra("id", -1L).takeIf { it != -1L }

        if (editingId != null) {
            findViewById<TextView>(R.id.formTitle).text = "Edit Swap"
            loadExistingSwap(editingId!!)
        }

        //Open the picker, showing only images
        findViewById<Button>(R.id.pickImageButton).setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        // Clears the image, then Saves to remove it from the swap
        findViewById<Button>(R.id.removeImageButton).setOnClickListener {
            selectedImage = ""                      // Marks swap as having no image
            imagePreview.setImageDrawable(null)     // Clears preview so we can see that its gone
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveSwap() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
    }

    private fun loadExistingSwap(id: Long) {
        val swap = AppData.swaps.findOne(id)

        if (swap == null) {
            Toast.makeText(this, "Swap not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        wantedInput.setText(swap.skillWanted)
        offeredInput.setText(swap.skillOffered)
        locationInput.setText(swap.location)

        // NEW: show the saved image, if there is one
        if (swap.image.isNotEmpty()) {
            selectedImage = swap.image
            imagePreview.setImageURI(Uri.parse(swap.image))
        }
    }

    private fun saveSwap() {
        val wanted = wantedInput.text.toString().trim()
        val offered = offeredInput.text.toString().trim()
        val location = locationInput.text.toString().trim()

        if (wanted.isEmpty()) {
            wantedInput.error = "Required"
            return
        }
        if (offered.isEmpty()) {
            offeredInput.error = "Required"
            return
        }
        if (location.isEmpty()) {
            locationInput.error = "Required"
            return
        }

        if (editingId == null) {
            AppData.swaps.create(
                SkillSwapModel(
                    skillWanted = wanted,
                    skillOffered = offered,
                    location = location,
                    image = selectedImage
                )
            )
            Timber.d("Created swap: $wanted for $offered")
            Toast.makeText(this, "Swap created", Toast.LENGTH_SHORT).show()
        } else {
            AppData.swaps.update(
                SkillSwapModel(
                    id = editingId!!,
                    skillWanted = wanted,
                    skillOffered = offered,
                    location = location,
                    image = selectedImage
                )
            )
            Timber.d("Updated swap id=$editingId")
            Toast.makeText(this, "Swap updated", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}