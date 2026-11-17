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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AddEditActivity : AppCompatActivity() {

    private lateinit var wantedInput: EditText
    private lateinit var offeredInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var miniMap: MapView
    private var miniMarker: Marker? = null


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

    // Opens the map screen and puts the chosen coordinates in the location box
    private val mapPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val lat = data.getDoubleExtra("latitude", 0.0)
                val lon = data.getDoubleExtra("longitude", 0.0)

                selectedLatitude = lat
                selectedLongitude = lon
                locationInput.setText("%.5f, %.5f".format(lat, lon))
                showMiniMap()
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_add_edit)

        wantedInput = findViewById(R.id.wantedInput)
        offeredInput = findViewById(R.id.offeredInput)
        locationInput = findViewById(R.id.locationInput)
        // Must be found before loadExistingSwap runs, or editing a swap crashes
        imagePreview = findViewById(R.id.imagePreview)
        // Small preview map: shows the pin, but can't be dragged
        miniMap = findViewById(R.id.miniMap)
        miniMap.setTileSource(TileSourceFactory.MAPNIK)
        miniMap.setMultiTouchControls(false)
        miniMap.setOnTouchListener { _, _ -> true }   // Swallows touches so it can't be moved
        miniMap.controller.setZoom(16.0)

        editingId = intent.getLongExtra("id", -1L).takeIf { it != -1L }

        if (editingId != null) {
            findViewById<TextView>(R.id.formTitle).text = "Edit Swap"
            loadExistingSwap(editingId!!)
            showMiniMap()
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
    }



    // Show the mini map with a pin, if a location has been picked
    private fun showMiniMap() {
        val lat = selectedLatitude
        val lon = selectedLongitude
        if (lat == null || lon == null) return

        val point = GeoPoint(lat, lon)
        miniMap.visibility = android.view.View.VISIBLE
        miniMap.controller.setCenter(point)

        if (miniMarker == null) {
            miniMarker = Marker(miniMap).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            miniMap.overlays.add(miniMarker)
        }
        miniMarker?.position = point
        miniMap.invalidate()
    }





    override fun onResume() {
        super.onResume()
        miniMap.onResume()
    }

    override fun onPause() {
        miniMap.onPause()
        super.onPause()
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