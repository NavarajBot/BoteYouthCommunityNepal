package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object BoteFirestoreManager {
    private const val TAG = "BoteFirestoreManager"

    private var firestore: FirebaseFirestore? = null
    private var isFirestoreAvailable = false

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _firestoreConnectionStatus = MutableStateFlow("Disconnected")
    val firestoreConnectionStatus: StateFlow<String> = _firestoreConnectionStatus.asStateFlow()

    private var newsListener: ListenerRegistration? = null
    private var archiveListener: ListenerRegistration? = null
    private var scholarshipListener: ListenerRegistration? = null
    private var forumListener: ListenerRegistration? = null
    private var donationListener: ListenerRegistration? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context, repository: BoteRepository) {
        try {
            if (BoteAuthManager.isFirebaseActive()) {
                firestore = FirebaseFirestore.getInstance()
                isFirestoreAvailable = true
                _firestoreConnectionStatus.value = "Connected (Cloud)"
                Log.d(TAG, "Firebase Firestore initialized successfully.")

                // Start real-time Firestore synchronization listeners
                startRealTimeListeners(repository)
            } else {
                _firestoreConnectionStatus.value = "Local Sandbox (Mock Cloud Live)"
                Log.d(TAG, "Firestore running in Local Fallback mode.")
            }
        } catch (e: Exception) {
            _firestoreConnectionStatus.value = "Local Sandbox (Mock Cloud Live)"
            Log.e(TAG, "Failed to initialize Firestore: ${e.message}. Using Local Sandbox mode.")
        }
    }

    fun isCloudActive(): Boolean = isFirestoreAvailable

    private fun startRealTimeListeners(repository: BoteRepository) {
        val db = firestore ?: return

        _isCloudSyncing.value = true

        // 1. Real-Time Listener for Community News
        newsListener = db.collection("community_news")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to community_news: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val idStr = doc.id
                            val id = idStr.toIntOrNull() ?: continue
                            val data = doc.data ?: continue
                            val article = data.toArticle(id)
                            repository.updateArticle(article) // Keep local database updated
                        }
                    }
                }
            }

        // 2. Real-Time Listener for Archive Items (Resources)
        archiveListener = db.collection("archive_items")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to archive_items: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val idStr = doc.id
                            val id = idStr.toIntOrNull() ?: continue
                            val data = doc.data ?: continue
                            val resource = data.toResource(id)
                            repository.updateResource(resource) // Keep local database updated
                        }
                    }
                }
            }

        // 3. Real-Time Listener for Scholarship Data (Registrations)
        scholarshipListener = db.collection("scholarship_data")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to scholarship_data: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val idStr = doc.id
                            val id = idStr.toIntOrNull() ?: continue
                            val data = doc.data ?: continue
                            val registration = data.toRegistration(id)
                            repository.insertRegistration(registration) // Keep local database updated
                        }
                    }
                }
            }

        // 4. Real-Time Listener for Forum Posts (Discussion Forum)
        forumListener = db.collection("forum_posts")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to forum_posts: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val idStr = doc.id
                            val id = idStr.toIntOrNull() ?: continue
                            val data = doc.data ?: continue
                            val forumPost = data.toForumPost(id)
                            repository.insertForumPost(forumPost) // Keep local database updated
                        }
                    }
                }
            }

        // 5. Real-Time Listener for Donations
        donationListener = db.collection("donations")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to donations: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val idStr = doc.id
                            val id = idStr.toIntOrNull() ?: continue
                            val data = doc.data ?: continue
                            val donation = data.toDonation(id)
                            repository.insertDonation(donation) // Keep local database updated
                            Log.d(TAG, "Synched donation ID $id from Firestore to local Room.")
                        }
                    }
                }
            }

        _isCloudSyncing.value = false
    }

    fun stopListeners() {
        newsListener?.remove()
        archiveListener?.remove()
        scholarshipListener?.remove()
        forumListener?.remove()
        donationListener?.remove()
        newsListener = null
        archiveListener = null
        scholarshipListener = null
        forumListener = null
        donationListener = null
    }

    // --- News (Articles) Cloud Write Actions ---
    fun saveOrUpdateArticleInCloud(article: Article) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("community_news")
                    ?.document(article.id.toString())
                    ?.set(article.toMap())
                    ?.awaitTask()
                Log.d(TAG, "Article '${article.title}' saved to Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving article to Firestore: ${e.message}")
            }
        }
    }

    fun deleteArticleFromCloud(id: Int) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("community_news")
                    ?.document(id.toString())
                    ?.delete()
                    ?.awaitTask()
                Log.d(TAG, "Article ID $id deleted from Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting article from Firestore: ${e.message}")
            }
        }
    }

    // --- Archive Items (Resources) Cloud Write Actions ---
    fun saveOrUpdateResourceInCloud(resource: Resource) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("archive_items")
                    ?.document(resource.id.toString())
                    ?.set(resource.toMap())
                    ?.awaitTask()
                Log.d(TAG, "Resource '${resource.title}' saved to Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving resource to Firestore: ${e.message}")
            }
        }
    }

    fun deleteResourceFromCloud(id: Int) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("archive_items")
                    ?.document(id.toString())
                    ?.delete()
                    ?.awaitTask()
                Log.d(TAG, "Resource ID $id deleted from Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting resource from Firestore: ${e.message}")
            }
        }
    }

    // --- Scholarship (Registration) Cloud Write Actions ---
    fun saveOrUpdateRegistrationInCloud(registration: Registration) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("scholarship_data")
                    ?.document(registration.id.toString())
                    ?.set(registration.toMap())
                    ?.awaitTask()
                Log.d(TAG, "Registration ID ${registration.id} saved to Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving registration to Firestore: ${e.message}")
            }
        }
    }

    fun deleteRegistrationFromCloud(id: Int) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("scholarship_data")
                    ?.document(id.toString())
                    ?.delete()
                    ?.awaitTask()
                Log.d(TAG, "Registration ID $id deleted from Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting registration from Firestore: ${e.message}")
            }
        }
    }

    // --- Manual Mapping Extensions ---
    private fun Map<String, Any>.toArticle(id: Int): Article {
        return Article(
            id = id,
            title = this["title"] as? String ?: "",
            content = this["content"] as? String ?: "",
            category = this["category"] as? String ?: "",
            author = this["author"] as? String ?: "",
            date = this["date"] as? String ?: "",
            imageUrl = this["imageUrl"] as? String ?: "",
            seoTitle = this["seoTitle"] as? String ?: "",
            seoMeta = this["seoMeta"] as? String ?: "",
            seoKeywords = this["seoKeywords"] as? String ?: "",
            isDraft = this["isDraft"] as? Boolean ?: false,
            isScheduled = this["isScheduled"] as? Boolean ?: false,
            publishDate = (this["publishDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun Article.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "content" to content,
            "category" to category,
            "author" to author,
            "date" to date,
            "imageUrl" to imageUrl,
            "seoTitle" to seoTitle,
            "seoMeta" to seoMeta,
            "seoKeywords" to seoKeywords,
            "isDraft" to isDraft,
            "isScheduled" to isScheduled,
            "publishDate" to publishDate
        )
    }

    private fun Map<String, Any>.toResource(id: Int): Resource {
        return Resource(
            id = id,
            title = this["title"] as? String ?: "",
            category = this["category"] as? String ?: "",
            resourceType = this["resourceType"] as? String ?: "",
            author = this["author"] as? String ?: "",
            year = this["year"] as? String ?: "",
            fileSize = this["fileSize"] as? String ?: "",
            downloadCount = (this["downloadCount"] as? Number)?.toInt() ?: 0,
            description = this["description"] as? String ?: "",
            contentUrl = this["contentUrl"] as? String ?: ""
        )
    }

    private fun Resource.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "category" to category,
            "resourceType" to resourceType,
            "author" to author,
            "year" to year,
            "fileSize" to fileSize,
            "downloadCount" to downloadCount,
            "description" to description,
            "contentUrl" to contentUrl
        )
    }

    private fun Map<String, Any>.toRegistration(id: Int): Registration {
        return Registration(
            id = id,
            type = this["type"] as? String ?: "",
            applicantName = this["applicantName"] as? String ?: "",
            email = this["email"] as? String ?: "",
            phone = this["phone"] as? String ?: "",
            details = this["details"] as? String ?: "",
            timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = this["status"] as? String ?: "Under Review"
        )
    }

    private fun Registration.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "type" to type,
            "applicantName" to applicantName,
            "email" to email,
            "phone" to phone,
            "details" to details,
            "timestamp" to timestamp,
            "status" to status
        )
    }

    // --- Forum Posts Cloud Write Actions ---
    fun saveOrUpdateForumPostInCloud(post: ForumPost) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("forum_posts")
                    ?.document(post.id.toString())
                    ?.set(post.toMap())
                    ?.awaitTask()
                Log.d(TAG, "ForumPost ID ${post.id} saved to Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving forum post to Firestore: ${e.message}")
            }
        }
    }

    fun deleteForumPostFromCloud(id: Int) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("forum_posts")
                    ?.document(id.toString())
                    ?.delete()
                    ?.awaitTask()
                Log.d(TAG, "ForumPost ID $id deleted from Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting forum post from Firestore: ${e.message}")
            }
        }
    }

    private fun Map<String, Any>.toForumPost(id: Int): ForumPost {
        return ForumPost(
            id = id,
            author = this["author"] as? String ?: "",
            authorRole = this["authorRole"] as? String ?: "Member",
            content = this["content"] as? String ?: "",
            timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            likes = (this["likes"] as? Number)?.toInt() ?: 0
        )
    }

    private fun ForumPost.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "author" to author,
            "authorRole" to authorRole,
            "content" to content,
            "timestamp" to timestamp,
            "likes" to likes
        )
    }

    // --- Donations Cloud Actions ---
    fun saveOrUpdateDonationInCloud(donation: BoteDonation) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("donations")
                    ?.document(donation.id.toString())
                    ?.set(donation.toMap())
                    ?.awaitTask()
                Log.d(TAG, "Donation ID ${donation.id} saved to Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving donation to Firestore: ${e.message}")
            }
        }
    }

    fun deleteDonationFromCloud(id: Int) {
        if (!isFirestoreAvailable) return
        scope.launch {
            try {
                firestore?.collection("donations")
                    ?.document(id.toString())
                    ?.delete()
                    ?.awaitTask()
                Log.d(TAG, "Donation ID $id deleted from Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting donation from Firestore: ${e.message}")
            }
        }
    }

    private fun Map<String, Any>.toDonation(id: Int): BoteDonation {
        return BoteDonation(
            id = id,
            donorName = this["donorName"] as? String ?: "",
            email = this["email"] as? String ?: "",
            amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
            currency = this["currency"] as? String ?: "NPR",
            targetCause = this["targetCause"] as? String ?: "",
            paymentMethod = this["paymentMethod"] as? String ?: "",
            timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            message = this["message"] as? String ?: "",
            status = this["status"] as? String ?: "Verified"
        )
    }

    private fun BoteDonation.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "donorName" to donorName,
            "email" to email,
            "amount" to amount,
            "currency" to currency,
            "targetCause" to targetCause,
            "paymentMethod" to paymentMethod,
            "timestamp" to timestamp,
            "message" to message,
            "status" to status
        )
    }
}
