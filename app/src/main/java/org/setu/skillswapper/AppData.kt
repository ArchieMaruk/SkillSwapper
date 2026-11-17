package org.setu.skillswapper


//Holds the one MemStore so every screen uses the same one
object AppData {
    val swaps = SkillSwapMemStore()
}