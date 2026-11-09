package com.example.comfyui.data.firebase

import android.content.Context
import android.util.Log
import com.example.comfyui.R
import com.example.comfyui.data.model.GenerationJob
import com.example.comfyui.data.model.Workflow
import com.example.comfyui.data.repository.WorkflowRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseRepo"

        fun create(context: Context): FirebaseRepository {
            val dbId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            return FirebaseRepository(firestore)
        }
    }

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun syncUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: "ComfyUI Creator"),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            Log.d(TAG, "User profile synced: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing profile", e)
        }
    }

    fun observeWorkflows(): Flow<List<FirestoreWorkflow>> = flow {
        val uid = requireUserId()
        val flow = db.collection("users").document(uid).collection("workflows")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(FirestoreWorkflow::class.java) }
        emitAll(flow)
    }

    suspend fun saveWorkflow(workflow: Workflow) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workflows").document(workflow.id)
        val jsonPayload = WorkflowRepository.exportToJson(workflow)
        val payload = mapOf(
            "userId" to uid,
            "name" to workflow.name,
            "description" to workflow.description,
            "nodesJson" to jsonPayload,
            "linksJson" to "[]",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        Log.d(TAG, "Workflow saved to cloud: ${workflow.id}")
    }

    fun observeGallery(): Flow<List<FirestoreGalleryItem>> = flow {
        val uid = requireUserId()
        val flow = db.collection("users").document(uid).collection("gallery")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(FirestoreGalleryItem::class.java) }
        emitAll(flow)
    }

    suspend fun saveGalleryItem(job: GenerationJob) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("gallery").document(job.id)
        val payload = mapOf(
            "userId" to uid,
            "promptText" to job.promptText,
            "negativePromptText" to job.negativePromptText,
            "seed" to job.seed,
            "steps" to job.steps,
            "cfg" to job.cfg.toDouble(),
            "samplerName" to job.samplerName,
            "modelName" to job.modelName,
            "width" to job.width,
            "height" to job.height,
            "durationSeconds" to job.durationSeconds.toDouble(),
            "imageUrl" to (job.outputImageUrl ?: ""),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        Log.d(TAG, "Gallery item synced to cloud: ${job.id}")
    }
}
