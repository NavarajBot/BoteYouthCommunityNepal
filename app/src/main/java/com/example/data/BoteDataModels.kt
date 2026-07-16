package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String,
    val author: String,
    val date: String,
    val imageUrl: String = "",
    val seoTitle: String = "",
    val seoMeta: String = "",
    val seoKeywords: String = "",
    val isDraft: Boolean = false,
    val isScheduled: Boolean = false,
    val publishDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "resources")
data class Resource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // PDF Report, Research Paper, Historical Document, Census Info, Photograph, Oral History
    val resourceType: String, // PDF, Image, Audio, Document
    val author: String,
    val year: String,
    val fileSize: String,
    val downloadCount: Int = 0,
    val description: String,
    val contentUrl: String = ""
)

@Entity(tableName = "faqs")
data class FaqItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val answer: String,
    val pageCategory: String // Bote History, Bote Language, Bote Culture, Festivals, Homestays, etc.
)

@Entity(tableName = "registrations")
data class Registration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // Volunteer, Scholarship, Event
    val applicantName: String,
    val email: String,
    val phone: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Under Review"
)

@Entity(tableName = "forum_posts")
data class ForumPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author: String,
    val authorRole: String = "Member",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)

@Entity(tableName = "users")
data class BoteUser(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val fullName: String,
    val role: String, // "standard" or "admin"
    val uid: String = java.util.UUID.randomUUID().toString()
)

@Entity(tableName = "scholarship_opportunities")
data class ScholarshipOpportunity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val amount: String,
    val deadline: String,
    val requirements: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_updates")
data class AppUpdate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val version: String,
    val title: String,
    val releaseNotes: String,
    val isMandatory: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "donations")
data class BoteDonation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val donorName: String,
    val email: String,
    val amount: Double,
    val currency: String = "NPR",
    val targetCause: String,
    val paymentMethod: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val status: String = "Verified"
)

