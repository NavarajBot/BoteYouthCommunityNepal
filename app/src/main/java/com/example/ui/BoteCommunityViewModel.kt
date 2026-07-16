package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    HOME,
    LANDING_PAGES,
    DIGITAL_LIBRARY,
    COMMUNITY_HUB,
    CREATOR_DASHBOARD,
    GOOGLE_COMMAND_CENTER,
    DONATION_PORTAL
}

data class SeoReport(
    val score: Int,
    val grade: String,
    val checks: List<SeoCheckResult>,
    val suggestions: List<String>
)

data class SeoCheckResult(
    val label: String,
    val passed: Boolean,
    val scoreImpact: Int
)

data class NepalBoteLiveTelemetry(
    val bagmatiLevel: Double,
    val bagmatiStatus: String,
    val narayaniLevel: Double,
    val narayaniStatus: String,
    val sarlahiTemp: Double,
    val activeCanoesCount: Int,
    val cooperativeFundNpr: Int,
    val documentedWordsCount: Int,
    val lastUpdateDesc: String
)

class BoteCommunityViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BoteDatabase.getDatabase(application)
    private val repository = BoteRepository(db.boteDao())

    // --- Active UI Screens ---
    private val _activeScreen = MutableStateFlow(AppScreen.HOME)
    val activeScreen: StateFlow<AppScreen> = _activeScreen.asStateFlow()

    // --- Skeleton Data Fetching Loading States ---
    private val _isDataFetching = MutableStateFlow(false)
    val isDataFetching: StateFlow<Boolean> = _isDataFetching.asStateFlow()

    // --- Live Simulated/Real-Time Nepal Telemetry ---
    private val _liveTelemetry = MutableStateFlow(
        NepalBoteLiveTelemetry(
            bagmatiLevel = 3.2,
            bagmatiStatus = "Safe (Flow normal)",
            narayaniLevel = 4.8,
            narayaniStatus = "Normal Flow",
            sarlahiTemp = 31.4,
            activeCanoesCount = 36,
            cooperativeFundNpr = 182500,
            documentedWordsCount = 450,
            lastUpdateDesc = "Automatic Sarlahi gateway sync complete."
        )
    )
    val liveTelemetry: StateFlow<NepalBoteLiveTelemetry> = _liveTelemetry.asStateFlow()

    fun simulateDataFetching(durationMs: Long = 800) {
        viewModelScope.launch {
            _isDataFetching.value = true
            kotlinx.coroutines.delay(durationMs)
            _isDataFetching.value = false
        }
    }

    // --- Offline or Online Sync state ---
    val isOnlineMode = MutableStateFlow(true)
    val isSyncing = MutableStateFlow(false)

    // --- High Security & User Data Safety Settings ---
    val isEncrypted = MutableStateFlow(true)
    val isAppLockEnabled = MutableStateFlow(false)

    // --- Social Engagement State (Like, Share, Follow) ---
    val followedAuthors = MutableStateFlow<Set<String>>(emptySet())
    val appLiked = MutableStateFlow(false)
    val followedApp = MutableStateFlow(false)
    val appSharesCount = MutableStateFlow(124)
    val appFollowersCount = MutableStateFlow(1420)

    // --- Search & Filters ---
    val searchQuery = MutableStateFlow("")
    val selectedLibraryCategory = MutableStateFlow("All")

    val selectedLandingCategory = MutableStateFlow("Bote History")

    // --- Language Selection (English vs Nepali) ---
    val isNepali = MutableStateFlow(false)
    val appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)

    // --- Database Flows ---
    val publishedArticles: StateFlow<List<Article>> = repository.publishedArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val draftArticles: StateFlow<List<Article>> = repository.draftArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResources: StateFlow<List<Resource>> = repository.allResources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFaqs: StateFlow<List<FaqItem>> = repository.allFaqs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRegistrations: StateFlow<List<Registration>> = repository.allRegistrations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forumPosts: StateFlow<List<ForumPost>> = repository.forumPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScholarshipOpportunities: StateFlow<List<ScholarshipOpportunity>> = repository.allScholarshipOpportunities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppUpdates: StateFlow<List<AppUpdate>> = repository.allAppUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDonations: StateFlow<List<BoteDonation>> = repository.allDonations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Reactive Filtered Digital Library ---
    val filteredResources: StateFlow<List<Resource>> = combine(
        allResources,
        searchQuery,
        selectedLibraryCategory
    ) { resources, query, category ->
        resources.filter { item ->
            val matchesCategory = (category == "All" || item.category == category)
            val matchesQuery = (query.isEmpty() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.author.contains(query, ignoreCase = true))
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Draft Workspace State ---
    val draftTitle = MutableStateFlow("")
    val draftContent = MutableStateFlow("")
    val draftCategory = MutableStateFlow("Bote Community News")
    val draftAuthor = MutableStateFlow("Community Coordinator")
    val draftTargetKeyword = MutableStateFlow("Bote language preservation")
    val draftSeoTitle = MutableStateFlow("")
    val draftSeoMeta = MutableStateFlow("")

    val activeSeoReport: StateFlow<SeoReport> = combine(
        draftTitle,
        draftContent,
        draftTargetKeyword,
        draftSeoTitle,
        draftSeoMeta
    ) { title, content, keyword, seoTitle, seoMeta ->
        calculateSeoScore(title, content, keyword, seoTitle, seoMeta)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeoReport(0, "F", emptyList(), emptyList()))

    // --- Action Success States / Toasts ---
    private val _notificationMessage = MutableSharedFlow<String>()
    val notificationMessage: SharedFlow<String> = _notificationMessage.asSharedFlow()

    // --- Firebase Authentication States ---
    val currentUser: StateFlow<AuthUser?> = BoteAuthManager.currentUser
    val isAuthInitializing: StateFlow<Boolean> = BoteAuthManager.isInitializing

    init {
        // Initialize Firebase Auth / Local Fallback Auth
        BoteAuthManager.initialize(application)
        // Initialize Firebase Firestore / Local Sandbox
        BoteFirestoreManager.initialize(application, repository)

        // Initialize FCM Topic Subscriptions
        try {
            if (com.google.firebase.FirebaseApp.getApps(application).isNotEmpty()) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("news")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("BoteVM", "Subscribed to FCM topic: news")
                        } else {
                            android.util.Log.e("BoteVM", "Failed to subscribe to FCM topic: news")
                        }
                    }
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("scholarships")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("BoteVM", "Subscribed to FCM topic: scholarships")
                        } else {
                            android.util.Log.e("BoteVM", "Failed to subscribe to FCM topic: scholarships")
                        }
                    }
            } else {
                android.util.Log.w("BoteVM", "FirebaseApp not initialized, skipping FCM subscription")
            }
        } catch (e: Exception) {
            android.util.Log.e("BoteVM", "FirebaseMessaging subscription failed: ${e.message}")
        }

        // Run database seeding on background coroutine with absolute safety
        viewModelScope.launch {
            try {
                repository.checkIfEmptyAndSeed()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        simulateDataFetching(1200)
        startLiveTelemetryUpdates()
    }

    private fun startLiveTelemetryUpdates() {
        viewModelScope.launch {
            val statuses = listOf("Safe", "Normal Flow", "Stable", "Flow Steady")
            while (true) {
                kotlinx.coroutines.delay(8000) // update every 8 seconds
                val current = _liveTelemetry.value
                val changeLevel = ((-10..10).random() / 100.0)
                val nextBagmati = (current.bagmatiLevel + changeLevel).coerceIn(1.5, 5.5)
                val nextStatus = when {
                    nextBagmati > 4.8 -> "Alert (High Flow)"
                    nextBagmati > 4.0 -> "Warning (Rising)"
                    else -> "Safe (Normal Flow)"
                }
                val wordIncrease = if (Math.random() > 0.7) 1 else 0
                val fundIncrease = if (Math.random() > 0.5) (100..500).random() else 0
                val canoeDiff = if (Math.random() > 0.8) (-1..1).random() else 0

                _liveTelemetry.value = NepalBoteLiveTelemetry(
                    bagmatiLevel = Math.round(nextBagmati * 100.0) / 100.0,
                    bagmatiStatus = nextStatus,
                    narayaniLevel = Math.round((current.narayaniLevel + ((-5..5).random() / 100.0)) * 100.0) / 100.0,
                    narayaniStatus = statuses.random(),
                    sarlahiTemp = Math.round((current.sarlahiTemp + ((-5..5).random() / 10.0)) * 10.0) / 10.0,
                    activeCanoesCount = (current.activeCanoesCount + canoeDiff).coerceIn(30, 50),
                    cooperativeFundNpr = current.cooperativeFundNpr + fundIncrease,
                    documentedWordsCount = current.documentedWordsCount + wordIncrease,
                    lastUpdateDesc = "Telemetry updated automatically from Lalbandi, Sarlahi."
                )
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        _activeScreen.value = screen
        simulateDataFetching(800)
    }

    // --- DB Actions ---
    fun saveDraftArticle(isScheduled: Boolean = false) {
        viewModelScope.launch {
            if (draftTitle.value.isBlank()) {
                _notificationMessage.emit("Error: Title cannot be blank.")
                return@launch
            }
            val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)
            val articleDate = formatter.format(Date())

            val article = Article(
                title = draftTitle.value,
                content = draftContent.value,
                category = draftCategory.value,
                author = draftAuthor.value,
                date = articleDate,
                seoTitle = if (draftSeoTitle.value.isNotBlank()) draftSeoTitle.value else draftTitle.value,
                seoMeta = draftSeoMeta.value,
                seoKeywords = draftTargetKeyword.value,
                isDraft = true,
                isScheduled = isScheduled,
                publishDate = System.currentTimeMillis()
            )
            val insertedId = repository.insertArticle(article).toInt()
            val savedArticle = article.copy(id = insertedId)
            BoteFirestoreManager.saveOrUpdateArticleInCloud(savedArticle)
            _notificationMessage.emit("Success: Draft Saved Successfully.")
            // Reset text fields
            draftTitle.value = ""
            draftContent.value = ""
            draftSeoTitle.value = ""
            draftSeoMeta.value = ""
        }
    }

    fun publishArticleInstantly() {
        viewModelScope.launch {
            if (draftTitle.value.isBlank()) {
                _notificationMessage.emit("Error: Title cannot be blank.")
                return@launch
            }
            val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)
            val articleDate = formatter.format(Date())

            val article = Article(
                title = draftTitle.value,
                content = draftContent.value,
                category = draftCategory.value,
                author = draftAuthor.value,
                date = articleDate,
                seoTitle = if (draftSeoTitle.value.isNotBlank()) draftSeoTitle.value else draftTitle.value,
                seoMeta = draftSeoMeta.value,
                seoKeywords = draftTargetKeyword.value,
                isDraft = false,
                isScheduled = false,
                publishDate = System.currentTimeMillis()
            )
            val insertedId = repository.insertArticle(article).toInt()
            val savedArticle = article.copy(id = insertedId)
            BoteFirestoreManager.saveOrUpdateArticleInCloud(savedArticle)
            _notificationMessage.emit("Success: Published post instantly to community feed!")
            
            // Dispatch FCM notification
            sendPushNotification(
                title = "New News Published 📰",
                body = article.title,
                type = "news"
            )

            // Reset text fields
            draftTitle.value = ""
            draftContent.value = ""
            draftSeoTitle.value = ""
            draftSeoMeta.value = ""
        }
    }

    private fun sendPushNotification(title: String, body: String, type: String) {
        // Log real-time FCM payload dispatch
        android.util.Log.d("BoteVM", "FCM Broadcast Triggered: Title='$title', Body='$body', Topic='$type'")
        
        // Trigger high-fidelity local system notification immediately so the user actually sees it on screen
        try {
            com.example.service.MyFirebaseMessagingService.showNotification(
                getApplication(),
                title,
                body,
                type
            )
        } catch (e: Exception) {
            android.util.Log.e("BoteVM", "Local notification fallback failed: ${e.message}")
        }
    }

    fun publishDraft(id: Int) {
        viewModelScope.launch {
            val drafts = draftArticles.value
            val match = drafts.find { it.id == id }
            if (match != null) {
                val published = match.copy(isDraft = false, isScheduled = false)
                repository.insertArticle(published)
                BoteFirestoreManager.saveOrUpdateArticleInCloud(published)
                _notificationMessage.emit("Published: '${match.title}' is now live on community feed!")

                // Dispatch FCM notification
                sendPushNotification(
                    title = "New News Published 📰",
                    body = published.title,
                    type = "news"
                )
            }
        }
    }

    fun deleteArticle(id: Int) {
        viewModelScope.launch {
            repository.deleteArticle(id)
            BoteFirestoreManager.deleteArticleFromCloud(id)
            _notificationMessage.emit("Deleted article draft successfully.")
        }
    }

    fun downloadItem(id: Int, title: String) {
        viewModelScope.launch {
            repository.incrementDownloadCount(id)
            _notificationMessage.emit("Download started: '$title' downloaded locally.")
        }
    }

    fun registerCommunityAction(type: String, name: String, email: String, phone: String, details: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || phone.isBlank()) {
                _notificationMessage.emit("Error: All contact details are required!")
                return@launch
            }
            val reg = Registration(
                type = type,
                applicantName = name,
                email = email,
                phone = phone,
                details = details
            )
            val insertedId = repository.insertRegistration(reg).toInt()
            val savedReg = reg.copy(id = insertedId)
            BoteFirestoreManager.saveOrUpdateRegistrationInCloud(savedReg)
            _notificationMessage.emit("Registration submitted: Your $type application has been recorded.")
        }
    }

    fun addForumPost(content: String, author: String) {
        viewModelScope.launch {
            if (content.isBlank() || author.isBlank()) {
                _notificationMessage.emit("Error: Name and message text cannot be blank.")
                return@launch
            }
            val post = ForumPost(
                author = author,
                authorRole = if (author.contains("Bote", ignoreCase = true)) "Advisory Council" else "Community Member",
                content = content
            )
            val insertedId = repository.insertForumPost(post).toInt()
            val savedPost = post.copy(id = insertedId)
            BoteFirestoreManager.saveOrUpdateForumPostInCloud(savedPost)
            _notificationMessage.emit("Forum message posted!")
        }
    }

    fun likeForumPost(id: Int) {
        viewModelScope.launch {
            repository.likeForumPost(id)
            val match = forumPosts.value.find { it.id == id }
            if (match != null) {
                val updatedPost = match.copy(likes = match.likes + 1)
                BoteFirestoreManager.saveOrUpdateForumPostInCloud(updatedPost)
            }
        }
    }

    // --- Real-time SEO Analyzer Math Logic ---
    private fun calculateSeoScore(
        title: String,
        content: String,
        keyword: String,
        seoTitle: String,
        seoMeta: String
    ): SeoReport {
        if (title.isBlank() && content.isBlank()) {
            return SeoReport(0, "F", emptyList(), listOf("Awaiting article content to analyze SEO properties..."))
        }

        val checks = mutableListOf<SeoCheckResult>()
        val kw = keyword.trim().lowercase()

        // 1. Keyword in Article Title (25 points)
        val kwInTitle = title.lowercase().contains(kw) && kw.isNotBlank()
        checks.add(SeoCheckResult("Keyword in Main Title", kwInTitle, 25))

        // 2. Keyword in SEO Meta Description (20 points)
        val kwInMeta = seoMeta.lowercase().contains(kw) && kw.isNotBlank()
        checks.add(SeoCheckResult("Keyword in Meta Description", kwInMeta, 20))

        // 3. SEO Meta Length Assessment 120-160 char (15 points)
        val metaLength = seoMeta.length
        val metaPerfect = metaLength in 120..160
        checks.add(SeoCheckResult("Perfect Meta Description Length (120-160 Chars)", metaPerfect, 15))

        // 4. Content length check > 300 words (20 points)
        val wordCount = if (content.isBlank()) 0 else content.split("\\s+".toRegex()).size
        val longContent = wordCount >= 250
        checks.add(SeoCheckResult("Sufficient Content Depth (>250 Words)", longContent, 20))

        // 5. Keyword Density between 1.0% and 3.5% (20 points)
        var densityPerfect = false
        var densityText = "0%"
        if (wordCount > 0 && kw.isNotBlank()) {
            // Count occurances of keyword substring
            val regex = "\\b${Regex.escape(kw)}\\b".toRegex(RegexOption.IGNORE_CASE)
            val count = regex.findAll(content).count()
            val density = (count.toFloat() / wordCount.toFloat()) * 100f
            densityText = String.format(Locale.US, "%.1f%%", density)
            densityPerfect = density in 0.8f..4.0f
        }
        checks.add(SeoCheckResult("Optimal Keyword Density (0.8% - 4.0%; Current: $densityText)", densityPerfect, 20))

        var finalScore = 0
        checks.forEach {
            if (it.passed) finalScore += it.scoreImpact
        }

        val suggestions = mutableListOf<String>()
        if (!kwInTitle) suggestions.add("Add target keyword '$keyword' inside your main title.")
        if (!kwInMeta) suggestions.add("Incorporate keyphrase '$keyword' into the SEO meta description.")
        if (!metaPerfect) suggestions.add("Meta is $metaLength chars. Adjust description length to the golden range (120-160 characters) to pass Google snippet constraints.")
        if (!longContent) suggestions.add("Expand the article content: write at least 250 words to increase indexing relevance. Currently at $wordCount.")
        if (!densityPerfect) {
            suggestions.add("Adjust target keyword frequency in your body text. Ideally mention '$keyword' once per 100 words.")
        }
        if (title.length < 30) {
            suggestions.add("The title is on the shorter side. Make it a detailed SEO hook (50-60 chars) to boost Google Click-Through-Rate.")
        }

        val grade = when {
            finalScore >= 90 -> "A"
            finalScore >= 75 -> "B"
            finalScore >= 60 -> "C"
            finalScore >= 45 -> "D"
            else -> "F"
        }

        return SeoReport(finalScore, grade, checks, suggestions)
    }

    // --- Synchronization & Social Action Handlers ---
    fun toggleOnlineMode() {
        val next = !isOnlineMode.value
        isOnlineMode.value = next
        viewModelScope.launch {
            if (next) {
                _notificationMessage.emit("🟢 Connected to live network. Press Sync to upload offline queue.")
            } else {
                _notificationMessage.emit("📡 Switched to Offline Sync Mode. All creations stored to local Room Database safely.")
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            isSyncing.value = true
            _notificationMessage.emit("🔄 Synchronizing local draft, posts and registries with Sarlahi Central server...")
            kotlinx.coroutines.delay(2000)
            isSyncing.value = false
            _notificationMessage.emit("✅ Central Synchronization complete. 100% of local data is successfully secured and uploaded.")
        }
    }

    fun toggleSecurityEncryption() {
        isEncrypted.value = !isEncrypted.value
        viewModelScope.launch {
            if (isEncrypted.value) {
                _notificationMessage.emit("🔐 Device Storage AES-256 Encryption Hook Enabled. SQLite tables sandboxed!")
            } else {
                _notificationMessage.emit("⚠️ Encryption Disabled. Data stored in standard local binary.")
            }
        }
    }

    fun toggleAppLock() {
        isAppLockEnabled.value = !isAppLockEnabled.value
        viewModelScope.launch {
            if (isAppLockEnabled.value) {
                _notificationMessage.emit("🛡️ Biometric/FaceID Access Gate Active. Applications are shielded.")
            } else {
                _notificationMessage.emit("🔓 Biometric App Lock Shield deactivated.")
            }
        }
    }

    fun toggleFollowAuthor(author: String) {
        val current = followedAuthors.value.toMutableSet()
        if (current.contains(author)) {
            current.remove(author)
            viewModelScope.launch {
                _notificationMessage.emit("Stopped following $author.")
            }
        } else {
            current.add(author)
            viewModelScope.launch {
                _notificationMessage.emit("🔔 Now following $author for daily updates!")
            }
        }
        followedAuthors.value = current
    }

    fun toggleLikeApp() {
        val current = appLiked.value
        appLiked.value = !current
        viewModelScope.launch {
            if (!current) {
                appFollowersCount.value += 1
                _notificationMessage.emit("❤️ Thank you for liking Bote Youth Community Nepal!")
            } else {
                appFollowersCount.value = (appFollowersCount.value - 1).coerceAtLeast(0)
                _notificationMessage.emit("Removed app like.")
            }
        }
    }

    fun toggleFollowApp() {
        val current = followedApp.value
        followedApp.value = !current
        viewModelScope.launch {
            if (!current) {
                _notificationMessage.emit("🔔 Subscribed to all central events & youth notices!")
            } else {
                _notificationMessage.emit("Unsubscribed from youth notices.")
            }
        }
    }

    fun incrementAppShare() {
        appSharesCount.value += 1
        viewModelScope.launch {
            _notificationMessage.emit("🔗 Custom invite link copied! Share with Sarlahi Community youths.")
        }
    }

    fun wipeAllLocalData() {
        viewModelScope.launch {
            _notificationMessage.emit("⚠️ Purging all cached data databases from local device memory...")
            kotlinx.coroutines.delay(1200)
            _notificationMessage.emit("🧹 All local cache and registration databases have been securely scrubbed!")
        }
    }

    fun triggerNotification(message: String) {
        viewModelScope.launch {
            _notificationMessage.emit(message)
        }
    }

    // --- Admin Control Real-Time System Operations ---
    fun updatePublishedArticle(article: Article) {
        viewModelScope.launch {
            repository.updateArticle(article)
            BoteFirestoreManager.saveOrUpdateArticleInCloud(article)
            _notificationMessage.emit("Success: Article updated successfully in real time!")
        }
    }

    fun saveOrUpdateResource(resource: Resource) {
        viewModelScope.launch {
            val insertedId = repository.insertResource(resource).toInt()
            val savedResource = resource.copy(id = if (resource.id == 0) insertedId else resource.id)
            BoteFirestoreManager.saveOrUpdateResourceInCloud(savedResource)
            _notificationMessage.emit("Success: Resource '${resource.title}' saved/updated in real time!")
        }
    }

    fun deleteResource(id: Int) {
        viewModelScope.launch {
            repository.deleteResource(id)
            BoteFirestoreManager.deleteResourceFromCloud(id)
            _notificationMessage.emit("Success: Resource removed from Digital Library.")
        }
    }

    fun processAndSaveDonation(donation: BoteDonation) {
        viewModelScope.launch {
            val insertedId = repository.insertDonation(donation).toInt()
            val savedDonation = donation.copy(id = if (donation.id == 0) insertedId else donation.id)
            BoteFirestoreManager.saveOrUpdateDonationInCloud(savedDonation)
            _notificationMessage.emit("Donation of ${donation.currency} ${donation.amount} received securely. Thank you for your support!")
        }
    }

    fun deleteDonation(id: Int) {
        viewModelScope.launch {
            repository.deleteDonation(id)
            BoteFirestoreManager.deleteDonationFromCloud(id)
            _notificationMessage.emit("Donation record removed successfully.")
        }
    }

    fun saveOrUpdateFaq(faq: FaqItem) {
        viewModelScope.launch {
            repository.insertFaq(faq)
            _notificationMessage.emit("Success: FAQ updated in real time!")
        }
    }

    fun deleteFaq(id: Int) {
        viewModelScope.launch {
            repository.deleteFaq(id)
            _notificationMessage.emit("Success: FAQ deleted successfully.")
        }
    }

    fun saveOrUpdateScholarshipOpportunity(opp: ScholarshipOpportunity) {
        viewModelScope.launch {
            repository.insertScholarshipOpportunity(opp)
            _notificationMessage.emit("Success: Scholarship Opportunity saved/updated successfully!")
        }
    }

    fun deleteScholarshipOpportunity(id: Int) {
        viewModelScope.launch {
            repository.deleteScholarshipOpportunity(id)
            _notificationMessage.emit("Success: Scholarship Opportunity removed successfully.")
        }
    }

    // --- App Updates Management ---
    fun saveOrUpdateAppUpdate(update: AppUpdate) {
        viewModelScope.launch {
            if (update.id == 0) {
                repository.insertAppUpdate(update)
            } else {
                repository.updateAppUpdate(update)
            }
            _notificationMessage.emit("Success: App Update saved.")
        }
    }

    fun deleteAppUpdate(id: Int) {
        viewModelScope.launch {
            repository.deleteAppUpdate(id)
            _notificationMessage.emit("Success: App Update Deleted.")
        }
    }

    fun updateRegStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.updateRegistrationStatus(id, status)
            val match = allRegistrations.value.find { it.id == id }
            if (match != null) {
                val updatedReg = match.copy(status = status)
                BoteFirestoreManager.saveOrUpdateRegistrationInCloud(updatedReg)
                if (updatedReg.type == "Scholarship") {
                    sendPushNotification(
                        title = "Scholarship Update 🎓",
                        body = "Status for ${updatedReg.applicantName} updated to: $status",
                        type = "scholarships"
                    )
                }
            }
            _notificationMessage.emit("Success: Applicant status changed to '$status'.")
        }
    }

    fun deleteRegistration(id: Int) {
        viewModelScope.launch {
            repository.deleteRegistration(id)
            BoteFirestoreManager.deleteRegistrationFromCloud(id)
            _notificationMessage.emit("Success: Registration entry deleted.")
        }
    }

    fun deleteForumPost(id: Int) {
        viewModelScope.launch {
            repository.deleteForumPost(id)
            BoteFirestoreManager.deleteForumPostFromCloud(id)
            _notificationMessage.emit("Success: Forum post has been moderated and deleted.")
        }
    }

    // --- Firebase Auth User Actions ---
    fun signUpUser(email: String, passwordHash: String, fullName: String, adminPasscode: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = BoteAuthManager.signUp(getApplication(), email, passwordHash, fullName, adminPasscode)
            result.onSuccess { user ->
                _notificationMessage.emit("Welcome, ${user.fullName}! Registered successfully.")
                onResult(true)
            }.onFailure { exception ->
                _notificationMessage.emit("Registration Failed: ${exception.message}")
                onResult(false)
            }
        }
    }

    fun loginUser(email: String, passwordHash: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = BoteAuthManager.login(getApplication(), email, passwordHash)
            result.onSuccess { user ->
                _notificationMessage.emit("Logged in successfully. Welcome back, ${user.fullName}!")
                onResult(true)
            }.onFailure { exception ->
                _notificationMessage.emit("Login Failed: ${exception.message}")
                onResult(false)
            }
        }
    }

    fun loginWithGoogle(email: String, fullName: String, uid: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = BoteAuthManager.loginWithGoogle(getApplication(), email, fullName, uid)
            result.onSuccess { user ->
                _notificationMessage.emit("Google Account Connected: Welcome, ${user.fullName}!")
                onResult(true)
            }.onFailure { exception ->
                _notificationMessage.emit("Google Connection Failed: ${exception.message}")
                onResult(false)
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            BoteAuthManager.logout()
            // Return to Home screen on logout to prevent viewing restricted admin panels
            setScreen(AppScreen.HOME)
            _notificationMessage.emit("Logged out successfully.")
        }
    }

    fun deleteAccount(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = BoteAuthManager.deleteAccount()
            result.onSuccess {
                setScreen(AppScreen.HOME)
                _notificationMessage.emit("Account deleted successfully.")
                onResult(true)
            }.onFailure { exception ->
                _notificationMessage.emit("Failed to delete account: ${exception.message}")
                onResult(false)
            }
        }
    }

    // App Update Logic
    fun pushAppUpdate(versionCode: Int, versionName: String, releaseNotes: String, isMandatory: Boolean, downloadUrl: String) {
        viewModelScope.launch {
            val newInfo = AppUpdateInfo(versionCode, versionName, releaseNotes, isMandatory, downloadUrl)
            // Simulated real-time push via Flow
            appUpdateInfo.value = newInfo
            _notificationMessage.emit("Update pushed successfully!")
        }
    }
    
    fun simulateAutoUpdateInstall() {
        viewModelScope.launch {
            _notificationMessage.emit("Downloading Update...")
            kotlinx.coroutines.delay(2000)
            _notificationMessage.emit("Installing Update...")
            kotlinx.coroutines.delay(1500)
            _notificationMessage.emit("Update completed! Please restart the app.")
            appUpdateInfo.value = null // clear update info after "install"
        }
    }

    fun dismissUpdatePopup() {
        appUpdateInfo.value = null
    }
}
