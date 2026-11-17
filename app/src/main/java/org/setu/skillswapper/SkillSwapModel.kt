package org.setu.skillswapper

data class SkillSwapModel(
    var id: Long = 0,
    var skillWanted: String,
    var skillOffered: String,
    var location: String,
    var image: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null  //null means that no spot has been pinned yet, so map pins stays optional
)
