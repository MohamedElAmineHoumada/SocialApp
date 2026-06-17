package com.Groupe15.SocialApp.models

import androidx.annotation.DrawableRes

data class Interest(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    var isSelected: Boolean = false
)
