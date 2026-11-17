package org.setu.skillswapper

import android.media.Image

data class SkillSwapModel(
    var id: Long = 0,
    var skillWanted: String,
    var skillOffered: String,
    var location: String,
    var image: String = "" //Empty "" means that there is no image, we save it as a String to make it easier to save to JSON
)
