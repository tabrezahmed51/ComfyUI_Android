package com.example.comfyui.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreWorkflow(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val nodesJson: String = "",
    val linksJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreGalleryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val promptText: String = "",
    val negativePromptText: String = "",
    val seed: Long = 0L,
    val steps: Long = 20L,
    val cfg: Double = 7.0,
    val samplerName: String = "",
    val modelName: String = "",
    val width: Long = 512L,
    val height: Long = 512L,
    val durationSeconds: Double = 0.0,
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
