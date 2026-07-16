package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BoteRepository(private val boteDao: BoteDao) {

    // --- Exposures (Flows) ---
    val publishedArticles: Flow<List<Article>> = boteDao.getPublishedArticles()
    val draftArticles: Flow<List<Article>> = boteDao.getDraftArticles()
    val allResources: Flow<List<Resource>> = boteDao.getAllResources()
    val allFaqs: Flow<List<FaqItem>> = boteDao.getAllFaqs()
    val allRegistrations: Flow<List<Registration>> = boteDao.getAllRegistrations()
    val forumPosts: Flow<List<ForumPost>> = boteDao.getForumPosts()
    val allScholarshipOpportunities: Flow<List<ScholarshipOpportunity>> = boteDao.getAllScholarshipOpportunities()
    val allAppUpdates: Flow<List<AppUpdate>> = boteDao.getAllAppUpdates()
    val allDonations: Flow<List<BoteDonation>> = boteDao.getAllDonations()

    fun getFaqsByCategory(category: String): Flow<List<FaqItem>> = boteDao.getFaqsByCategory(category)
    fun getResourcesByCategory(category: String): Flow<List<Resource>> = boteDao.getResourcesByCategory(category)

    // --- Write Operations ---
    suspend fun insertArticle(article: Article): Long = boteDao.insertArticle(article)
    suspend fun updateArticle(article: Article) = boteDao.updateArticle(article)
    suspend fun deleteArticle(id: Int) = boteDao.deleteArticleById(id)

    suspend fun insertResource(resource: Resource): Long = boteDao.insertResource(resource)
    suspend fun updateResource(resource: Resource) = boteDao.updateResource(resource)
    suspend fun deleteResource(id: Int) = boteDao.deleteResourceById(id)
    suspend fun incrementDownloadCount(id: Int) = boteDao.incrementDownloadCount(id)

    suspend fun insertRegistration(registration: Registration): Long = boteDao.insertRegistration(registration)
    suspend fun updateRegistrationStatus(id: Int, status: String) = boteDao.updateRegistrationStatus(id, status)
    suspend fun deleteRegistration(id: Int) = boteDao.deleteRegistrationById(id)

    suspend fun insertForumPost(post: ForumPost): Long = boteDao.insertForumPost(post)
    suspend fun likeForumPost(id: Int) = boteDao.likeForumPost(id)
    suspend fun deleteForumPost(id: Int) = boteDao.deleteForumPostById(id)

    suspend fun insertFaq(faq: FaqItem) = boteDao.insertFaq(faq)
    suspend fun updateFaq(faq: FaqItem) = boteDao.updateFaq(faq)
    suspend fun deleteFaq(id: Int) = boteDao.deleteFaqById(id)

    suspend fun insertScholarshipOpportunity(opp: ScholarshipOpportunity): Long = boteDao.insertScholarshipOpportunity(opp)
    suspend fun updateScholarshipOpportunity(opp: ScholarshipOpportunity) = boteDao.updateScholarshipOpportunity(opp)
    suspend fun deleteScholarshipOpportunity(id: Int) = boteDao.deleteScholarshipOpportunityById(id)

    // App Updates
    suspend fun insertAppUpdate(update: AppUpdate): Long = boteDao.insertAppUpdate(update)
    suspend fun updateAppUpdate(update: AppUpdate) = boteDao.updateAppUpdate(update)
    suspend fun deleteAppUpdate(id: Int) = boteDao.deleteAppUpdateById(id)

    // Donations
    suspend fun insertDonation(donation: BoteDonation): Long = boteDao.insertDonation(donation)
    suspend fun deleteDonation(id: Int) = boteDao.deleteDonationById(id)

    // --- Database Seeding Routine ---
    suspend fun checkIfEmptyAndSeed() {
        val count = publishedArticles.firstOrNull()?.size ?: 0
        if (count == 0) {
            seedArticles()
            seedResources()
            seedFaqs()
            seedForumPosts()
            seedScholarshipOpportunities()
            seedDonations()
        }
    }

    private suspend fun seedArticles() {
        val currentDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())

        val seeds = listOf(
            Article(
                title = "Preserving the Wisdom of the River: An Interview with Elder Mangal Bote",
                category = "Bote Culture and Traditions",
                author = "Karmahiya Documentation Team",
                date = currentDate,
                content = """Karmahiya, Sarlahi – As the waters of the Bagmati river flow gently past our ancestors' shores, 84-year-old Elder Mangal Bote sits on his wooden canoe, pointing at the river bend. 'For centuries, our nets fed the valley, and our boats connected brothers. Today, the youth must balance modern learning with these ancient strokes.'

In this extensive interview, Elder Mangal highlights traditional fishing practices, sustainable aquatic ecology, and Bote river mythology. He recalls how they built giant dugout canoes from Sal logs without a single iron nail, using only water-resistant plant resins. 'The river is not a highway—it is a mother. We never took more fish than we needed. We spoke to the water before throwing the net.' 

As river dredging and commercial fishing put pressure on traditional ways, the elder urges community youth to lead local eco-tourism and homestay initiatives to protect and leverage this heritage. He hopes this digital archive will keep our river heritage alive for generations to come.""",
                seoTitle = "History of Bote Community Sarlahi: Wisdom of River Elders",
                seoMeta = "Read an intellectual interview with 84-year-old Bote Elder Mangal Bote on river traditions, boat building, and organic fishing in Sarlahi, Nepal.",
                seoKeywords = "Bote History, Bote Elder Interview, Karmahiya Sarlahi, Traditional Fishing, River Heritage"
            ),
            Article(
                title = "The Bote Language Project: Recording Our Ancient Phonetic Vocabulary",
                category = "Bote Language Preservation",
                author = "Prof. Ramchandra Bote",
                date = currentDate,
                content = """The Bote language is classified as an endangered Sino-Tibetan or Indo-Aryan branch local variant, spoken primevally by the Bote families of the Madhesh and Gandaki water basins. With less than 3,000 fluent native speakers recorded in the latest censuses, we are racing against time.

Our language preservation committee, partnered with Tribhuvan University, is documenting critical phonetic structures. For instance:
- 'Shawa' translates to water.
- 'Nauni' translates to fish.
- 'Dhoni' represents the primary wooden boat.
- 'Jhal' is our traditional circular net.

We have mapped over 450 unique riverine and botanical terms that do not exist in Nepali or Maithili. By embedding these words into youth workshops and digital school textbooks, we are crafting a strong foundation for indigenous bilingual curriculum preservation. We invite youth leaders to participate in transcription workshops every Saturday at the Karmahiya Community Center.""",
                seoTitle = "Bote Language Preservation Nepal: Vocabulary & Archive Project",
                seoMeta = "Discover the endangered Bote language. Learn about the phonetic mapping project, grammar, and essential tribal vocabulary in Madhesh Province, Nepal.",
                seoKeywords = "Bote language preservation, indigenous dialect, Tribhuvan University, bilingual education, Karmahiya"
            ),
            Article(
                title = "Karmahiya Opens First Riverfront Homestays to Support Local Families",
                category = "Homestay Tourism",
                author = "Sita Bote, Cooperative Lead",
                date = "May 25, 2026",
                content = """Sarlahi – Under the newly established Sarlahi Homestay Committee, four traditional Bote households in Karmahiya, Sarlahi have opened their doors to domestic and international travelers. This milestone marks the first indigenous community-led tourism hub in the Madhesh Province river belt.

Visitors will experience authentic riverine living. The standard homestay itinerary includes:
1. Traditional Bote meals consisting of river-caught snail soup, roasted dry fish ('sukuti'), and millet flatbread.
2. Boat rowing excursions on traditional wooden canoes guided by local youth.
3. Cultural evening performances featuring traditional folk music, Badhai dances, and oral storytelling around the bonfire.

'This program keeps our culture alive and provides a direct, dignified livelihood for our women and youth,' says Sita Bote. The local government has provided hospitality, hygiene, and language communication training to our cooperative members to ensure high-quality hosting standards. Bookings are handled directly through our hospitality register panel.""",
                seoTitle = "Bote Homestay Tourism Sarlahi: Authentic River Living",
                seoMeta = "Book a cultural Bote homestay in Karmahiya, Sarlahi. Support indigenous ecotourism, experience canoe rowing, and enjoy organic tribal cuisine.",
                seoKeywords = "Bote homestay, Sarlahi tourism, Madhesh Province, river eco-tourism, indigenous homestay"
            ),
            Article(
                title = "Empowering Bote Women Through Handcrafts and Small-Scale Banking",
                category = "Women's Empowerment",
                author = "Uma Bote, Women Lead",
                date = "May 18, 2026",
                content = """The Bote Women Savings Cooperative of Karmahiya has grown from 12 members to over 85 active participants. By initiating micro-finance and small loans, women are now running local businesses, organic vegetable nurseries, and traditional handcraft production workshops.

Using water reeds ('Khar') and river weeds, the women weave elaborate decorative baskets, traditional fishing traps ('Koki'), and dining mats. These products are being purchased by tourism hotels in Janakpur and Bardibas, showing the vast potential of local indigenous arts.

'We are no longer just household workers; we are financial anchors of our community,' says Uma Bote. Microfinance has also funded six school scholarships for girls who otherwise faced early dropouts. The cooperative plans to showcase products in the Madhesh Indigenous Fair next month.""",
                seoTitle = "Bote Women Empowerment: Cooperatives & Handcrafts Sarlahi",
                seoMeta = "See how the Bote Women Savings Cooperative in Sarlahi is transforming lives through microfinance, reed weaving, and river craft micro-enterprises.",
                seoKeywords = "Bote Women Empowerment, microfinance, indigenous handicrafts, Karmahiya cooperatives, Sarlahi"
            )
        )

        seeds.forEach { boteDao.insertArticle(it) }
    }

    private suspend fun seedResources() {
        val seeds = listOf(
            Resource(
                title = "National Socio-Demographic Profile of Bote Community (2021 Census Analysis)",
                category = "Census Info",
                resourceType = "PDF",
                author = "Nepal Central Bureau of Statistics",
                year = "2021",
                fileSize = "2.4 MB",
                downloadCount = 142,
                description = "Comprehensive sociological and economic census details mapping population density, literacy indicators, and occupational transitions of Bote families across Nepal."
            ),
            Resource(
                title = "The Linguistic Heritage and Phonetics of Tarahi Bote",
                category = "Research Paper",
                resourceType = "Document",
                author = "Prof. K. B. Adhikari, Tribhuvan University",
                year = "2024",
                fileSize = "1.8 MB",
                downloadCount = 89,
                description = "An academic linguistic research paper documenting Bote grammatical structure, noun classes, and phonology in Sarlahi, Madhesh Province."
            ),
            Resource(
                title = "Bote River Heritage: Indigenous Ecological Knowledge of Bagmati River",
                category = "PDF Report",
                resourceType = "PDF",
                author = "International Centre for Integrated Mountain Development (ICIMOD)",
                year = "2023",
                fileSize = "3.2 MB",
                downloadCount = 205,
                description = "Environmental study detailing how the Bote people's fishing gear and water conservation practices actively prevent ecosystem depletion in the Bagmati river basins."
            ),
            Resource(
                title = "Oral History Collection: Audio Recordings of Tribal Legends",
                category = "Oral History",
                resourceType = "Audio",
                author = "Bote Cultural Documentation Center",
                year = "2025",
                fileSize = "15.4 MB",
                downloadCount = 67,
                description = "High-quality audio recordings accompanied by Nepali translations capturing elders telling the historic tales of the legendary Bote kings and river protectors."
            ),
            Resource(
                title = "Karmahiya Municipal Study on Homestay Tourism Potential",
                category = "Research Paper",
                resourceType = "Document",
                author = "Sarlahi Tourism Development Board",
                year = "2025",
                fileSize = "1.2 MB",
                downloadCount = 51,
                description = "Economic feasibility study analyzing infrastructure, target tourist routes, and micro-loan models for developing eco-friendly tribal homestays in Sarlahi."
            ),
            Resource(
                title = "Origins of the Bote Janajati: Archaic Migrations and Riverine Settlements",
                category = "Research Paper",
                resourceType = "PDF",
                author = "Nepal Academy of Social Sciences",
                year = "2023",
                fileSize = "4.1 MB",
                downloadCount = 312,
                description = "An in-depth ethnographical study tracking the ancestral migration of Bote communities along the Narayani and Rapti river corridors, detailing their pre-modern social institutions."
            ),
            Resource(
                title = "Traditional Boat-Building and Ferrying Technology of the Bote Tribe",
                category = "Research Paper",
                resourceType = "Document",
                author = "Anthropological Society of Nepal",
                year = "2022",
                fileSize = "1.5 MB",
                downloadCount = 194,
                description = "Documentation of the unique, eco-friendly carpentry and navigational techniques developed by Bote boat builders to carve dugout canoes from single tree trunks."
            ),
            Resource(
                title = "Gold Panning (Sunaula Search) in Nepal's River Sands: Economic Realities of Botes",
                category = "Research Paper",
                resourceType = "PDF",
                author = "Department of Mines and Geology, Nepal",
                year = "2024",
                fileSize = "2.8 MB",
                downloadCount = 277,
                description = "A specialized socio-economic study investigating the traditional gold extraction methods (Sunaula) practiced by Bote families in riverbeds and its current economic viability."
            ),
            Resource(
                title = "Bote Language Revitalization Plan: Preserving a Highly Threatened Tongue",
                category = "PDF Report",
                resourceType = "PDF",
                author = "Linguistic Survey of Nepal (LinSuN)",
                year = "2025",
                fileSize = "3.0 MB",
                downloadCount = 168,
                description = "A collaborative report mapping the remaining fluent speakers of the Bote language and laying out an educational framework for bilingual mother-tongue schooling."
            )
        )

        seeds.forEach { boteDao.insertResource(it) }
    }

    private suspend fun seedFaqs() {
        val seeds = listOf(
            FaqItem(
                question = "Who are the Bote people of Nepal and what is their heritage?",
                answer = "The Bote are an ancient, highly marginalized indigenous riverine community of Nepal, traditionally residing along the large river basins of the Madhesh and Gandaki Provinces, such as the Bagmati, Narayani, and Rapti. For centuries, they have lived as master boat rowers, canoe builders, and sustainable fishermen.",
                pageCategory = "Bote History"
            ),
            FaqItem(
                question = "Where is the Tarahi Bote Community specifically located?",
                answer = "The Tarahi Bote Community is primarily settled in Karmahiya, Lalbandi/Sarlahi, Madhesh Province, Nepal. They live in close compact settlements near the river systems, keeping ancient traditions alive while developing modern community networks.",
                pageCategory = "Bote Community of Sarlahi"
            ),
            FaqItem(
                question = "What are the unique properties of the Bote language?",
                answer = "The Bote language is a non-written, highly endangered tribal dialect. It is heavily rich in aquatic, riverine, and ecological vocabulary. Currently, it is classified as critically endangered due to the small number of native speakers and lack of formal school scripts.",
                pageCategory = "Bote Language"
            ),
            FaqItem(
                question = "What is the 'Badhai' festival celebrated by Botes?",
                answer = "Badhai is the signature festival of the Bote people, typically celebrated at the onset of monsoon and fishing season. Botes worship the water gods, clean their canoes, perform ancestral dances, and make offerings to the river to ensure safety from floods and a rich fish harvest.",
                pageCategory = "Bote Festivals"
            ),
            FaqItem(
                question = "Can international guests visit the homestays in Karmahiya?",
                answer = "Yes! The Tarahi Bote Homestay in Karmahiya, Sarlahi welcomes international guests. It offers clean, hygienic modern bedrooms inside traditional mud-brick huts, local ethnic dining, and daily guided boat rowing tours. You can register interest directly via the app.",
                pageCategory = "Bote Homestays"
            ),
            FaqItem(
                question = "What initiatives are in place of empowerment for Bote women?",
                answer = "The community has established the Bote Women's Savings and Credit Cooperative. This initiative empowers women by providing micro-credits to build reed-weaving stalls, promote local homestay hosting, and finance educational scholarships for young girls.",
                pageCategory = "Bote Women Empowerment"
            )
        )

        seeds.forEach { boteDao.insertFaq(it) }
    }

    private suspend fun seedForumPosts() {
        val seeds = listOf(
            ForumPost(
                author = "Ram Bahadur Bote",
                authorRole = "Youth Coordinator",
                content = "Welcome to the Tarahi Bote community digital forum! Let's use this space to share language lessons, coordinate our homestay rosters, and plan the upcoming Badhai river festival in Sarlahi.",
                likes = 12
            ),
            ForumPost(
                author = "Priya Bote",
                authorRole = "Student Advocate",
                content = "Does anyone have information regarding when the applications open for the 2026 secondary school scholarships? I have two sisters in Karmahiya who want to apply.",
                likes = 8
            )
        )

        seeds.forEach { boteDao.insertForumPost(it) }
    }

    private suspend fun seedScholarshipOpportunities() {
        val seeds = listOf(
            ScholarshipOpportunity(
                title = "Bote Higher Education IT Scholarship",
                description = "Funding support for computer science, hardware training, or software engineering certification courses.",
                amount = "NPR 25,000 / student",
                deadline = "October 15, 2026",
                requirements = "Open to high school graduate Bote youth in Sarlahi, demonstrating academic interest."
            ),
            ScholarshipOpportunity(
                title = "Primary & Secondary School girls Incentive",
                description = "Textbooks, school bag, uniforms, and fee support designed to reduce young girls dropout rate.",
                amount = "NPR 12,000 / student",
                deadline = "November 1, 2026",
                requirements = "Bote female students enrolled in Grades 1-10 in Karmahiya or Lalbandi public schools."
            ),
            ScholarshipOpportunity(
                title = "Indigenous Crafts & Tourism Apprentice Fund",
                description = "Vocational scholarship matching apprentices with master reed weavers and homestay coordinators.",
                amount = "NPR 18,000 / student",
                deadline = "September 30, 2026",
                requirements = "Bote youth (ages 16-25) interested in preserving traditional river craftsmanship."
            )
        )
        seeds.forEach { boteDao.insertScholarshipOpportunity(it) }
    }

    private suspend fun seedDonations() {
        val seeds = listOf(
            BoteDonation(
                donorName = "Arjun Bote",
                email = "arjun@example.com",
                amount = 2500.0,
                targetCause = "Tarahi Higher Education Scholarship Fund",
                paymentMethod = "eSewa",
                timestamp = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                message = "Wishing the best for our hard-working students in Sarlahi!",
                status = "Verified"
            ),
            BoteDonation(
                donorName = "Sita Maya Bote",
                email = "sitamaya@example.com",
                amount = 5000.0,
                targetCause = "Bote Traditional Boat-building Preservation",
                paymentMethod = "Khalti",
                timestamp = System.currentTimeMillis() - 86400000 * 1, // 1 day ago
                message = "Preserving our ancestors' canoe craftsmanship.",
                status = "Verified"
            ),
            BoteDonation(
                donorName = "Anonymous",
                email = "anon@example.com",
                amount = 10000.0,
                targetCause = "Tarahi Community Library & IT Center",
                paymentMethod = "Card Payment",
                timestamp = System.currentTimeMillis() - 3600000 * 4, // 4 hours ago
                message = "To support youth digital literacy and laptop accessibility.",
                status = "Verified"
            )
        )
        seeds.forEach { boteDao.insertDonation(it) }
    }
}
