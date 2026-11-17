package org.setu.skillswapper

//This listener is for the cards only

interface OnSwapListener {
    fun onSwapClick(swap: SkillSwapModel)
    fun onSwapLongClick(swap: SkillSwapModel): Boolean
}