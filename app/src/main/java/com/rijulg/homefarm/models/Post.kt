package com.rijulg.homefarm.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Post(
    var description: String = "",
    @get:PropertyName("image_url") @set:PropertyName("image_url") var imageUrl: String = "",
    @get:PropertyName("creation_time_ms") @set:PropertyName("creation_time_ms") var creationTimeMs: Long = 0,
    var user: User? = null,
    var fruit: String = ""
) : Serializable