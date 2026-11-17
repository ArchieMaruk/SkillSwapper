package org.setu.skillswapper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import timber.log.Timber

class MapPickerActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSM needs to know which app is asking for map tiles
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_map_picker)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)  // The standard OpenStreetMap style
        map.setMultiTouchControls(true)   // Pinch to zoom

        // Start where the swap already is, or Waterford if it has no location yet
        val lat = intent.getDoubleExtra("latitude", 52.245954)
        val lon = intent.getDoubleExtra("longitude", -7.139539)
        val start = GeoPoint(lat, lon)

        map.controller.setZoom(16.0)
        map.controller.setCenter(start)
        setSelectedPoint(start)
        showExistingSwaps()

        // Tap or long press the map to move the pin
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                setSelectedPoint(point)
                return true
            }

            override fun longPressHelper(point: GeoPoint): Boolean {
                setSelectedPoint(point)
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(receiver))

        // Send the chosen spot back to the Add/Edit screen
        findViewById<Button>(R.id.confirmLocationButton).setOnClickListener {
            val point = selectedPoint ?: return@setOnClickListener

            val result = Intent()
                .putExtra("latitude", point.latitude)
                .putExtra("longitude", point.longitude)

            Timber.d("Picked location ${point.latitude}, ${point.longitude}")
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    // Move the "selected" pin to a new spot
    private fun setSelectedPoint(point: GeoPoint) {
        selectedPoint = point

        if (marker == null) {
            marker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
        }

        marker?.position = point
        marker?.title = "Selected location"
        map.invalidate()   // Redraw the map
    }

    // Show every swap that has a pinned location
    private fun showExistingSwaps() {
        AppData.swaps.findAll().forEach { swap ->
            val lat = swap.latitude
            val lon = swap.longitude

            if (lat != null && lon != null) {
                val swapMarker = Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    title = "Wants: ${swap.skillWanted}"
                    snippet = "Offers: ${swap.skillOffered}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(swapMarker)
            }
        }
        map.invalidate()
    }

    // The map has to be told when the screen pauses and resumes
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        map.onPause()
        super.onPause()
    }
}