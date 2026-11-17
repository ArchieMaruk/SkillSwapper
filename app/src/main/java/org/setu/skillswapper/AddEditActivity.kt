package org.setu.skillswapper

import android.app.Activity
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
    private lateinit var pinnedLocationText: TextView

    private var editingId: Long? = null
    private var selectedImage: String = ""            // "" means no image
    private var selectedLatitude: Double? = null      // null means no pin
    private var selectedLongitude: Double? = null

    // Opens the phone's file picker and gets back the chosen image
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
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

    // Opens the map screen and gets back the chosen coordinates
    private val mapPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                selectedLongitude = data.getDoubleExtra("longitude", 0.0)
                showPinnedLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        wantedInput = findViewById(R.id.wantedInput)
        offeredInput = findViewById(R.id.offeredInput)
        locationInput = findViewById(R.id.locationInput)
        // Must be found before loadExistingSwap runs, or editing a swap crashes
        imagePreview = findViewById(R.id.imagePreview)
        pinnedLocationText = findViewById(R.id.pinnedLocationText)

        editingId = intent.getLongExtra("id", -1L).takeIf { it != -1L }

        if (editingId != null) {
            findViewById<TextView>(R.id.formTitle).text = "Edit Swap"
            loadExistingSwap(editingId!!)
        }

        // Open the map, starting at the current pin if there is one
        findViewById<Button>(R.id.pickLocationButton).setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            val lat = selectedLatitude
            val lon = selectedLongitude
            if (lat != null && lon != null) {
                intent.putExtra("latitude", lat)
                intent.putExtra("longitude", lon)
            }
            mapPicker.launch(intent)
        }

        // Open the picker, showing only images
        findViewById<Button>(R.id.pickImageButton).setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        // Clear the image, then Save to remove it from the swap
        findViewById<Button>(R.id.removeImageButton).setOnClickListener {
            selectedImage = ""                      // Marks swap as having no image
            imagePreview.setImageDrawable(null)     // Clears preview so we can see that it's gone
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

        // Show the saved image, if there is one
        if (swap.image.isNotEmpty()) {
            selectedImage = swap.image
            imagePreview.setImageURI(Uri.parse(swap.image))
        }

        // Load the saved pin, if there is one
        selectedLatitude = swap.latitude
        selectedLongitude = swap.longitude
        showPinnedLocation()
    }

    // Show the pinned coordinates under the Pick on Map button
    private fun showPinnedLocation() {
        val lat = selectedLatitude
        val lon = selectedLongitude
        pinnedLocationText.text = if (lat != null && lon != null) {
            "Pinned: %.5f, %.5f".format(lat, lon)
        } else {
            "No location pinned"
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
                    image = selectedImage,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude
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
                    image = selectedImage,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude
                )
            )
            Timber.d("Updated swap id=$editingId")
            Toast.makeText(this, "Swap updated", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}