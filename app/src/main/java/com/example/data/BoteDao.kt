package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoteDao {
    // --- Articles ---
    @Query("SELECT * FROM articles WHERE isDraft = 0 ORDER BY publishDate DESC")
    fun getPublishedArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isDraft = 1 ORDER BY publishDate DESC")
    fun getDraftArticles(): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article): Long

    @Update
    suspend fun updateArticle(article: Article)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticleById(id: Int)

    // --- Resources ---
    @Query("SELECT * FROM resources ORDER BY id DESC")
    fun getAllResources(): Flow<List<Resource>>

    @Query("SELECT * FROM resources WHERE category = :category ORDER BY id DESC")
    fun getResourcesByCategory(category: String): Flow<List<Resource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: Resource): Long

    @Update
    suspend fun updateResource(resource: Resource)

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun deleteResourceById(id: Int)

    @Query("UPDATE resources SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: Int)

    // --- FAQs ---
    @Query("SELECT * FROM faqs ORDER BY id DESC")
    fun getAllFaqs(): Flow<List<FaqItem>>

    @Query("SELECT * FROM faqs WHERE pageCategory = :category ORDER BY id DESC")
    fun getFaqsByCategory(category: String): Flow<List<FaqItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaq(faq: FaqItem)

    @Update
    suspend fun updateFaq(faq: FaqItem)

    @Query("DELETE FROM faqs WHERE id = :id")
    suspend fun deleteFaqById(id: Int)

    // --- Registrations ---
    @Query("SELECT * FROM registrations ORDER BY timestamp DESC")
    fun getAllRegistrations(): Flow<List<Registration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: Registration): Long

    @Query("UPDATE registrations SET status = :status WHERE id = :id")
    suspend fun updateRegistrationStatus(id: Int, status: String)

    @Query("DELETE FROM registrations WHERE id = :id")
    suspend fun deleteRegistrationById(id: Int)

    // --- Forum Posts ---
    @Query("SELECT * FROM forum_posts ORDER BY timestamp DESC")
    fun getForumPosts(): Flow<List<ForumPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForumPost(post: ForumPost): Long

    @Query("UPDATE forum_posts SET likes = likes + 1 WHERE id = :id")
    suspend fun likeForumPost(id: Int)

    @Query("DELETE FROM forum_posts WHERE id = :id")
    suspend fun deleteForumPostById(id: Int)

    // --- Users ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): BoteUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: BoteUser)

    // --- Scholarship Opportunities ---
    @Query("SELECT * FROM scholarship_opportunities ORDER BY timestamp DESC")
    fun getAllScholarshipOpportunities(): Flow<List<ScholarshipOpportunity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScholarshipOpportunity(opp: ScholarshipOpportunity): Long

    @Update
    suspend fun updateScholarshipOpportunity(opp: ScholarshipOpportunity)

    @Query("DELETE FROM scholarship_opportunities WHERE id = :id")
    suspend fun deleteScholarshipOpportunityById(id: Int)

    // --- App Updates ---
    @Query("SELECT * FROM app_updates ORDER BY timestamp DESC")
    fun getAllAppUpdates(): Flow<List<AppUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUpdate(update: AppUpdate): Long

    @Update
    suspend fun updateAppUpdate(update: AppUpdate)

    @Query("DELETE FROM app_updates WHERE id = :id")
    suspend fun deleteAppUpdateById(id: Int)

    // --- Donations ---
    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAllDonations(): Flow<List<BoteDonation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: BoteDonation): Long

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun deleteDonationById(id: Int)
}
