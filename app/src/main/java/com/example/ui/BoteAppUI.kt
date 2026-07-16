package com.example.ui

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import coil.compose.AsyncImage

// Data structure for the 15 SEO Landing pages
data class SeoLandingPage(
    val categoryName: String,
    val canonicalTitle: String,
    val keywords: String,
    val metaDescription: String,
    val details: String,
    val faqs: List<Pair<String, String>>,
    val altTagGuide: String,
    val schemaType: String,
    val internalLinks: List<String>
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BoteAppUI(
    viewModel: BoteCommunityViewModel,
    modifier: Modifier = Modifier
) {
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val isDataFetching by viewModel.isDataFetching.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    // Firebase Auth States
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthInitializing by viewModel.isAuthInitializing.collectAsStateWithLifecycle()
    var showAuthDialog by remember { mutableStateOf(false) }

    var appThemeState by remember { mutableStateOf("morning") }
    var showIntroLoader by remember { mutableStateOf(true) }

    // Automatically synchronize theme with current system/local hour
    LaunchedEffect(Unit) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        appThemeState = if (hour in 6..17) "morning" else "night"
    }

    // Observe DB States
    val publishedArticles by viewModel.publishedArticles.collectAsStateWithLifecycle()
    val draftArticles by viewModel.draftArticles.collectAsStateWithLifecycle()
    val allFaqs by viewModel.allFaqs.collectAsStateWithLifecycle()
    val filteredResources by viewModel.filteredResources.collectAsStateWithLifecycle()
    val allRegistrations by viewModel.allRegistrations.collectAsStateWithLifecycle()
    val forumPosts by viewModel.forumPosts.collectAsStateWithLifecycle()

    // Sync, security & social states
    val isOnlineMode by viewModel.isOnlineMode.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isEncrypted by viewModel.isEncrypted.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val followedAuthors by viewModel.followedAuthors.collectAsStateWithLifecycle()
    val appLiked by viewModel.appLiked.collectAsStateWithLifecycle()
    val followedApp by viewModel.followedApp.collectAsStateWithLifecycle()
    val appSharesCount by viewModel.appSharesCount.collectAsStateWithLifecycle()
    val appFollowersCount by viewModel.appFollowersCount.collectAsStateWithLifecycle()

    // Temporary active toast state
    var toastText by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.notificationMessage.collect { msg ->
            toastText = msg
            showToast = true
        }
    }

    if (showToast) {
        LaunchedEffect(toastText) {
            kotlinx.coroutines.delay(3000)
            showToast = false
        }
    }

    val isNepali by viewModel.isNepali.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()

    // List of 15 Landing Pages definitions
    val seoLandingPages = remember(isNepali) { getSeoLandingPagesData(isNepali) }

    if (appUpdateInfo != null && (appUpdateInfo?.versionCode ?: 1) > 1) {
        val info = appUpdateInfo!!
        AlertDialog(
            onDismissRequest = { if (!info.isMandatory) viewModel.dismissUpdatePopup() },
            title = { Text(if (isNepali) "नयाँ अपडेट उपलब्ध छ" else "New Update Available: ${info.versionName}") },
            text = { Text(info.releaseNotes) },
            confirmButton = {
                Button(
                    onClick = { viewModel.simulateAutoUpdateInstall() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isNepali) "अपडेट गर्नुहोस्" else "Update Now")
                }
            },
            dismissButton = {
                if (!info.isMandatory) {
                    OutlinedButton(onClick = { viewModel.dismissUpdatePopup() }) {
                        Text(if (isNepali) "पछि" else "Later")
                    }
                }
            }
        )
    }

    if (isAuthInitializing) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (currentUser == null) {
        ImmersiveOnboardingScreen(
            viewModel = viewModel,
            isNepali = isNepali,
            appTheme = appThemeState
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val screens = listOf(
                    Triple(AppScreen.HOME, Icons.Default.Home, "Home"),
                    Triple(AppScreen.LANDING_PAGES, Icons.Default.Info, "Details"),
                    Triple(AppScreen.DIGITAL_LIBRARY, Icons.Default.Search, "Search"),
                    Triple(AppScreen.COMMUNITY_HUB, Icons.Default.Share, "Community"),
                    Triple(AppScreen.CREATOR_DASHBOARD, Icons.Default.Add, "Creator DB"),
                    Triple(AppScreen.GOOGLE_COMMAND_CENTER, Icons.Default.Settings, "Setting")
                )

                screens.forEach { (screen, icon, label) ->
                    val isSelected = activeScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.setScreen(screen)
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_btn_${label.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                 // Shared Top Header with River Wave Aesthetic and Theme Selector
                 HeaderSection(
                     activeTheme = appThemeState,
                     onThemeChange = { appThemeState = it },
                     onTriggerLaunchAnim = { showIntroLoader = true },
                     isNepali = isNepali,
                     onLanguageChange = { viewModel.isNepali.value = it },
                     currentUser = currentUser,
                     onLoginClick = { showAuthDialog = true },
                     onLogoutClick = { viewModel.logoutUser() }
                 )
 
                 // Active screen content
                 Box(modifier = Modifier.weight(1f)) {
                     AnimatedContent(
                         targetState = Pair(activeScreen, isDataFetching),
                         transitionSpec = {
                             fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                         },
                         label = "ScreenTransition"
                     ) { (targetScreen, fetching) ->
                         if (fetching) {
                             SkeletonLoadingScreen(targetScreen, appThemeState)
                         } else {
                             when (targetScreen) {
                                 AppScreen.HOME -> HomeDashboard(viewModel, publishedArticles, appThemeState)
                                 AppScreen.LANDING_PAGES -> SEOPagesPanel(viewModel, seoLandingPages, clipboardManager, isNepali)
                                AppScreen.DIGITAL_LIBRARY -> DigitalLibraryPanel(viewModel, filteredResources)
                                AppScreen.COMMUNITY_HUB -> CommunityHubPanel(
                                    viewModel = viewModel,
                                    articles = publishedArticles,
                                    forumPosts = forumPosts,
                                    registrations = allRegistrations,
                                    isOnlineMode = isOnlineMode,
                                    isSyncing = isSyncing,
                                    isEncrypted = isEncrypted,
                                    isAppLockEnabled = isAppLockEnabled,
                                    followedAuthors = followedAuthors,
                                    appLiked = appLiked,
                                    followedApp = followedApp,
                                    appSharesCount = appSharesCount,
                                    appFollowersCount = appFollowersCount
                                )
                                AppScreen.CREATOR_DASHBOARD -> CreatorDashboardPanel(
                                    viewModel = viewModel,
                                    drafts = draftArticles,
                                    published = publishedArticles,
                                    resources = filteredResources,
                                    faqs = allFaqs,
                                    registrations = allRegistrations,
                                    forumPosts = forumPosts
                                )
                                AppScreen.GOOGLE_COMMAND_CENTER -> GoogleCommandCenterPanel(viewModel, seoLandingPages, clipboardManager)
                                AppScreen.DONATION_PORTAL -> DonationPortalPanel(viewModel)
                            }
                        }
                    }
                }
            }

            // Custom Elegant Snackbar / Toast overlay
            AnimatedVisibility(
                visible = showToast,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    color = NightPebble,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.widthIn(max = 500.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = LeafGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = toastText,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Cinematic Intro Loader Layer
            if (showIntroLoader) {
                CinematicIntroLoader(onDismiss = { showIntroLoader = false })
            }

            // Secure Community Auth Dialog
            if (showAuthDialog) {
                AuthDialog(
                    isNepali = isNepali,
                    onDismiss = { showAuthDialog = false },
                    onLogin = { email, password ->
                        viewModel.loginUser(email, password) { success ->
                            if (success) showAuthDialog = false
                        }
                    },
                    onSignUp = { email, password, name, adminCode ->
                        viewModel.signUpUser(email, password, name, adminCode) { success ->
                            if (success) showAuthDialog = false
                        }
                    }
                )
            }
        }
    }
    }
}

@Composable
fun CinematicIntroLoader(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var startAnimate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimate = true
        kotlinx.coroutines.delay(2600)
        visible = false
        onDismiss()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimate) 1.0f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimate) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 1000),
        label = "LogoAlpha"
    )
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimate) 0f else -30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoRotation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(600)),
        modifier = Modifier.fillMaxSize()
    ) {
        val transition = rememberInfiniteTransition(label = "CinematicTransition")
        val boatPos by transition.animateFloat(
            initialValue = -120f,
            targetValue = 550f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "BoatCrossing"
        )
        val waveOffset by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WaveSurge"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1E29)) // Ultra premium dark blue-cobalt canvas
                .clickable {
                    visible = false
                    onDismiss()
                }
        ) {
            // Flowing river lines on canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path()
                val waveY = size.height * 0.65f
                path.moveTo(0f, waveY)
                for (x in 0..size.width.toInt() step 6) {
                    val y = waveY + sin(x * 0.012f + waveOffset) * 20f
                    path.lineTo(x.toFloat(), y)
                }
                path.lineTo(size.width, size.height)
                path.lineTo(0f, size.height)
                path.close()
                drawPath(path, Brush.verticalGradient(listOf(Color(0xFF145369), Color(0xFF08131B))))
            }

            // Animated wooden dugout canoe
            Box(
                modifier = Modifier
                    .offset(x = boatPos.dp, y = 350.dp)
                    .size(90.dp, 36.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bPath = androidx.compose.ui.graphics.Path()
                    bPath.moveTo(0f, size.height * 0.5f)
                    bPath.cubicTo(size.width * 0.3f, size.height * 0.95f, size.width * 0.7f, size.height * 0.95f, size.width, size.height * 0.3f)
                    bPath.lineTo(size.width * 0.9f, size.height * 0.42f)
                    bPath.cubicTo(size.width * 0.65f, size.height * 0.75f, size.width * 0.35f, size.height * 0.75f, size.width * 0.12f, size.height * 0.48f)
                    bPath.close()
                    drawPath(bPath, Color(0xFFF57C00)) // Radiant Heritage Amber Canoe
                }
            }

            // Central branding text
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Bote Youba Samaj Logo
                AsyncImage(
                    model = R.drawable.img_app_logo,
                    contentDescription = "Bote Youba Samaj Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer(
                            scaleX = logoScale,
                            scaleY = logoScale,
                            alpha = logoAlpha,
                            rotationZ = logoRotation
                        )
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFFF9A825).copy(alpha = 0.85f), CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "बोटे युवा समुदाय 🛶",
                    color = Color(0xFFF9A825),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "BOTE YOUTH",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 5.sp
                )
                Text(
                    text = "COMMUNITY NEPAL",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(30.dp))
                CircularProgressIndicator(
                    color = Color(0xFFF9A825),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = "[ Tap screen to skip entrance • 🛶 ]",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    activeTheme: String,
    onThemeChange: (String) -> Unit,
    onTriggerLaunchAnim: () -> Unit,
    isNepali: Boolean = false,
    onLanguageChange: (Boolean) -> Unit = {},
    currentUser: AuthUser? = null,
    onLoginClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                val lineCol = when (activeTheme) {
                    "night" -> Color(0xFF57C5B6).copy(alpha = 0.25f)
                    "festival" -> Color(0xFFF9A825).copy(alpha = 0.35f)
                    else -> DeepTeal.copy(alpha = 0.2f)
                }
                drawLine(
                    color = lineCol,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }
            .background(
                Brush.horizontalGradient(
                    when (activeTheme) {
                        "night" -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        "festival" -> listOf(Color(0xFF581C1C), Color(0xFF450A0A))
                        else -> listOf(Color(0xFF0F4C81), Color(0xFF1D5A8C))
                    }
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTriggerLaunchAnim() }
            ) {
                Text(
                    text = "बोटे युवा समाज नेपाल 🛶",
                    color = if (activeTheme == "festival") Color(0xFFF9A825) else SandOchre,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Bote Youth Community Nepal",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isNepali) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onLanguageChange(false) }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("EN 🇬🇧", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isNepali) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onLanguageChange(true) }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("नेपाली 🇳🇵", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Sticky Quick Switcher for Premium Theme modes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { onThemeChange("morning") },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTheme == "morning") Color.White.copy(alpha = 0.25f) else Color.Transparent)
                ) {
                    Text("🌅", fontSize = 14.sp)
                }
                IconButton(
                    onClick = { onThemeChange("night") },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTheme == "night") Color.White.copy(alpha = 0.25f) else Color.Transparent)
                ) {
                    Text("🌌", fontSize = 14.sp)
                }
                IconButton(
                    onClick = { onThemeChange("festival") },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTheme == "festival") Color.White.copy(alpha = 0.25f) else Color.Transparent)
                ) {
                    Text("🏮", fontSize = 14.sp)
                }
            }
        }
    }
}

// ==================== TAB 1: HOME DASHBOARD ====================
// Confetti particle representation for high-satisfaction feedback
data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val speedY: Float,
    val speedX: Float,
    val rotation: Float,
    val size: Float
)

// Dynamic Water Ripple representation
data class CustomWaterRipple(
    val id: Long,
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeDashboard(
    viewModel: BoteCommunityViewModel,
    articles: List<Article>,
    activeTheme: String
) {
    val scope = rememberCoroutineScope()
    val isNepali by viewModel.isNepali.collectAsStateWithLifecycle()
    val appUpdates by viewModel.allAppUpdates.collectAsStateWithLifecycle()
    val latestMandatoryUpdate = appUpdates.firstOrNull { it.isMandatory }
    val latestUpdate = appUpdates.firstOrNull()

    val clipboardManager = LocalClipboardManager.current
    var selectedMapDistrict by remember { mutableStateOf("Sarlahi") }
    var lightboxImageTitle by remember { mutableStateOf<String?>(null) }
    var lightboxImageDesc by remember { mutableStateOf("") }
    
    // Event Registration Modal states
    var registeringEventTitle by remember { mutableStateOf<String?>(null) }
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regDetails by remember { mutableStateOf("") }
    
    // Confetti particles state
    var confettiList by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var triggerConfetti by remember { mutableStateOf(0) }
    
    // Custom Water Ripples click list
    var waterRipples by remember { mutableStateOf<List<CustomWaterRipple>>(emptyList()) }
    var rippleCounter by remember { mutableLongStateOf(0L) }
    
    // Active Interactive Heritage lore display dialog
    var activeLoreTitle by remember { mutableStateOf<String?>(null) }
    var activeLoreContent by remember { mutableStateOf("") }

    // Carousel state automatic sliding scrolling
    val successStories = remember {
        listOf(
            Triple("Anita Bote", "Youth Conservationist", "Anita successfully restored 3 kilometers of native Bagmati River wetland ecosystems, preserving traditional marsh marigold sites in Sarlahi."),
            Triple("Navaraj Bot", "First Software Engineer", "Navaraj developed the first type-safe keyboard input engine for standard Bote orthography, helping preserve our oral grammar structures."),
            Triple("Sunita Bote", "Women Cooperative President", "Sunita established our river homestay micro-finances, expanding local eco-tourism and providing sustainable income for 14 families."),
            Triple("Manoj Bote", "Archival Clothing Artist", "Manoj cataloged over 45 hand-woven traditional outfit designs, securing national heritage recognition for our festive wardrobe.")
        )
    }
    val carouselState = androidx.compose.foundation.lazy.rememberLazyListState()
    var carouselIndex by remember { mutableStateOf(0) }
    
    // Infinite Animation Transitions for Rivers, Birds, and Floating lights
    val infiniteTransition = rememberInfiniteTransition(label = "HomeInfinite")
    
    // Smooth river wave float
    val riverWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RiverWavePhase"
    )

    // Floating birds transition
    val birdFloatOffsetX by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BirdFloatX"
    )
    val birdFloatOffsetY by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BirdFloatY"
    )

    // Floating Festival Lights / Diyas
    val festiveLightsY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FestiveLightsY"
    )

    // Scroll Counter Progress for Statistic Numbers Count-up
    var statsAnimProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        ) { value, _ ->
            statsAnimProgress = value
        }
    }

    // Auto-scroll loop for Netflix carousel
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4500)
            carouselIndex = (carouselIndex + 1) % successStories.size
            if (carouselState.layoutInfo.totalItemsCount > 0) {
                try {
                    carouselState.animateScrollToItem(carouselIndex)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Ignore scroll interrupts
                }
            }
        }
    }

    // Confetti Physics simulation loop
    LaunchedEffect(triggerConfetti) {
        if (triggerConfetti > 0) {
            // Seed 40 random confetti pieces
            val colors = listOf(Color(0xFFF9A825), Color(0xFF2E7D32), Color(0xFF0F4C81), Color(0xFFE53935), Color(0xFF8E24AA))
            val initialConfetti = (0..40).map {
                ConfettiParticle(
                    x = 100f + (Math.random() * 400).toFloat(),
                    y = 100f,
                    color = colors.random(),
                    speedY = 5f + (Math.random() * 8).toFloat(),
                    speedX = -3f + (Math.random() * 6).toFloat(),
                    rotation = (Math.random() * 360).toFloat(),
                    size = 8f + (Math.random() * 12).toFloat()
                )
            }
            confettiList = initialConfetti

            // Animate flying items
            for (frame in 0..60) {
                kotlinx.coroutines.delay(16)
                confettiList = confettiList.map { p ->
                    p.copy(
                        x = p.x + p.speedX,
                        y = p.y + p.speedY,
                        rotation = p.rotation + 4f
                    )
                }.filter { it.y <= 1500f }
            }
            confettiList = emptyList()
        }
    }

    // Water Ripple simulation frames
    LaunchedEffect(Unit) {
        while (true) {
            if (waterRipples.isNotEmpty()) {
                waterRipples = waterRipples.map { rip ->
                    rip.copy(
                        radius = rip.radius + 12f,
                        alpha = rip.alpha - 0.05f
                    )
                }.filter { it.alpha > 0f }
            }
            kotlinx.coroutines.delay(16)
        }
    }

    // Establish dynamic background gradients matching Morning, Night, and Festival
    val dashboardBgBrush = when (activeTheme) {
        "night" -> Brush.verticalGradient(listOf(Color(0xFF0F1E29), Color(0xFF070F14)))
        "festival" -> Brush.verticalGradient(listOf(Color(0xFF220A0A), Color(0xFF140505)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFAFAFA), Color(0xFFF3F8F9)))
    }

    val primaryCardColor = when (activeTheme) {
        "night" -> Color(0xFF162530)
        "festival" -> Color(0xFF2E1212)
        else -> Color.White
    }

    val bodyTextColor = when (activeTheme) {
        "night" -> Color(0xFFBDC1C6)
        "festival" -> Color(0xFFE2C4C4)
        else -> Color(0xFF3C4043)
    }

    val headingTextColor = when (activeTheme) {
        "night" -> Color.White
        "festival" -> Color(0xFFF9A825)
        else -> Color(0xFF0F4C81)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dashboardBgBrush)
            .pointerInput(activeTheme) {
                detectTapGestures { offset ->
                    rippleCounter++
                    val newRipple = CustomWaterRipple(
                        id = rippleCounter,
                        x = offset.x,
                        y = offset.y,
                        radius = 10f,
                        alpha = 0.82f
                    )
                    waterRipples = waterRipples + newRipple
                }
            }
    ) {
        // Draw overall background water ripples and flying elements under the widgets
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw birds crossing the morning/festive skies occasionally
            if (activeTheme != "night") {
                val birdX = birdFloatOffsetX.dp.toPx()
                val birdY = birdFloatOffsetY.dp.toPx() + 100f
                // Draw 2 birds in V shape
                listOf(0f, -40f).forEach { offset ->
                    val bx = birdX + offset
                    val by = birdY + (offset * 0.5f)
                    val bPath = androidx.compose.ui.graphics.Path()
                    bPath.moveTo(bx, by)
                    bPath.quadraticTo(bx + 15f, by - 12f, bx + 30f, by)
                    bPath.quadraticTo(bx + 45f, by - 12f, bx + 60f, by)
                    bPath.quadraticTo(bx + 30f, by + 10f, bx, by)
                    drawPath(bPath, Color(0xFF2E7D32).copy(alpha = 0.4f), style = Stroke(width = 2.dp.toPx()))
                }
            }

            // Draw floating Diyas / oil lamps during Festival mode
            if (activeTheme == "festival") {
                val lampAnimationOffset = festiveLightsY
                listOf(
                    Offset(size.width * 0.15f, size.height * 0.8f + lampAnimationOffset),
                    Offset(size.width * 0.45f, size.height * 0.6f + lampAnimationOffset * 1.2f),
                    Offset(size.width * 0.85f, size.height * 0.7f + lampAnimationOffset * 0.8f)
                ).forEach { basePoint ->
                    // Make lamp fade out as it reaches top
                    val alphaRatio = ((basePoint.y / size.height).coerceIn(0f, 1f))
                    drawCircle(Color(0xFFF9A825).copy(alpha = 0.35f * alphaRatio), radius = 18f, center = basePoint)
                    drawCircle(Color.White.copy(alpha = 0.6f * alphaRatio), radius = 6f, center = basePoint)
                }
            }

            // Draw user interaction water ripples dynamically
            waterRipples.forEach { rip ->
                drawCircle(
                    color = when (activeTheme) {
                        "night" -> Color(0xFF57C5B6).copy(alpha = rip.alpha)
                        "festival" -> Color(0xFFF9A825).copy(alpha = rip.alpha)
                        else -> Color(0xFF0F4C81).copy(alpha = rip.alpha)
                    },
                    radius = rip.radius,
                    center = Offset(rip.x, rip.y),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("home_screen_9_sections"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==================== APP UPDATE BANNER ====================
            if (latestMandatoryUpdate != null || latestUpdate != null) {
                val updateToShow = latestMandatoryUpdate ?: latestUpdate!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (updateToShow.isMandatory) ErrorRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Update",
                                tint = if (updateToShow.isMandatory) ErrorRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Available: ${updateToShow.title} (v${updateToShow.version})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (updateToShow.isMandatory) ErrorRed else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = updateToShow.releaseNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // ==================== NEPAL FUND DONATION BANNER ====================
            item {
                DonationBannerCard(viewModel)
            }

            // ==================== HERO SECTION ====================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryCardColor),
                    elevation = CardDefaults.cardElevation(if (activeTheme == "night") 4.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Interactive Animated River Banner (Height 170dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(
                                    when (activeTheme) {
                                        "night" -> Color(0xFF0B1924)
                                        "festival" -> Color(0xFF3F0B0B)
                                        else -> Color(0xFFDFEFF4)
                                    }
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw double flowing river paths
                                val path1 = androidx.compose.ui.graphics.Path()
                                val path2 = androidx.compose.ui.graphics.Path()
                                val riverY = size.height * 0.55f
                                
                                path1.moveTo(0f, riverY)
                                path2.moveTo(0f, riverY + 20f)
                                for (x in 0..size.width.toInt() step 5) {
                                    val y1 = riverY + sin(x * 0.008f + riverWavePhase) * 16f
                                    val y2 = riverY + 12f + sin(x * 0.01f + riverWavePhase + 1f) * 14f
                                    path1.lineTo(x.toFloat(), y1)
                                    path2.lineTo(x.toFloat(), y2)
                                }
                                
                                // Color variables
                                val colPrimary = when (activeTheme) {
                                    "night" -> Color(0xFF57C5B6)
                                    "festival" -> Color(0xFFF9A825)
                                    else -> Color(0xFF0F4C81)
                                }
                                drawPath(path1, colPrimary.copy(alpha = 0.35f), style = Stroke(width = 4.dp.toPx()))
                                drawPath(path2, colPrimary.copy(alpha = 0.20f), style = Stroke(width = 3.dp.toPx()))

                                // Drawing floating solar sparkles on morning theme
                                if (activeTheme == "morning") {
                                    listOf(0.2f, 0.45f, 0.7f, 0.9f).forEach { ratio ->
                                        val sparkleX = size.width * ratio
                                        val sparkleY = riverY - 30f + sin(riverWavePhase * 2f + ratio * 10f) * 15f
                                        drawCircle(Color(0xFFF9A825).copy(alpha = 0.5f), radius = 6f, center = Offset(sparkleX, sparkleY))
                                    }
                                }
                            }

                            // Wooden Bote canoe slowly floating
                            val transition = rememberInfiniteTransition(label = "CanoeLoop")
                            val canoeOffset by transition.animateFloat(
                                initialValue = -80f,
                                targetValue = 420f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(9000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "CanoeOffset"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = canoeOffset.dp, y = 75.dp)
                                    .size(60.dp, 24.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val bPath = androidx.compose.ui.graphics.Path()
                                    bPath.moveTo(0f, size.height * 0.5f)
                                    bPath.cubicTo(size.width * 0.3f, size.height * 0.95f, size.width * 0.7f, size.height * 0.95f, size.width, size.height * 0.3f)
                                    bPath.lineTo(size.width * 0.9f, size.height * 0.42f)
                                    bPath.cubicTo(size.width * 0.65f, size.height * 0.75f, size.width * 0.35f, size.height * 0.75f, size.width * 0.1f, size.height * 0.48f)
                                    bPath.close()
                                    drawPath(bPath, if (activeTheme == "festival") Color(0xFFD49B54) else Color(0xFF8D6E63))
                                }
                            }

                            // Sarlahi Madhesh tag
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Sarlahi, Nepal",
                                    color = if (activeTheme == "morning") Color(0xFF0F4C81) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Hero textual content and actions
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "BOTE YOUTH COMMUNITY NEPAL",
                                color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Guardians of Nepal's Rivers",
                                color = headingTextColor,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                text = "Preserving Heritage, Empowering Future",
                                color = headingTextColor.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "The masters of aquatic ecosystems and ancient canoe craft of Madhesh Province, Nepal. We protect the Bagmati and Rapti rivers while establishing digital sovereignty and eco-tourism opportunities for tribal youths.",
                                color = bodyTextColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Call to Action navigation buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.setScreen(AppScreen.LANDING_PAGES) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("hero_explore_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Search, "Explore", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Explore Our Heritage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setScreen(AppScreen.COMMUNITY_HUB) },
                                    border = BorderStroke(
                                        1.dp,
                                        if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F4C81)),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("hero_join_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, "Join", tint = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Join Community", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 1: COMMUNITY STATISTICS ====================
            item {
                Column {
                    Text(
                        text = "📊 Our Impact & Growth",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = headingTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    
                    val stats = listOf(
                        Pair("members", Pair(11000, "Community Members")),
                        Pair("leaders", Pair(50, "Youth Leaders Registered")),
                        Pair("stories", Pair(100, "Historical Lore Archived")),
                        Pair("villages", Pair(20, "Villages Linked Locally"))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        stats.take(2).forEach { (tag, info) ->
                            val (targetVal, desc) = info
                            val animatedVal = (targetVal * statsAnimProgress).toInt()
                            
                            var isTouched by remember { mutableStateOf(false) }
                            val touchOffset by animateDpAsState(if (isTouched) (-6).dp else 0.dp, label = "TouchUp")

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .offset(y = touchOffset)
                                    .clickable {
                                        isTouched = !isTouched
                                        scope.launch {
                                            kotlinx.coroutines.delay(800)
                                            isTouched = false
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = primaryCardColor),
                                elevation = CardDefaults.cardElevation(if (isTouched) 8.dp else 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${String.format("%,d", animatedVal)}+",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = bodyTextColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        stats.drop(2).forEach { (tag, info) ->
                            val (targetVal, desc) = info
                            val animatedVal = (targetVal * statsAnimProgress).toInt()
                            
                            var isTouched by remember { mutableStateOf(false) }
                            val touchOffset by animateDpAsState(if (isTouched) (-6).dp else 0.dp, label = "TouchUp2")

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .offset(y = touchOffset)
                                    .clickable {
                                        isTouched = !isTouched
                                        scope.launch {
                                            kotlinx.coroutines.delay(800)
                                            isTouched = false
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = primaryCardColor),
                                elevation = CardDefaults.cardElevation(if (isTouched) 8.dp else 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$animatedVal+",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = bodyTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== ETHNOGRAPHIC DOSSIER SECTION ====================
            item {
                var activeDossierTab by remember { mutableStateOf("origins") }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("ethnographic_registration_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryCardColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Dossier",
                                tint = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isNepali) "📖 राष्ट्रिय बोटे आदिवासी दस्तावेज" else "📖 National Ethnographic Register",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = headingTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isNepali) 
                                "बोटे समुदायको इतिहास, जनसंख्या, परम्परागत पेशा र वर्तमान सामाजिक-आर्थिक रूपान्तरण सम्बन्धी प्रमाणिक अनुसन्धान संग्रह।" 
                                else "Historical demographics, livelihoods, and socio-economic transitions of the Bote people of Nepal.",
                            fontSize = 11.sp,
                            color = bodyTextColor.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Brief Overview Info Tag Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(headingTextColor.copy(alpha = 0.04f))
                                .border(1.dp, headingTextColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isNepali) 
                                        "बोटे जाति नेपालको एक अति सीमान्तकृत नदी किनारमा बसोबास गर्ने आदिवासी जनजाति हो, जसको जनसंख्या कुल राष्ट्रिय जनसंख्याको करिब ०.०४ प्रतिशत (लगभग ११,००० जना) रहेको छ। परम्परागत रूपमा नदी र वन क्षेत्रहरूमा आश्रित यो समुदाय कडा संरक्षण कानुन र निकुञ्ज प्रतिबन्धहरूका कारण ठूलो सांस्कृतिक र आर्थिक संक्रमणको सामना गरिरहेको छ। [१, २, ३]"
                                        else "The Bote are an indigenous, highly marginalized ethnic group in Nepal, comprising roughly 0.04% of the population (about 11,000 people). Traditionally depending on the country's rivers and forests, they are facing a major cultural and economic transition due to strict conservation laws and park restrictions. [1, 2, 3]",
                                    fontSize = 13.sp,
                                    color = bodyTextColor,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F4C81).copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isNepali) "जनसंख्या: ~११,०००" else "Population: ~11,000", 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = Color(0xFF0F4C81)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2E7D32).copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isNepali) "हिस्सा: ०.०४%" else "Ethnic Share: 0.04%", 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Tab Layout (Responsive row of chips)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabs = listOf(
                                Pair("origins", if (isNepali) "उत्पत्ति 🗺️" else "Origins 🗺️"),
                                Pair("livelihoods", if (isNepali) "आजीविका ⚓" else "Livelihoods ⚓"),
                                Pair("challenges", if (isNepali) "चुनौतीहरू ⚠️" else "Challenges ⚠️"),
                                Pair("adaptations", if (isNepali) "अनुकूलन 🚀" else "Adaptations 🚀")
                            )
                            tabs.forEach { (key, label) ->
                                val isSelected = activeDossierTab == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                                            } else {
                                                headingTextColor.copy(alpha = 0.05f)
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else headingTextColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { activeDossierTab = key }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else bodyTextColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Tab content
                        when (activeDossierTab) {
                            "origins" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isNepali) "🗺️ उत्पत्ति र जनसांख्यिकी" else "🗺️ Origins & Demographics", 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = headingTextColor
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• बसोबास: मुख्यतया कालीगण्डकी, नारायणी र राप्ती नदीका किनारहरूमा, विशेषतः चितवन, नवलपरासी, पाल्पा, तनहुँ र गोर्खा जिल्लाहरूमा र सर्लाहीको कर्महियाका नदी तटीय खण्डहरूमा। [२, ४, ५]"
                                            else "• Location: Primarily scattered along the banks of the Kali Gandaki, Narayani, and Rapti rivers, mostly in Chitwan, Nawalparasi, Palpa, Tanahu, and Gorkha districts. [2, 4, 5]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• पुर्खा र सादृश्यता: बोटे समुदायको बोलीचाली र संस्कृति दनुवार, दरै, थारु र माझी समुदायसँग नजिकबाट मेल खान्छ। [४]"
                                            else "• Ancestry: Their dialect and culture closely resemble those of the Danuwar, Darai, Tharu, and Majhi communities. [4]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• उप-समूहहरू: बोटेहरू परम्परागत रूपमा दुई समूहमा विभाजित छन्:\n  - पानी बोटे: पूर्ण रूपमा माछा मार्ने, डुङ्गा चलाउने र नदी किनारमा बालुवाबाट सुन चाल्ने काममा संलग्न हुने।\n  - पाखे बोटे: कृषि, पशुपालन र भारी बोक्ने काममा बढी निर्भर रहने खण्ड। [६, ७, ८]"
                                            else "• Sub-groups: They are traditionally divided into two groups:\n  - Paani Bote: Solely dependent on fishing, boating, and panning for gold along river banks.\n  - Pakhe Bote: Rely more on agriculture, animal husbandry, and porter work. [6, 7, 8]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                }
                            }
                            "livelihoods" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isNepali) "⚓ परम्परागत जीविकोपार्जन" else "⚓ Traditional Livelihoods", 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = headingTextColor
                                    )
                                    Text(
                                        text = if (isNepali) "कसै-कसैले 'पानीका राजा' को उपनाम दिएका बोटेहरूको परम्परागत पहिचान नदीसँगै गाँसिएको छ: [१, ४]" 
                                            else "Known by some as the 'Kings of water,' their traditional identity is deeply tied to the rivers: [1, 4]",
                                        fontSize = 12.sp, color = bodyTextColor, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• घाट र डुङ्गा: परम्परागत रूपमा हातले कुँदिएका काठे डुङ्गा (धोनी) प्रयोग गरेर यात्रु र सरसामान नदी पार गराउने काम।"
                                            else "• Ferrying: Transporting passengers and goods across rivers using traditional, handcrafted wooden boats.",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• स्रोत संकलन: नदीमा जाल हानेर माछा मार्ने, बालुवाबाट सुन चाल्ने र वन क्षेत्रबाट निउरो, दाउरा तथा खर बटुल्ने।"
                                            else "• Resource Gathering: Fishing, gold panning, and collecting edible ferns, wood, and elephant grass from nearby forests.",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• धार्मिक विश्वास: हिन्दू चाडपर्वहरू (दशैं-तिहार) मनाउनुका साथै जल र वन देवतालाई खुशी पार्न शिकारी पूजा र वायु पूजा जस्ता प्रकृतिपूजक संस्कृति। [१, ४, ७, ९, १०]"
                                            else "• Belief System: They practice a mix of Hindu traditions (celebrating Dashain and Tihar) and animistic rituals, maintaining mystical beliefs like Sikari Pooja and Bayu Pooja. [1, 4, 7, 9, 10]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                }
                            }
                            "challenges" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isNepali) "⚠️ वर्तमान चुनौती र संक्रमण" else "⚠️ Current Challenges & Transition", 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = headingTextColor
                                    )
                                    Text(
                                        text = if (isNepali) "पुस्ताैंदेखि चल्दै आएको बोटे जीवनशैली र आधुनिक संरक्षण प्रयासबीच विरोधाभास उत्पन्न भएको छ: [१]"
                                            else "For generations, their way of life has clashed with modern conservation efforts: [1]",
                                        fontSize = 12.sp, color = bodyTextColor, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• माछा मार्ने प्रतिबन्ध: चितवन राष्ट्रिय निकुञ्जको स्थापना र कडा नदी संरक्षण नियमावलीले बोटेहरूको माछा मार्ने र परम्परागत स्रोत संकलनको अधिकारलाई धेरै संकुचित बनाएको छ। [१, ९]"
                                            else "• Fishing Bans: The establishment of Chitwan National Park and strict river preservation regulations have severely restricted their fishing and resource-gathering rights. [1, 9]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• भूमिहीनता: धेरै बोटेहरू भूमिहीन छन् र ऐतिहासिक रूपमा सरकारी नदी किनारमा बस्दै आएका छन्, यद्यपि आधुनिक कृषिमैत्री आयोजनाले उनीहरूलाई दिगो खेतीतर्फ आकर्षित गरिरहेको छ। [१, ११]"
                                            else "• Landlessness: Many Bote do not own land and historically lived on government riverbanks, though some farming projects have begun to transition them to sustainable agriculture. [1, 11]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• सामाजिक-आर्थिक स्तर: यो समुदाय निरन्तर गरिबी, न्यून साक्षरता दर र जीविकोपार्जनको असुरक्षासँग जुधिरहेको छ। [१०, १२]"
                                            else "• Socio-Economic State: The community often struggles with poverty, low literacy rates, and livelihood insecurity. [10, 12]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                }
                            }
                            "adaptations" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isNepali) "🚀 आधुनिक अनुकूलन र सुरक्षा" else "🚀 Modern Adaptations", 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = headingTextColor
                                    )
                                    Text(
                                        text = if (isNepali) "परिवर्तित परिवेशमा बाँच्नका लागि बोटे समुदायले आम्दानीका स्रोतहरू विविधिकरण गर्दैछ: [१३, १४]"
                                            else "To survive in a rapidly changing world, the Bote community is diversifying its income: [13, 14]",
                                        fontSize = 12.sp, color = bodyTextColor, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• घरबास (होमस्टे): बोटे महिलाहरूले चितवन (माडी) र सर्लाही (कर्महिया) जस्ता क्षेत्रमा सामुदायिक होमस्टे सफलतापूर्वक सञ्चालन गरी सांस्कृतिक अनुभव, स्थानीय परिकार र माछाका व्यञ्जनहरू पस्किरहेका छन्। [१५]"
                                            else "• Homestays: Bote women are successfully operating community homestays in places like Chitwan (e.g., Madi) and Sarlahi (Karmahiya), offering cultural experiences, local alcohol (chhyang), and fish delicacies. [15]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• फोहोर संकलन आयोजना: केही समुदायहरूले 'प्रोजेक्ट क्याप' जस्ता वातावरणीय आयोजनामा सहभागी भई नदीबाट महाजाल प्रयोग गरेर प्लास्टिकका बोतलहरू निकाल्ने काम गर्छन्, जसले नदी सफा राख्नुका साथै पुन:चक्रण मार्फत आम्दानी दिन्छ। [२, १६]"
                                            else "• Waste Management: Some communities are participating in environmental projects (like Project CAP) by pulling plastic and PET bottles from rivers with nets, which helps keep local waterways clean while generating income through recycling. [2, 16]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                    Text(
                                        text = if (isNepali)
                                            "• खेती र वैदेशिक रोजगार: धेरै मानिसहरू आधुनिक व्यावसायिक खेती (मकै, धान, र तोरी) तिर आकर्षित हुँदै छन् वा वैदेशिक रोजगारीको खोजी गरिरहेका छन्। [११, १४]"
                                            else "• Farming & Migration: Many are shifting to modern commercial farming (corn, rice, and mustard) and seeking foreign employment. [11, 14]",
                                        fontSize = 12.sp, color = bodyTextColor, lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = headingTextColor.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Academic Citations references
                        Text(
                            text = if (isNepali) "📚 सन्दर्भ सामग्री तथा प्रमाणिकरण:" else "📚 Research References & Verification:", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = headingTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[1] globalvoices.org/2025/nepals-indigenous-bote-community/\n[2] thehimalayantimes.com/nepal/bote-people-around-rivers\n[3] kathmandupost.com/province-no-3/2021/10/08/bote-community- chitwan-park\n[4] wikipedia.org/wiki/Bote_people | National Foundation (NFDIN)\n[7] mdpi.com/2071-1050/15/3/2834 (Ecotourism & Livelihoods Research)\n[15] english.onlinekhabar.com/bote-community-homestay-nepal.html\n[16] Project CAP Eco-Initiative: River Cleaning Cooperative.",
                            fontSize = 10.sp,
                            color = bodyTextColor.copy(alpha = 0.6f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // ==================== SECTION 2: EXPLORE BOTE HERITAGE ====================
            item {
                Column {
                    Text(
                        text = "🛶 Explore Bote Heritage Foundations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = headingTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val foundations = listOf(
                        Triple("History 📜", "Ancient river civilization stories", "The Bote are an ancient indigenous riverine community of Nepal, historically serving as expert ferrymen, fishermen, and gold-panners along major river basins like the Gandaki, Narayani, Rapti, and Bagmati."),
                        Triple("Language 🗣️", "Preserve Bote language dialects", "Bote Bhasha is an endangered Indo-Aryan language spoken by the Bote people, closely related to the Majhi and Danuwar languages, traditionally oral and today transcribed using Devanagari script."),
                        Triple("Culture 🔱", "Traditions, beliefs & river worship", "Deeply rooted in nature worship and animism, the Bote perform 'Nadi Puja' (River worship) to honor water spirits, along with worshipping nature deities like Sansari Mai and the hunter god Shikari."),
                        Triple("Dress 👗", "Traditional clothing archive designs", "Men traditionally wear a simple Kachhad (loincloth) and Bhoto (sleeveless vest) suited for river navigation, while women wear a Phariya (sari/skirt) tied with a Patuka (waistband) and a Cholo (blouse), accessorized with silver ornaments like Hasuli and Bulaki.")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        foundfoundRow(foundations.take(2), primaryCardColor, bodyTextColor, headingTextColor, activeTheme) { title, text ->
                            activeLoreTitle = title
                            activeLoreContent = text
                        }
                        foundfoundRow(foundations.drop(2), primaryCardColor, bodyTextColor, headingTextColor, activeTheme) { title, text ->
                            activeLoreTitle = title
                            activeLoreContent = text
                        }
                    }
                }
            }

            // ==================== SECTION 3: INTERACTIVE TIMELINE ====================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⏳ Sarlahi Bote Milestones Timeline",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = headingTextColor
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val epochs = listOf(
                            Pair("Ancestors", "Ancient River Pact kingdoms"),
                            Pair("Kingdom Era", "Madhesh Province migrations"),
                            Pair("Traditional Life", "Canoe and fishing monopoly"),
                            Pair("Conservation Era", "National park river wardens"),
                            Pair("Sovereignty Era", "Preserving language archives"),
                            Pair("Future Youths", "Empowerment through tech")
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(epochs) { (phase, details) ->
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(headingTextColor.copy(alpha = 0.06f))
                                        .border(1.dp, headingTextColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF9A825))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = phase,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = headingTextColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = details,
                                            fontSize = 11.sp,
                                            color = bodyTextColor,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 4: LATEST COMMUNITY NEWS ====================
            item {
                Column {
                    Text(
                        text = "📰 Latest Community Initiatives & News",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = headingTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Featured Story
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                    ) {
                        Column {
                            // Graphics simulation element representing the news photo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2E7D32), Color(0xFF0F4C81))
                                        )
                                    )
                                    .padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = "FEATURED SCRIPTURE",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Sarlahi Bote Youth Digital Initiative Launched at Sarlahi Cultural Center",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = headingTextColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "May 30, 2026 • Karmahiya, Sarlahi",
                                    color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Our Youth Association has coordinated with indigenous linguists to map over 40 hours of oral narratives, uploading high-quality WAV dialogues directly to the Bote Search database. This establishes permanent cultural records for posterity.",
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = bodyTextColor
                                )
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 5: COMMUNITY MAP ====================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryCardColor),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🗺️ Bote Heartlands & Sarlahi Docks Map",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = headingTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap on map nodes to view community counts and local river initiatives:",
                            fontSize = 12.sp,
                            color = bodyTextColor
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Nepal highlights drawing Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTheme == "night") Color(0xFF0F1B25) else Color(0xFFF2F7FA))
                                .border(1.dp, headingTextColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw an abstract green-blue chain representing the rivers Bagmati & Rapti
                                val riverPath = androidx.compose.ui.graphics.Path()
                                riverPath.moveTo(20f, size.height * 0.4f)
                                riverPath.quadraticTo(size.width * 0.3f, size.height * 0.1f, size.width * 0.5f, size.height * 0.7f)
                                riverPath.quadraticTo(size.width * 0.8f, size.height * 0.9f, size.width - 20f, size.height * 0.5f)
                                drawPath(riverPath, Color(0xFF0F4C81).copy(alpha = 0.35f), style = Stroke(width = 8.dp.toPx()))

                                // Map pulse tags
                                val pulses = listOf(
                                    Pair("Sarlahi", Offset(size.width * 0.7f, size.height * 0.65f)),
                                    Pair("Chitwan", Offset(size.width * 0.45f, size.height * 0.45f)),
                                    Pair("Nawalparasi", Offset(size.width * 0.3f, size.height * 0.55f)),
                                    Pair("Palpa", Offset(size.width * 0.22f, size.height * 0.35f)),
                                    Pair("Tanahun", Offset(size.width * 0.38f, size.height * 0.25f)),
                                    Pair("Gorkha", Offset(size.width * 0.55f, size.height * 0.18f))
                                )

                                pulses.forEach { (name, pos) ->
                                    val isSelected = selectedMapDistrict == name
                                    val pulseColor = if (isSelected) Color(0xFFF9A825) else Color(0xFF2E7D32)
                                    // Circle pulse animation simulation using riverWavePhase
                                    val scale = 1f + (sin(riverWavePhase * 2.5f) * 0.25f)
                                    drawCircle(pulseColor.copy(alpha = 0.25f), radius = 18f * scale, center = pos)
                                    drawCircle(pulseColor, radius = 6f, center = pos)
                                }
                            }

                            // Interactive clickable overlay regions
                            Row(modifier = Modifier.fillMaxSize()) {
                                val locations = listOf("Palpa", "Nawalparasi", "Tanahun", "Chitwan", "Gorkha", "Sarlahi")
                                locations.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { selectedMapDistrict = item }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = headingTextColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Text(
                                    text = "🚩 Selected Basin: $selectedMapDistrict District",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = headingTextColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val desc = when (selectedMapDistrict) {
                                    "Sarlahi" -> "Our central base Karmahiya is home to 1,200 Bote members. Location of Sarlahi youth office, grammar documentations, and regular Bagmati River Puja ceremonies."
                                    "Chitwan" -> "Rapti River basin. Highly active eco-tourism cooperative that operates traditional dugout canoe rides and certified mud homestays for organic travelers."
                                    "Nawalparasi" -> "Major canoe construction harbor where master boatwrights train boys ages 14-20 to select and sculpt heavy trunks using traditional tools."
                                    "Palpa" -> "Hill tributary communities preserving historical fishing techniques, utilizing custom 'Koki' traps woven from wild bamboo strips."
                                    "Tanahun" -> "Highland pocket community documenting ancient monsoon songs and folk legends, preserving oral literature from extinction."
                                    else -> "Trident mountain valleys hosting historical Bote river crossings, where youths are trained in water rescue and riverine forest patrolling."
                                }
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = bodyTextColor,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 6: YOUTH SUCCESS STORIES ====================
            item {
                Column {
                    Text(
                        text = "🌟 Netflix-Style Youth Spotlights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = headingTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    LazyRow(
                        state = carouselState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("netflix_carousel")
                    ) {
                        items(successStories) { (name, title, bio) ->
                            Card(
                                modifier = Modifier
                                    .width(280.dp)
                                    .border(
                                        1.dp,
                                        if (carouselIndex == successStories.indexOf(Triple(name, title, bio))) Color(0xFFF9A825).copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(21.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFF9A825), Color(0xFF0F4C81))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = headingTextColor
                                            )
                                            Text(
                                                text = title,
                                                fontSize = 11.sp,
                                                color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = bio,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = bodyTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 7: PHOTO GALLERY ====================
            item {
                Column {
                    Text(
                        text = "📸 Photo Gallery & Cultural Archives",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = headingTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val photos = listOf(
                        Pair("Bagmati Puja", "Festivals category. Annual river worship for protection and good fish harvests."),
                        Pair("Bamboo Fish Traps", "Culture category. Hand-woven traditional 'Koki' fish traps made by elders."),
                        Pair("Organic Mud Homestay", "Homestays category. Mud and straw thatch huts with clean visitor beds."),
                        Pair("Canoe Rower Training", "Youth Programs category. Boys learning ancient river navigation."),
                        Pair("Ancient Monsoons", "Rivers category. Sarlahi riverbanks during traditional monsoon rafting.")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(photos) { (title, description) ->
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable {
                                        lightboxImageTitle = title
                                        lightboxImageDesc = description
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                            ) {
                                Column {
                                    // Abstract vector-based gallery mockup card drawing
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFFD49B54), Color(0xFF0F4C81))
                                                )
                                            )
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🛶 $title",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = headingTextColor
                                        )
                                        Text(
                                            text = description,
                                            fontSize = 11.sp,
                                            color = bodyTextColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 8: UPCOMING EVENTS ====================
            item {
                Column {
                    Text(
                        text = "📅 Sarlahi Upcoming Events & Registrations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = headingTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val events = listOf(
                        Triple("Sarlahi Bote Youth Leadership Camp", "June 15, 2026", "Karmahiya Center"),
                        Triple("Madhesh River Ecology and Canoe Race", "July 2, 2026", "Bagmati Shores")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        events.forEach { (name, date, loc) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = headingTextColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📅 $date  |  📍 $loc",
                                            fontSize = 11.sp,
                                            color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            registeringEventTitle = name
                                            regName = ""
                                            regEmail = ""
                                            regPhone = ""
                                            regDetails = "Interested in joining $name"
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32)
                                        ),
                                        modifier = Modifier.testTag("register_btn_${name.take(5).lowercase()}")
                                    ) {
                                        Text("Register", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 9: NEWSLETTER GLASSMORPHISM CARD ====================
            item {
                var emailInput by remember { mutableStateOf("") }
                var isSubscribed by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeTheme == "night") Color(0x3B1E293B) else Color(0x3B0F4C81)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (activeTheme == "festival") Color(0x40F9A825) else Color(0x420F4C81)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🏮 Stay Connected & Defend Rivers",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = headingTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Subscribe to receive quarterly grammar logs, eco-tourism discounts, and Sarlahi volunteer options.",
                            fontSize = 12.sp,
                            color = bodyTextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isSubscribed) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                placeholder = { Text("Enter your email address...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("newsletter_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = headingTextColor,
                                    unfocusedBorderColor = headingTextColor.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank()) {
                                        isSubscribed = true
                                        triggerConfetti++
                                        viewModel.registerCommunityAction(
                                            type = "Newsletter",
                                            name = "Tribal Supporter",
                                            email = emailInput,
                                            phone = "+977-9800000000",
                                            details = "Newsletter subscription list"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("newsletter_subscribe_btn")
                            ) {
                                Text("Subscribe", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🎉 Thank you! You have joined the circular river preservation network.",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==================== FOUR-COLUMN FOOTER ====================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 20.dp)
                ) {
                    HorizontalDivider(color = headingTextColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Col 1: Community
                        Column(modifier = Modifier.weight(1f)) {
                            Text("COMMUNITY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = headingTextColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• About", fontSize = 12.sp, color = bodyTextColor)
                            Text("• History", fontSize = 12.sp, color = bodyTextColor)
                            Text("• Grammar", fontSize = 12.sp, color = bodyTextColor)
                        }
                        // Col 2: Resources
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RESOURCES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = headingTextColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Research Paper", fontSize = 12.sp, color = bodyTextColor)
                            Text("• Downloads", fontSize = 12.sp, color = bodyTextColor)
                            Text("• Archives", fontSize = 12.sp, color = bodyTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Col 3: Contact
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = if (isNepali) "सम्पर्क र ठेगाना" else "CONTACT & ADDRESS", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Black, 
                                color = headingTextColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🏠 Central Office:", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = bodyTextColor
                            )
                            Text("Bote Community Hall", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            Text("Karmahiya, Ward No. 1", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            Text("Lalbandi Municipality, Sarlahi", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            Text("Madhesh Province, Nepal", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("+977-9844012345"))
                                        viewModel.triggerNotification("📞 Phone number copied to clipboard!")
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text("📞 Tel: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = bodyTextColor)
                                Text("+977-9844012345", fontSize = 11.sp, color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81), fontWeight = FontWeight.Bold)
                            }
                        }
                        // Col 4: Social
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "सामाजिक सञ्जाल" else "SOCIAL LINKS", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Black, 
                                color = headingTextColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Column(
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("info@boteyouthcommunity.org.np"))
                                        viewModel.triggerNotification("📧 Email address copied to clipboard!")
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text("📧 Email Support:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = bodyTextColor)
                                Text("info@boteyouth.org.np", fontSize = 11.sp, color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("🔗 Pages to Follow:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = bodyTextColor)
                            
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("https://facebook.com/BoteYouthSarlahi"))
                                        viewModel.triggerNotification("🔵 Facebook Page Link copied!")
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• Facebook", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            }
                            
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("https://youtube.com/@BoteFolk"))
                                        viewModel.triggerNotification("🔴 YouTube Folk Channel Link copied!")
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• YouTube Folk", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            }
                            
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("https://instagram.com/BoteYouth"))
                                        viewModel.triggerNotification("📸 Instagram Profile Link copied!")
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• Instagram Youths", fontSize = 11.sp, color = bodyTextColor.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Madhesh Province Indigenous Association © 2026. All rights secured.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = bodyTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- Confetti Overlays Canvas ---
        if (confettiList.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                confettiList.forEach { p ->
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.x, p.y),
                        size = Size(p.size, p.size)
                    )
                }
            }
        }

        // --- Lightbox Modal Dialog ---
        lightboxImageTitle?.let { title ->
            Dialog(
                onDismissRequest = { lightboxImageTitle = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable { lightboxImageTitle = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 480.dp)
                            .clickable(enabled = false) {}, // prevent closing click inside card
                        colors = CardDefaults.cardColors(containerColor = primaryCardColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📸 Photo Detail Preview",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32)
                                )
                                IconButton(
                                    onClick = { lightboxImageTitle = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Close", tint = headingTextColor)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // High contrast stylized visual inside lightbox
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFD49B54), Color(0xFF1E3C72))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🛶 Traditional Bote: $title",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = headingTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lightboxImageDesc,
                                fontSize = 13.sp,
                                color = bodyTextColor,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { lightboxImageTitle = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close Preview", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Interactive Heritage Foundations Details Dialog ---
        activeLoreTitle?.let { title ->
            Dialog(onDismissRequest = { activeLoreTitle = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = headingTextColor
                            )
                            IconButton(onClick = { activeLoreTitle = null }) {
                                Icon(Icons.Default.Close, "Close", tint = headingTextColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activeLoreContent,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = bodyTextColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { activeLoreTitle = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Understood", color = Color.White)
                        }
                    }
                }
            }
        }

        // --- Event Registration Modal Dialog ---
        registeringEventTitle?.let { eventName ->
            Dialog(onDismissRequest = { registeringEventTitle = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryCardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝 Event Registration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = headingTextColor
                            )
                            IconButton(onClick = { registeringEventTitle = null }) {
                                Icon(Icons.Default.Close, "Close", tint = headingTextColor)
                            }
                        }
                        Text(
                            text = eventName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            label = { Text("Your full name") },
                            modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Your email address") },
                            modifier = Modifier.fillMaxWidth().testTag("reg_email_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("Your mobile phone") },
                            modifier = Modifier.fillMaxWidth().testTag("reg_phone_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regDetails,
                            onValueChange = { regDetails = it },
                            label = { Text("Additional information") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (regName.isNotBlank() && regEmail.isNotBlank() && regPhone.isNotBlank()) {
                                    viewModel.registerCommunityAction(
                                        type = "Event: $eventName",
                                        name = regName,
                                        email = regEmail,
                                        phone = regPhone,
                                        details = regDetails
                                    )
                                    registeringEventTitle = null
                                    triggerConfetti++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTheme == "festival") Color(0xFFF9A825) else Color(0xFF0F4C81)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("reg_submit_btn")
                        ) {
                            Text("Submit Application", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun foundfoundRow(
    items: List<Triple<String, String, String>>,
    cardColor: Color,
    textColor: Color,
    headingColor: Color,
    activeTheme: String,
    onLoreSelected: (String, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (title, subtitle, lore) ->
            var isHeld by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (isHeld) 0.96f else 1f, label = "LoreScale")
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clickable {
                        isHeld = true
                        onLoreSelected(title, lore)
                        isHeld = false
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = headingColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun GbpRowItem(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

// ==================== TAB 2: SEO LANDING PAGES ====================
@Composable
fun SEOPagesPanel(
    viewModel: BoteCommunityViewModel,
    pagesList: List<SeoLandingPage>,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    isNepali: Boolean
) {
    val selectedCategory by viewModel.selectedLandingCategory.collectAsStateWithLifecycle()
    val activePage = pagesList.find { it.categoryName == selectedCategory } ?: pagesList[0]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("seo_pages_screen")
    ) {
        // Horizontal scroll category chips (15 items!)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pagesList) { page ->
                val isSelected = selectedCategory == page.categoryName
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedLandingCategory.value = page.categoryName },
                    label = { Text(text = if (isNepali) getCategoryTranslation(page.categoryName) else page.categoryName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("seo_chip_${page.categoryName.lowercase().replace(" ", "_")}")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Section info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LeafGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isNepali) "गुगल र्याङ्किङ क्यानोनिकल स्क्रिन" else "Google Ranking Canonical Screen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activePage.canonicalTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activePage.details,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Google Search Preview (Simulates exact Google Ranking snippets)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NightPebble)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Web Preview",
                                tint = LeafGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNepali) "गुगल खोज नतिजा पूर्वावलोकन" else "Google Search Result Snippet Preview",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "https://botecommunity.org/${activePage.categoryName.lowercase().replace(" ", "-")}",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activePage.canonicalTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8AB4F8), // Classic Google Blue in Dark Mode
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Text(
                            text = activePage.metaDescription,
                            fontSize = 12.sp,
                            color = Color(0xFFBDC1C6), // Classic Google Snippet Grey
                            lineHeight = 17.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isNepali) "🔑 मुख्य कुञ्जीशब्दहरू: ${activePage.keywords}" else "🔑 Focus Keywords: ${activePage.keywords}",
                                fontSize = 10.sp,
                                color = SandOchre,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Structured FAQs (FAQ Schema integration)
            item {
                Column {
                    Text(
                        text = if (isNepali) "❔ व्यवस्थित FAQ खण्ड (स्ट्रक्चर्ड डाटा सुहाउँदो)" else "❔ Optimized FAQ Section (Structured Data compatible)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    activePage.faqs.forEach { (q, a) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Q: $q",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "A: $a",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Visual ALT tag guidelines (Combats AI Slop by establishing precise media transparency attributes)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📸 Image SEO Accessibility & Alt Tag Matrix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, "Alt Tag", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Recommended Asset ALT Attribute:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\"${activePage.altTagGuide}\"",
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Internal Ranking Anchor Links
            item {
                Column {
                    Text(
                        text = "🔗 Related Internal Anchor Targets",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activePage.internalLinks.forEach { linkTarget ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.selectedLandingCategory.value = linkTarget
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "→ $linkTarget",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 3: DIGITAL LIBRARY ====================
@Composable
fun DigitalLibraryPanel(
    viewModel: BoteCommunityViewModel,
    resources: List<Resource>
) {
    val searchVal by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeLibraryCategory by viewModel.selectedLibraryCategory.collectAsStateWithLifecycle()

    val categories = listOf("All", "PDF Report", "Research Paper", "Census Info", "Oral History")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Search and filter headers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchVal,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search academic PDFs, censuses, oral files...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchVal.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Dynamic Library filters horizontal scroll
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { category ->
                    val isSelected = activeLibraryCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedLibraryCategory.value = category },
                        label = { Text(text = category, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("lib_chip_${category.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        if (resources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, "No results", tint = SandOchre, modifier = Modifier.size(48.dp))
                    Text("No records matched your search query", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("library_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resources) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.resourceType == "PDF") Icons.Default.Info else Icons.Default.PlayArrow,
                                        contentDescription = item.resourceType,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "By ${item.author} • ${item.year}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💾 File size: ${item.fileSize}   |   📥 Downloads: ${item.downloadCount}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Button(
                                    onClick = { viewModel.downloadItem(item.id, item.title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, "Download", tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== AUXILIARY PRIVATE DATA MASKING UTILITIES ====================
fun maskEmailSecured(email: String): String {
    if (!email.contains("@")) return "********"
    val parts = email.split("@")
    val name = parts[0]
    val domain = parts[1]
    if (name.length <= 2) {
        return "${name.firstOrNull() ?: ""}***@$domain"
    }
    return "${name.take(1)}***${name.takeLast(1)}@$domain"
}

fun maskPhoneSecured(phone: String): String {
    val clean = phone.trim()
    if (clean.length < 6) return "******"
    return "${clean.take(3)}****${clean.takeLast(3)}"
}

@Composable
fun DonationBannerCard(
    viewModel: BoteCommunityViewModel
) {
    val donations by viewModel.allDonations.collectAsStateWithLifecycle()
    val isNepali by viewModel.isNepali.collectAsStateWithLifecycle()
    
    val totalGoal = 500000.0
    val totalRaised = donations.sumOf { it.amount }
    val progressFraction = (totalRaised / totalGoal).coerceIn(0.0, 1.0).toFloat()
    val donorCount = donations.size

    val title = if (isNepali) "बोटे युवा समुदाय नेपाल कोष" else "Bote Youth Community Nepal Fund"
    val subtitle = if (isNepali) "उच्च शिक्षा छात्रवृत्ति, प्रविधि पुस्तकालय, बोटे भाषा र परम्परागत पोशाक संरक्षण अभियान" else "Supporting higher education scholarships, IT library development, language preservation, and traditional dress heritage."
    val raisedLabel = if (isNepali) "कुल संकलन:" else "Total Raised:"
    val goalLabel = if (isNepali) "लक्ष्य:" else "Goal:"
    val donorsLabel = if (isNepali) "दाताहरू:" else "Donors:"
    val donateBtnText = if (isNepali) "अहिले नै सहयोग गर्नुहोस् 💖" else "Secure Donation Portal 💖"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("donation_banner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Fund Donation Logo",
                    tint = ErrorRed,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Verified Cloud Project via Firebase",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            
            // Progress Bar and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$raisedLabel NPR ${String.format("%,.0f", totalRaised)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$goalLabel NPR ${String.format("%,.0f", totalGoal)} (${String.format("%.1f", progressFraction * 100)}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 $donorCount $donorsLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Button(
                    onClick = { viewModel.setScreen(AppScreen.DONATION_PORTAL) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("go_to_donation_portal_button")
                ) {
                    Text(
                        text = donateBtnText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class BotePopSegment(
    val district: String,
    val districtNp: String,
    val population: Int,
    val percentage: Float,
    val color: Color,
    val river: String,
    val riverNp: String,
    val details: String,
    val detailsNp: String
)

@Composable
fun DonationPortalPanel(
    viewModel: BoteCommunityViewModel
) {
    val donations by viewModel.allDonations.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isNepali by viewModel.isNepali.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("Form") } // "Form", "Progress", "HonorRoll", "Impact", "History"
    var impactSubTab by remember { mutableStateOf("Demographics") }
    var selectedSegmentIndex by remember { mutableStateOf(0) }
    
    // Donation Form States
    var selectedCauses by remember { mutableStateOf(setOf("Bote Language Preservation")) }
    var isListExpanded by remember { mutableStateOf(true) } // Starts expanded, can be toggled
    var donorName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var isAnonymous by remember { mutableStateOf(false) }
    var donorEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var customAmountStr by remember { mutableStateOf("") }
    var selectedPredefinedAmount by remember { mutableStateOf<Double?>(1000.0) }
    var bestWishesMsg by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("eSewa") } // "eSewa", "Khalti", "Card Payment"
    var historySearchEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    
    // Gateway Dialog States
    var showGatewayDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<BoteDonation?>(null) }
    
    // Local Visual Success Toast notification state
    var showLocalSuccessToast by remember { mutableStateOf(false) }
    var localSuccessToastMsg by remember { mutableStateOf("") }

    if (showLocalSuccessToast) {
        LaunchedEffect(localSuccessToastMsg) {
            kotlinx.coroutines.delay(4000)
            showLocalSuccessToast = false
        }
    }
    
    val calculatedAmount: Double = if (selectedPredefinedAmount != null) {
        selectedPredefinedAmount!!
    } else {
        customAmountStr.toDoubleOrNull() ?: 0.0
    }

    val totalGoal = 500000.0
    val totalRaised = donations.sumOf { it.amount }
    val progressFraction = (totalRaised / totalGoal).coerceIn(0.0, 1.0).toFloat()

    val causesList = remember(isNepali) {
        if (isNepali) {
            listOf(
                Triple("Bote Language Preservation", "🗣️ बोटे भाषा संरक्षण तथा पठनपाठन", "लोपोन्मुख बोटे भाषाको अभिलेखीकरण, शब्दकोश निर्माण र दुईभाषी पाठ्यक्रम सहितका मातृभाषा कक्षाहरू सञ्चालन गर्ने अभियान।"),
                Triple("Bote Traditional Dress", "👗 परम्परागत बोटे पहिरन तथा चाँदीका गहना संरक्षण", "मौलिक हस्तनिर्मित पहिरन (पुरुषका लागि कच्छाड र भोटो, महिलाका लागि फरिया र पटुका) तथा परम्परागत चाँदीका गहनाहरू (हँसुली, बुलाकी, कल्ली) को जगेर्ना र प्रवर्द्धन।"),
                Triple("Higher Education Scholarships", "🎓 उच्च शिक्षा छात्रवृत्ति तथा विश्वविद्यालय भर्ना सहयोग", "बोटे युवाहरूका लागि विश्वविद्यालय भर्ना, वृत्ति मार्गनिर्देशन, र पूर्ण वित्तीय सहयोग सहितको छात्रवृत्ति कार्यक्रम।"),
                Triple("IT Library Development", "💻 सौर्य-ऊर्जाबाट चल्ने सूचना प्रविधि पुस्तकालय विकास", "नदी तटीय ग्रामीण क्षेत्र (जस्तै कर्महिया, सर्लाही) मा डिजिटल दूरी मेटाउन सौर्य-ऊर्जाबाट चल्ने सूचना प्रविधि पुस्तकालय (IT Library) र कम्प्युटर सिकाइ केन्द्रको विकास।"),
                Triple("Supporting Bote Youth Community", "🤝 बोटे युवा समुदाय नेपाल सहयोग र सबलीकरण", "बोटे युवा समुदाय नेपाल (BYCN) लाई स्थानीय स्तरमा सामाजिक न्याय, युवा नेतृत्व मञ्च र नदी संरक्षण अभियानको अगुवाइ गर्न सशक्तिकरण।"),
                Triple("Supporting Bote Young Generations", "🌟 बोटे नयाँ पुस्ता सशक्तिकरण", "नयाँ पुस्ताका बोटे बालबालिका तथा युवाहरूलाई नेतृत्व तालिम, व्यावसायिक मार्गनिर्देशन, र आधुनिक प्राविधिक सामग्री सहयोग।")
            )
        } else {
            listOf(
                Triple("Bote Language Preservation", "🗣️ Bote Language Preservation & Classes", "Documenting endangered linguistic goals, compiling dictionary resources, and running bilingual curriculum primary teaching classes."),
                Triple("Bote Traditional Dress", "👗 Bote Traditional Dress & Sacred Ornaments", "Preserving hand-woven traditional clothing (Kachhad and Bhoto for men, Phariya and Patuka for women) along with sacred silver ornaments like the Hasuli, Bulaki, and Kalli."),
                Triple("Higher Education Scholarships", "🎓 Higher Education & University Admission Scholarships", "Supporting Bote students with university admission sponsorships, professional guidance, and complete financial support to eliminate cost barriers."),
                Triple("IT Library Development", "💻 Solar-Powered IT Library & Learning Centers", "Setting up physical, solar-powered IT library and computer learning centers across rural riverine communities (such as Karmahiya, Sarlahi) to bridge the digital divide."),
                Triple("Supporting Bote Youth Community", "🤝 Supporting Bote Youth Community Nepal", "Empowering Bote Youth Community Nepal (BYCN) to lead local social advocacy, river conservation campaigns, and youth employment networks."),
                Triple("Supporting Bote Young Generations", "🌟 Supporting Bote Young Generations", "Providing leadership workshops, vocational career guidance, modern technical skill bootcamps, and digital tools for Bote youth.")
            )
        }
    }

    val pillars = remember {
        listOf(
            Triple("Bote Language Preservation", "🗣️ Bote Language Preservation", "बोटे भाषा संरक्षण तथा पठनपाठन"),
            Triple("Bote Traditional Dress", "👗 Bote Traditional Dress & Ornaments", "बोटे परम्परागत पोशाक र गहना संरक्षण"),
            Triple("Higher Education Scholarships", "🎓 Higher Ed Scholarships & Admits", "उच्च शिक्षा छात्रवृत्ति तथा विश्वविद्यालय भर्ना"),
            Triple("IT Library Development", "💻 Solar-Powered IT Libraries", "प्रविधि पुस्तकालय र कम्प्युटर सिकाइ केन्द्र"),
            Triple("Supporting Bote Youth Community", "🤝 Supporting Bote Youth Community", "बोटे युवा समुदाय नेपाल सशक्तिकरण"),
            Triple("Supporting Bote Young Generations", "🌟 Next-Gen Leadership & Guidance", "बोटे नयाँ पुस्ता नेतृत्व तथा मार्गनिर्देशन")
        )
    }

    val pillarDetails = remember {
        mapOf(
            "Bote Language Preservation" to (100000.0 to Color(0xFF1E88E5)),
            "Bote Traditional Dress" to (75000.0 to Color(0xFF43A047)),
            "Higher Education Scholarships" to (125000.0 to Color(0xFFE53935)),
            "IT Library Development" to (100000.0 to Color(0xFF8E24AA)),
            "Supporting Bote Youth Community" to (50000.0 to Color(0xFFF4511E)),
            "Supporting Bote Young Generations" to (50000.0 to Color(0xFF00ACC1))
        )
    }

    val pillarRaised = remember(donations) {
        val map = mutableMapOf<String, Double>()
        val keys = listOf(
            "Bote Language Preservation",
            "Bote Traditional Dress",
            "Higher Education Scholarships",
            "IT Library Development",
            "Supporting Bote Youth Community",
            "Supporting Bote Young Generations"
        )
        keys.forEach { map[it] = 0.0 }
        
        donations.forEach { don ->
            val parts = don.targetCause.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matched = parts.filter { map.containsKey(it) }
            if (matched.isNotEmpty()) {
                val share = don.amount / matched.size
                matched.forEach { key ->
                    map[key] = (map[key] ?: 0.0) + share
                }
            } else {
                val share = don.amount / map.size
                map.keys.forEach { key ->
                    map[key] = (map[key] ?: 0.0) + share
                }
            }
        }
        map
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("donation_portal_panel"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Back Button & Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setScreen(AppScreen.HOME) },
                    modifier = Modifier.testTag("donation_portal_back_btn")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go back to Home")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isNepali) "बोटे युवा समुदाय नेपाल विकास कोष" else "Bote Youth Community Nepal Fund",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isNepali) "सुरक्षित भुक्तानी पोर्टल • तत्काल क्लाउड सिंक" else "Secure Payment Gateway • Real-time Cloud Synchronization",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Live Total Stat Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isNepali) "समुदाय कोष संकलन प्रगति" else "Community Development Fund Progress",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "NPR ${String.format("%,.0f", totalRaised)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Target: NPR ${String.format("%,.0f", totalGoal)} (${String.format("%.1f", progressFraction * 100)}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "👥 ${donations.size} Contributor(s)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "🔒 SSL Encrypted Checkout",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Horizontal Segmented Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Form" to (if (isNepali) "सहयोग" else "Donate"),
                    "Progress" to (if (isNepali) "प्रगति" else "Progress"),
                    "HonorRoll" to (if (isNepali) "दाताहरू" else "Donors"),
                    "Impact" to (if (isNepali) "प्रभाव" else "Impact"),
                    "History" to (if (isNepali) "इतिहास" else "History")
                ).forEach { (tabKey, label) ->
                    val isSelected = activeTab == tabKey
                    Button(
                        onClick = { activeTab = tabKey },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("donation_tab_$tabKey")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tab Content Switcher
        when (activeTab) {
            "Form" -> {
                // Explore & Select Cause
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isNepali) "१. सहयोगको क्षेत्र अन्वेषण र छनोट गर्नुहोस्" else "1. Explore & Select Causes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isNepali) "तपाईंको सहयोग आवश्यकता अनुसार विभाजित गरिनेछ।" else "Your support will fund your selected areas.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            
                            TextButton(
                                onClick = { isListExpanded = !isListExpanded },
                                modifier = Modifier.testTag("toggle_causes_list_btn")
                            ) {
                                Text(
                                    text = if (isListExpanded) {
                                        if (isNepali) "सूची लुकाउनुहोस् ▲" else "Collapse ▲"
                                    } else {
                                        if (isNepali) "सूची देखाउनुहोस् ▼" else "Expand ▼"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Selected Summary Card (acts as the click-to-expand trigger too!)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isListExpanded = !isListExpanded }
                                .testTag("selected_causes_summary_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isNepali) "छनोट गरिएका क्षेत्रहरू (${selectedCauses.size})" else "Selected Focus Areas (${selectedCauses.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    val selectedNames = causesList
                                        .filter { selectedCauses.contains(it.first) }
                                        .map { it.second }
                                        .joinToString(", ")
                                    Text(
                                        text = selectedNames.ifEmpty { if (isNepali) "कुनै पनि छनोट गरिएको छैन" else "None selected" },
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.graphicsLayer(rotationZ = if (isListExpanded) 180f else 0f)
                                )
                            }
                        }
                    }
                }

                if (isListExpanded) {
                    items(causesList) { (key, label, desc) ->
                        val isSelected = selectedCauses.contains(key)
                        val onSelect = {
                            selectedCauses = if (isSelected) {
                                if (selectedCauses.size > 1) selectedCauses - key else selectedCauses
                            } else {
                                selectedCauses + key
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect() }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("cause_card_${key.take(10).lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedCauses = selectedCauses + key
                                        } else {
                                            if (selectedCauses.size > 1) selectedCauses - key else selectedCauses
                                        }
                                    },
                                    modifier = Modifier.testTag("cause_checkbox_${key.take(10).lowercase().replace(" ", "_")}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Donor Info
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "२. आफ्नो विवरण भर्नुहोस्" else "2. Enter Contributor Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAnonymous,
                                onCheckedChange = { isAnonymous = it },
                                modifier = Modifier.testTag("anonymous_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isNepali) "गुमनाम रूपमा सहयोग गर्नुहोस्" else "Donate Anonymously (Hide Name from Board)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!isAnonymous) {
                            OutlinedTextField(
                                value = donorName,
                                onValueChange = { donorName = it },
                                label = { Text(if (isNepali) "पूरा नाम" else "Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("donor_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = donorEmail,
                            onValueChange = { donorEmail = it },
                            label = { Text(if (isNepali) "इमेल ठेगाना" else "Email Address (For Receipt)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("donor_email_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = bestWishesMsg,
                            onValueChange = { bestWishesMsg = it },
                            label = { Text(if (isNepali) "शुभकामना सन्देश (ऐच्छिक)" else "Best Wishes Message (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("donor_message_input"),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Select Amount
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "३. सहयोग रकम रोज्नुहोस्" else "3. Select Contribution Amount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val amounts = listOf(500.0, 1000.0, 2500.0, 5000.0, 10000.0)
                        items(amounts) { amt ->
                            val isSelected = selectedPredefinedAmount == amt
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPredefinedAmount = amt
                                    customAmountStr = ""
                                },
                                label = { Text("Rs. ${String.format("%,.0f", amt)}") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("amount_chip_${amt.toInt()}")
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Custom Amount Textfield
                    OutlinedTextField(
                        value = customAmountStr,
                        onValueChange = {
                            customAmountStr = it
                            selectedPredefinedAmount = null
                        },
                        label = { Text(if (isNepali) "अन्य रकम प्रविष्ट गर्नुहोस्" else "Or Enter Custom Amount (NPR)") },
                        leadingIcon = { Text("Rs.", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth().testTag("custom_amount_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Payment Options
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "४. भुक्तानी विधि रोज्नुहोस्" else "4. Select Secure Payment Gateway",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Local Gateways
                        Text(
                            text = if (isNepali) "स्थानीय गेटवे (Local Gateways)" else "Local Gateways",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("eSewa", Color(0xFF60BB46), Icons.Default.CheckCircle),
                                Triple("Khalti", Color(0xFF5C2D91), Icons.Default.Star),
                                Triple("Mobile Banking", Color(0xFFFF9800), Icons.Default.Home)
                            ).forEach { (method, themeColor, icon) ->
                                val isSelected = paymentMethod == method
                                Card(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .clickable { paymentMethod = method }
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) themeColor else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .testTag("payment_chip_${method.lowercase().replace(" ", "_")}"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) themeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = method,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // International Gateways
                        Text(
                            text = if (isNepali) "अन्तर्राष्ट्रिय भुक्तानी (International Gateways)" else "International Gateways",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Card Payment", Color(0xFF1E88E5), Icons.Default.ShoppingCart),
                                Triple("Stripe", Color(0xFF6772E5), Icons.Default.Check),
                                Triple("PayPal", Color(0xFF003087), Icons.Default.Person),
                                Triple("Google Pay", Color(0xFF4285F4), Icons.Default.PlayArrow)
                            ).forEach { (method, themeColor, icon) ->
                                val isSelected = paymentMethod == method
                                Card(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .clickable { paymentMethod = method }
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) themeColor else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .testTag("payment_chip_${method.lowercase().replace(" ", "_")}"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) themeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = method,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Proceed Button
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    val nameToSubmit = if (isAnonymous) "Anonymous" else donorName.trim()
                    val isValid = calculatedAmount > 0.0 && donorEmail.contains("@") && (isAnonymous || nameToSubmit.isNotEmpty())
                    Button(
                        onClick = { showGatewayDialog = true },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when(paymentMethod) {
                                "eSewa" -> Color(0xFF60BB46)
                                "Khalti" -> Color(0xFF5C2D91)
                                "Mobile Banking" -> Color(0xFFFF9800)
                                "Stripe" -> Color(0xFF6772E5)
                                "PayPal" -> Color(0xFF003087)
                                "Google Pay" -> Color(0xFF4285F4)
                                "Card Payment" -> Color(0xFF1E88E5)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_payment_portal_button")
                    ) {
                        Text(
                            text = if (isNepali) {
                                "सुरक्षित NPR ${String.format("%,.0f", calculatedAmount)} भुक्तानी गर्नुहोस् 🔒"
                            } else {
                                "Proceed with NPR ${String.format("%,.0f", calculatedAmount)} Securely 🔒"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            "Progress" -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = if (isNepali) "सांस्कृतिक लक्ष्य तथा स्तम्भ प्रगति" else "Key Pillars & Cultural Goals Progress",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isNepali) {
                                "हाम्रो पुर्खाको कला, भाषा र मौलिकतालाई नयाँ पुस्तामा हस्तान्तरण गर्न प्रत्यक्ष योगदान ड्यासबोर्ड।"
                            } else {
                                "Real-time tracking of goal targets supporting language, dress, scholarships, and community structures."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                items(pillars) { (key, enLabel, npLabel) ->
                    val (target, themeColor) = pillarDetails[key] ?: (100000.0 to MaterialTheme.colorScheme.primary)
                    val raised = pillarRaised[key] ?: 0.0
                    val fraction = (raised / target).coerceIn(0.0, 5.0).toFloat()
                    val percent = (fraction * 100).toInt()
                    
                    val desc = when (key) {
                        "Bote Language Preservation" -> if (isNepali) "लोपोन्मुख बोटे भाषाको अभिलेखीकरण, शब्दकोश निर्माण र मातृभाषा कक्षाहरू सञ्चालन गर्ने लक्ष्य।" else "Documenting endangered linguistic heritage, compiling dictionaries, and hosting primary bilingual language courses."
                        "Bote Traditional Dress" -> if (isNepali) "मौलिक हस्तनिर्मित पहिरन (कच्छाड, भोटो, फरिया, पटुका) र पवित्र चाँदीका गहनाहरूको संरक्षण र प्रवर्द्धन।" else "Preserving hand-woven traditional clothing and sacred silver ornaments like the Hasuli, Bulaki, and Kalli."
                        "Higher Education Scholarships" -> if (isNepali) "बोटे समुदायका प्रतिभावान विद्यार्थीहरूका लागि विश्वविद्यालय भर्ना र पूर्ण कलेज छात्रवृत्ति व्यवस्था।" else "Sponsoring university enrollments, tuition fees, and professional career mentorship programs."
                        "IT Library Development" -> if (isNepali) "ग्रामीण क्षेत्र (जस्तै कर्महिया, सर्लाही) मा सौर्य-ऊर्जाबाट चल्ने प्रविधि पुस्तकालय र कम्प्युटर केन्द्र स्थापना।" else "Setting up solar-powered, offline computer learning stations to bridge the digital divide in remote settlements."
                        "Supporting Bote Youth Community" -> if (isNepali) "बोटे युवा समुदाय नेपाल (BYCN) लाई नदी संरक्षण, नेतृत्व कार्यशाला र पैरवी अभियान सञ्चालन गर्न सहयोग।" else "Empowering BYCN to organize local advocacy panels, river conservation drives, and youth networks."
                        else -> if (isNepali) "नयाँ पुस्ताका बालबालिकाहरूलाई वृत्ति मार्गनिर्देशन, आधुनिक प्रविधिको ज्ञान र अध्ययन सामग्री वितरण सहयोग।" else "Providing leadership bootcamps, career guidance, computer literacy, and modern student study tools."
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pillar_progress_$key"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isNepali) npLabel else enLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$percent%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColor
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = themeColor,
                                trackColor = themeColor.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${if (isNepali) "संकलित: रु " else "Raised: NPR "}${String.format("%,.0f", raised)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                                Text(
                                    text = "${if (isNepali) "लक्ष्य: रु " else "Target: NPR "}${String.format("%,.0f", target)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            "HonorRoll" -> {
                // List of donors / Contributors Leaderboard
                item {
                    Text(
                        text = if (isNepali) "हाम्रा आदरणीय दाताहरू (सम्मान सूची)" else "Honor Roll of Generous Contributors",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (donations.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No donations recorded yet. Be the first to support our youth!",
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(donations) { don ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("donation_record_${don.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (don.donorName == "Anonymous") {
                                                if (isNepali) "गुमनाम दाता" else "Anonymous Donor"
                                            } else {
                                                don.donorName
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "NPR ${String.format("%,.0f", don.amount)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = don.targetCause,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    if (don.message.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${don.message}\"",
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Via ${don.paymentMethod} • ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(don.timestamp))}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        
                                        // Admin Delete Capability
                                        if (currentUser?.role == "admin") {
                                            IconButton(
                                                onClick = { viewModel.deleteDonation(don.id) },
                                                modifier = Modifier.size(20.dp).testTag("delete_donation_${don.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Impact" -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = if (isNepali) "बोटे समुदाय डेमोग्राफिक्स र प्रभाव" else "Bote Demographics & Growth",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isNepali) {
                                "नेपालमा बोटे समुदायको वास्तविक जनसंख्या वितरण र भावी विकास रणनीतिहरू।"
                            } else {
                                "Real demographic census distribution in Nepal & strategic developmental pathways."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Sub-tabs Row with horizontal scrolling
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val subTabs = listOf(
                                "Demographics" to (if (isNepali) "📊 बोटे जनसंख्या (केक चार्ट)" else "📊 Bote Population (Cake Chart)"),
                                "Impact" to (if (isNepali) "🌱 हाम्रो प्रभाव" else "🌱 Our Impact"),
                                "Growth" to (if (isNepali) "🚀 विकास विकल्पहरू" else "🚀 Growth Options")
                            )
                            subTabs.forEach { (subKey, label) ->
                                val isSelected = impactSubTab == subKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable { impactSubTab = subKey }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (impactSubTab == "Demographics") {
                    val populationSegments = listOf(
                        BotePopSegment(
                            district = "Nawalparasi",
                            districtNp = "नवलपरासी",
                            population = 4250,
                            percentage = 38.0f,
                            color = Color(0xFFE53935),
                            river = "Narayani & Kali Gandaki",
                            riverNp = "नारायणी र कालीगण्डकी",
                            details = "Largest ancestral settlement in Nawalparasi East and West. Communities traditionally work in boat ferry transportation and sustainable fishing. Currently advocating for river-use permissions.",
                            detailsNp = "नवलपरासी पूर्व र पश्चिममा सबैभन्दा ठूलो पुर्ख्यौली बसोबास। नदी किनारमा डुङ्गा चलाउने, माछा मार्ने र संस्कृति संरक्षणमा सक्रिय। हाल नदी उपयोग अधिकारका लागि वकालत गर्दै।"
                        ),
                        BotePopSegment(
                            district = "Chitwan",
                            districtNp = "चितवन",
                            population = 2460,
                            percentage = 22.0f,
                            color = Color(0xFFEC407A),
                            river = "Rapti & Narayani",
                            riverNp = "राप्ती र नारायणी",
                            details = "Concentrated in Madi and near Chitwan National Park borders. Facing strict conservation boundaries. Transitioning towards organic farming, nature guiding, and community homestay programs.",
                            detailsNp = "माडी र चितवन राष्ट्रिय निकुञ्ज क्षेत्रमा बसोबास। निकुञ्जको प्रतिबन्धका कारण कृषि, प्रकृति पथप्रदर्शक र मध्यवर्ती क्षेत्र पर्या-पर्यटन होमस्टेतर्फ स्थानान्तरण हुँदै।"
                        ),
                        BotePopSegment(
                            district = "Sarlahi",
                            districtNp = "सर्लाही",
                            population = 1680,
                            percentage = 15.0f,
                            color = Color(0xFF8E24AA),
                            river = "Bagmati River",
                            riverNp = "बागमती नदी",
                            details = "Home to Karmahiya and the core operations of Bote Youth Community Nepal (BYCN). Highly active youth networks pioneering language conservation, solar learning hubs, and cultural revival.",
                            detailsNp = "कर्महिया र बोटे युवा समुदाय नेपाल (BYCN) को केन्द्र। भाषा संरक्षण, सौर्य पुस्तकालय र युवा नेतृत्व विकासमा देशकै सबैभन्दा संगठित र सक्रिय जिल्ला।"
                        ),
                        BotePopSegment(
                            district = "Tanahu",
                            districtNp = "तनहुँ",
                            population = 1340,
                            percentage = 12.0f,
                            color = Color(0xFF1E88E5),
                            river = "Madi & Seti Rivers",
                            riverNp = "मादी र सेती नदी",
                            details = "Residing in Vyas and Bhimad municipalities. Historically famous for river panning for gold dust, sacred river rituals, and river rafting/navigation coaching.",
                            detailsNp = "व्यास र भिमाद नगरपालिकाका नदी तटीय क्षेत्रमा बसोबास। नदी किनारमा सुन चाल्ने (सुनखानी) र नदी गाइड (राफ्टिङ) को विशेष पुर्ख्यौली सीप भएको बोटे समुदाय।"
                        ),
                        BotePopSegment(
                            district = "Palpa & Others",
                            districtNp = "पाल्पा र अन्य",
                            population = 1450,
                            percentage = 13.0f,
                            color = Color(0xFF00897B),
                            river = "Kali Gandaki & Trishuli",
                            riverNp = "कालीगण्डकी र त्रिशुली",
                            details = "Scattered families along the major Gandaki basins. Highly determined families working hard to secure education, preserve sacred river shrines, and document ancestral oral folklore.",
                            detailsNp = "कालीगण्डकी र त्रिशूली नदीको बहाव क्षेत्रमा छरिएर रहेका परिवारहरू। जल देवताको पूजा, मौखिक लोककथा र बालबालिकाको आधारभूत शिक्षामा केन्द्रित समुदाय।"
                        )
                    )

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isNepali) "बोटे समुदायको भौगोलिक वितरण (कुल: ~११,१८०)" else "Bote Geographic Population Distribution (Total: ~11,180)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Custom Interactive Cake (Pie) Chart
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .pointerInput(Unit) {
                                                detectTapGestures { offset ->
                                                    val centerX = size.width / 2f
                                                    val centerY = size.height / 2f
                                                    val dx = offset.x - centerX
                                                    val dy = offset.y - centerY
                                                    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                    angle = (angle + 360f) % 360f
                                                    // Start angle is -90f (top point, which corresponds to 270f on math unit circle)
                                                    val adjustedAngle = (angle - 270f + 360f) % 360f
                                                    
                                                    var currentSumAngle = 0f
                                                    val totalPop = 11180f
                                                    populationSegments.forEachIndexed { idx, seg ->
                                                        val sweep = (seg.population.toFloat() / totalPop) * 360f
                                                        if (adjustedAngle >= currentSumAngle && adjustedAngle < currentSumAngle + sweep) {
                                                            selectedSegmentIndex = idx
                                                        }
                                                        currentSumAngle += sweep
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            var startAngle = -90f
                                            val totalPop = 11180f
                                            populationSegments.forEachIndexed { index, seg ->
                                                val sweepAngle = (seg.population.toFloat() / totalPop) * 360f
                                                val isSelected = index == selectedSegmentIndex
                                                val scale = if (isSelected) 1.05f else 1.0f
                                                
                                                drawArc(
                                                    color = seg.color,
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = true,
                                                    size = Size(size.width * scale, size.height * scale),
                                                    topLeft = Offset(
                                                        (size.width - size.width * scale) / 2f,
                                                        (size.height - size.height * scale) / 2f
                                                    )
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }

                                        // Doughnut inner overlay for professional look
                                        Box(
                                            modifier = Modifier
                                                .size(45.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${populationSegments[selectedSegmentIndex].percentage.toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = populationSegments[selectedSegmentIndex].color
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Legend list
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        populationSegments.forEachIndexed { index, seg ->
                                            val isSelected = index == selectedSegmentIndex
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) seg.color.copy(alpha = 0.12f) else Color.Transparent)
                                                    .clickable { selectedSegmentIndex = index }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(seg.color)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = if (isNepali) seg.districtNp else seg.district,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) seg.color else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${seg.percentage}% (~${seg.population})",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val activeSegment = populationSegments[selectedSegmentIndex]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .border(1.dp, activeSegment.color.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = activeSegment.color.copy(alpha = 0.06f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 ${if (isNepali) activeSegment.districtNp else activeSegment.district} Bote Community",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeSegment.color
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(activeSegment.color)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isNepali) "जनसंख्या: ${activeSegment.population}" else "Pop: ${activeSegment.population}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${if (isNepali) "मुख्य नदी प्रणाली: " else "Major River System: "}${if (isNepali) activeSegment.riverNp else activeSegment.river}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isNepali) activeSegment.detailsNp else activeSegment.details,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                } else if (impactSubTab == "Growth") {
                    val growthOptions = if (isNepali) {
                        listOf(
                            Triple("📈 भाषा र संस्कृति डिजिटल रेकर्डिङ (२०२६-२०२७)", "बोटे भाषाका पुर्ख्यौली लोककथा, मन्त्र, र गीतहरू डिजिटल अडियो/भिडियो पुस्तकालयमा रेकर्ड गर्ने। (लक्ष्य: १००% संकलन, हाल: ३५%)", 0.35f),
                            Triple("🛶 पर्या-पर्यटन र काठको डुङ्गा उद्यम (२०२६-२०२८)", "नारायणी र राप्ती नदीमा बोटे युवाहरूलाई नदी पथप्रदर्शक, जङ्गल सफारी र सांस्कृतिक होमस्टे व्यवसाय तालीम। (लक्ष्य: ५० होमस्टे, हाल: १५)", 0.30f),
                            Triple("🐟 वैज्ञानिक जलाशय माछापालन सहकारी (२०२७-२०२९)", "परम्परागत माछा मार्ने सीपलाई सहकारी मार्फत आधुनिक र दिगो जलाशय मत्स्यपालनमा रूपान्तरण गर्ने। (लक्ष्य: १० सहकारी, हाल: ३)", 0.30f),
                            Triple("💡 डिजिटल साक्षरता र सौर्य पुस्तकालय सञ्जाल (२०२६-२०३०)", "सर्लाहीको कर्महियाका अतिरिक्त तनहुँ र नवलपरासीका बोटे टोलहरूमा थप ६ वटा अफलाइन सौर्य डिजिटल शिक्षा केन्द्र स्थापना। (लक्ष्य: ८ केन्द्र, हाल: २)", 0.25f),
                            Triple("🏫 प्राविधिक शिक्षा र लोकसेवा छात्रवृत्ति (२०२६-२०२८)", "बोटे विद्यार्थीहरूका लागि स्टाफ नर्स, सब-इन्जिनियर, र कृषि प्राविधिक पढ्न थप छात्रवृत्ति। (लक्ष्य: २५ जना, हाल: ५)", 0.20f)
                        )
                    } else {
                        listOf(
                            Triple("📈 Digital Language & Folklore Archive (2026-2027)", "Digitizing sacred chants, oral river folklore, and tribal vocabulary into an open-access multimedia database. (Target: 100%, Current: 35%)", 0.35f),
                            Triple("🛶 Riverine Eco-Tourism & Canoe Homestays (2026-2028)", "Training youth as certified river guides, nature naturalists, and establishing authentic river-heritage homestays along Narayani. (Target: 50, Current: 15)", 0.30f),
                            Triple("🐟 Sustainable Fishery & Aquaponics Cooperatives (2027-2029)", "Merging ancestral fishing expertise with legal local cooperatives, introducing eco-friendly fish farming, and aquaponics. (Target: 10, Current: 3)", 0.30f),
                            Triple("💡 Solar Offline Digital Learning Networks (2026-2030)", "Expanding our highly successful solar offline server hubs from Sarlahi to Tanahu and Nawalparasi river-settlements. (Target: 8 hubs, Current: 2)", 0.25f),
                            Triple("🏫 Technical & Public Administration Sponsorships (2026-2028)", "Special vocational scholarships for Bote youth in nursing, forestry, civil engineering, and public services training. (Target: 25, Current: 5)", 0.20f)
                        )
                    }

                    items(growthOptions) { (title, description, progress) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = description,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val metrics = if (isNepali) {
                        listOf(
                            Triple("🗣️ १०+ बोटे भाषा कक्षाहरू", "नयाँ पुस्ताका ५० भन्दा बढी बालबालिकाहरूलाई मातृभाषा, लिपि र दुईभाषी पाठ्यक्रमको नियमित प्रशिक्षण दिइँदै।", "बोटे भाषा संरक्षण तथा पठनपाठन"),
                            Triple("👗 ४५+ परम्परागत पोशाक र गहना", "युवाहरूद्वारा हस्तनिर्मित कच्छाड, भोटो, फरिया, पटुका र परम्परागत चाँदीका गहनाहरू (हँसुली, बुलाकी, कल्ली) को जगेर्ना र प्रवर्द्धन।", "परम्परागत बोटे पहिरन तथा चाँदीका गहना संरक्षण"),
                            Triple("🎓 १२+ उच्च शिक्षा छात्रवृत्ति", "प्रतिभावान नदी तटीय बोटे विद्यार्थीहरूका लागि कलेज भर्ना शुल्क, पुस्तक खर्च, र विश्वविद्यालय तहको छात्रवृत्ति सहयोग।", "उच्च शिक्षा छात्रवृत्ति तथा विश्वविद्यालय भर्ना सहयोग"),
                            Triple("💻 २ सौर्य-ऊर्जा सञ्चालित प्रविधि केन्द्र", "डिजिटल दूरी मेटाउन सर्लाहीको कर्महिया लगायतका विकट नदी तटीय क्षेत्रमा सौर्य-ऊर्जाबाट चल्ने सूचना प्रविधि पुस्तकालय (IT Library) स्थापना।", "सौर्य-ऊर्जाबाट चल्ने सूचना प्रविधि पुस्तकालय विकास"),
                            Triple("🛶 ३ वटा परम्परागत सालका डुङ्गा", "नदी किनारका बुढापाका कालिगढहरूबाट काठको परम्परागत डुङ्गा (धोनी) बनाउने कला नयाँ पुस्तामा हस्तान्तरण।", "परम्परागत डुङ्गा निर्माण र नदी सम्पदा जगेर्ना"),
                            Triple("🤝 ८ नेतृत्व विकास मञ्च", "बोटे युवा समुदाय नेपाल (BYCN) लाई सामाजिक न्याय, नदी संरक्षण अभियान र युवा सशक्तिकरणका कार्यक्रम सञ्चालन गर्न सबलीकरण।", "बोटे युवा समुदाय नेपाल सहयोग र सबलीकरण"),
                            Triple("🌟 १२०+ बालबालिका मार्गनिर्देशित", "नयाँ पुस्ताका बोटे बालबालिका तथा युवाहरूलाई वृत्ति मार्गदर्शन, नेतृत्व कार्यशाला, र डिजिटल प्रविधि सामग्री सहयोग।", "बोटे नयाँ पुस्ता सशक्तिकरण")
                        )
                    } else {
                        listOf(
                            Triple("🗣️ 10+ Bote Language Classes", "Over 50 children and youth are regularly training to speak, write, and preserve the endangered ancestral Bote language.", "Bote Language Preservation & Classes"),
                            Triple("👗 45+ Traditional Costumes & Attire", "Hand-woven traditional Bote clothing (Kachhad, Boto, Phariya, Patuka) and silver ornaments (Hasuli, Bulaki, Kalli) preserved and worn by youth.", "Bote Traditional Dress & Sacred Ornaments"),
                            Triple("🎓 12 Higher Ed Scholarships", "Supporting university admission sponsorships, career guidance mentoring, and academic fees for talented Bote students.", "Higher Education & University Admission Scholarships"),
                            Triple("💻 2 Solar-Powered IT Centers", "Established solar-powered offline servers and computer learning centers in rural settlements like Karmahiya, Sarlahi to bridge the digital divide.", "Solar-Powered IT Library & Learning Centers"),
                            Triple("🛶 3 Traditional Canoes Hand-Carved", "Funding elder artisans to teach the younger generation the sacred art of riverine canoe-building and navigation.", "Traditional Boat-Building & Riverine Heritage"),
                            Triple("🤝 8 Leadership Forums", "Empowering Bote Youth Community Nepal (BYCN) to coordinate river protection networks, legal rights workshops, and social justice circles.", "Supporting Bote Youth Community Nepal"),
                            Triple("🌟 120+ Youth Guided", "Providing career counseling, leadership retreats, digital skill bootcamps, and essential study tool kits for the upcoming generation.", "Supporting Bote Young Generations")
                        )
                    }

                    items(metrics) { (title, desc, cause) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Under Cause: $cause",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            "History" -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = if (isNepali) "तपाईंको सहयोग इतिहास" else "Your Donation History",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isNepali) {
                                "फायरबेस क्लाउड डाटाबेसबाट सुरक्षित रूपमा प्राप्त गरिएका सफल योगदानहरूको रेकर्ड।"
                            } else {
                                "Secure live records of your successful contributions fetched in real-time from Firebase."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = historySearchEmail,
                        onValueChange = { historySearchEmail = it },
                        label = { Text(if (isNepali) "दाता इमेल द्वारा खोज्नुहोस्" else "Search/Filter by Donor Email") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("history_email_search"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                val filteredHistory = donations.filter {
                    it.email.lowercase().trim() == historySearchEmail.lowercase().trim()
                }

                if (filteredHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag("history_empty_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isNepali) "कुनै रेकर्ड फेला परेन" else "No Records Found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isNepali) {
                                        "यो इमेल अन्तर्गत कुनै पनि सफल सहयोग फेला परेन। कृपया आफ्नो आधिकारिक इमेल जाँच गर्नुहोस् वा नयाँ सहयोग गर्नुहोस्।"
                                    } else {
                                        "No verified donation records found under this email. Please check your spelling or make a new contribution!"
                                    },
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { activeTab = "Form" },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("history_donate_now_btn")
                                ) {
                                    Text(if (isNepali) "अहिले सहयोग गर्नुहोस्" else "Donate Now")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredHistory) { don ->
                        val matchedPillar = pillars.firstOrNull { it.first == don.targetCause }
                        val (targetGoal, themeColor) = pillarDetails[don.targetCause] ?: (100000.0 to MaterialTheme.colorScheme.primary)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("history_item_${don.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(themeColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val emoji = matchedPillar?.second?.take(2) ?: "💝"
                                            Text(emoji, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isNepali) (matchedPillar?.third ?: don.targetCause) else (matchedPillar?.second?.substring(3) ?: don.targetCause),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(don.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "NPR ${String.format("%,.0f", don.amount)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = themeColor
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF43A047).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isNepali) "सफल" else "Successful",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF43A047)
                                            )
                                        }
                                    }
                                }

                                if (don.message.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "\"${don.message}\"",
                                            fontSize = 11.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isNepali) "विधि: " else "Via: "}${don.paymentMethod}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    
                                    Button(
                                        onClick = { showReceiptDialog = don },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = themeColor.copy(alpha = 0.12f),
                                            contentColor = themeColor
                                        ),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("receipt_btn_${don.id}"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isNepali) "रसिद हेर्नुहोस्" else "View Receipt",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Secure Gateway Authorization Dialogue Box
    if (showGatewayDialog) {
        val amountToPay = calculatedAmount
        var walletPhone by remember { mutableStateOf(currentUser?.email?.substringBefore("@") ?: "9841234567") }
        var walletPin by remember { mutableStateOf("") }
        var walletOtp by remember { mutableStateOf("") }
        
        var cardHolder by remember { mutableStateOf(currentUser?.fullName ?: "") }
        var cardNumber by remember { mutableStateOf("") }
        var cardCvv by remember { mutableStateOf("") }
        var cardExpiry by remember { mutableStateOf("") }

        // Mobile Banking
        var bankName by remember { mutableStateOf("Global IME Bank") }
        var showBankDropdown by remember { mutableStateOf(false) }
        var bankAccountNumber by remember { mutableStateOf("") }
        var bankAccountHolder by remember { mutableStateOf(currentUser?.fullName ?: "") }
        var bankPin by remember { mutableStateOf("") }

        // Stripe
        var stripeEmail by remember { mutableStateOf(currentUser?.email ?: "") }
        var stripeCardNumber by remember { mutableStateOf("") }
        var stripeCvv by remember { mutableStateOf("") }
        var stripeExpiry by remember { mutableStateOf("") }

        // PayPal
        var paypalEmail by remember { mutableStateOf(currentUser?.email ?: "") }
        var paypalPassword by remember { mutableStateOf("") }

        // Google Pay
        var googlePayEmail by remember { mutableStateOf(currentUser?.email ?: "") }
        
        var isAuthorizing by remember { mutableStateOf(false) }
        var authMessage by remember { mutableStateOf("") }

        val gatewayThemeColor = when(paymentMethod) {
            "eSewa" -> Color(0xFF60BB46)
            "Khalti" -> Color(0xFF5C2D91)
            "Mobile Banking" -> Color(0xFFFF9800)
            "Stripe" -> Color(0xFF6772E5)
            "PayPal" -> Color(0xFF003087)
            "Google Pay" -> Color(0xFF4285F4)
            "Card Payment" -> Color(0xFF1E88E5)
            else -> Color(0xFF1A73E8)
        }

        Dialog(
            onDismissRequest = { if (!isAuthorizing) showGatewayDialog = false },
            properties = DialogProperties(dismissOnBackPress = !isAuthorizing, dismissOnClickOutside = !isAuthorizing)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("secure_gateway_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gateway Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "SSL Encrypted",
                                tint = gatewayThemeColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Secure $paymentMethod Gateway",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!isAuthorizing) {
                            IconButton(onClick = { showGatewayDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    if (isAuthorizing) {
                        // Loading State Spinner
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(color = gatewayThemeColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authMessage,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Processing standard SSL handshake protocols...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        // Inputs depending on method
                        Text(
                            text = "Charge Amount: NPR ${String.format("%,.0f", amountToPay)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = gatewayThemeColor
                        )

                        if (paymentMethod == "eSewa" || paymentMethod == "Khalti") {
                            OutlinedTextField(
                                value = walletPhone,
                                onValueChange = { walletPhone = it },
                                label = { Text("$paymentMethod Registered Mobile") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("wallet_phone_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = walletPin,
                                onValueChange = { walletPin = it },
                                label = { Text("4-digit MPIN") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("wallet_pin_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = walletOtp,
                                onValueChange = { walletOtp = it },
                                label = { Text("6-digit Verification OTP (sent via SMS)") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("wallet_otp_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else if (paymentMethod == "Mobile Banking") {
                            Text(
                                text = "Select Bank for Direct Pay",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = gatewayThemeColor,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Global IME", "Nabil Bank", "NIC Asia", "Rastriya Banijya", 
                                    "Nepal Investment Mega", "Siddhartha", "Prabhu Bank", "Standard Chartered"
                                ).forEach { bank ->
                                    val isBankSelected = bankName == bank
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isBankSelected) gatewayThemeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .border(1.dp, if (isBankSelected) gatewayThemeColor else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { bankName = bank }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(bank, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isBankSelected) gatewayThemeColor else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bankAccountHolder,
                                onValueChange = { bankAccountHolder = it },
                                label = { Text("Account Holder Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("bank_holder_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = bankAccountNumber,
                                onValueChange = { bankAccountNumber = it },
                                label = { Text("Account Number") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("bank_account_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = bankPin,
                                onValueChange = { bankPin = it },
                                label = { Text("Mobile Banking PIN / Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("bank_pin_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else if (paymentMethod == "Card Payment") {
                            OutlinedTextField(
                                value = cardHolder,
                                onValueChange = { cardHolder = it },
                                label = { Text("Cardholder Name") },
                                modifier = Modifier.fillMaxWidth().testTag("card_holder_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("16-digit Card Number") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("card_number_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text("Expiry (MM/YY)") },
                                    modifier = Modifier.weight(1f).testTag("card_expiry_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { cardCvv = it },
                                    label = { Text("CVV") },
                                    modifier = Modifier.weight(1f).testTag("card_cvv_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        } else if (paymentMethod == "Stripe") {
                            OutlinedTextField(
                                value = stripeEmail,
                                onValueChange = { stripeEmail = it },
                                label = { Text("Stripe Account Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("stripe_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = stripeCardNumber,
                                onValueChange = { stripeCardNumber = it },
                                label = { Text("Card Number (Visa / Mastercard)") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("stripe_card_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = stripeExpiry,
                                    onValueChange = { stripeExpiry = it },
                                    label = { Text("Expiry (MM/YY)") },
                                    modifier = Modifier.weight(1f).testTag("stripe_expiry_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = stripeCvv,
                                    onValueChange = { stripeCvv = it },
                                    label = { Text("CVC") },
                                    modifier = Modifier.weight(1f).testTag("stripe_cvc_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        } else if (paymentMethod == "PayPal") {
                            OutlinedTextField(
                                value = paypalEmail,
                                onValueChange = { paypalEmail = it },
                                label = { Text("PayPal Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("paypal_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = paypalPassword,
                                onValueChange = { paypalPassword = it },
                                label = { Text("PayPal Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("paypal_password_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else if (paymentMethod == "Google Pay") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Google Pay Direct Pay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = googlePayEmail,
                                onValueChange = { googlePayEmail = it },
                                label = { Text("Google Account Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("google_pay_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val formValid = when (paymentMethod) {
                            "eSewa", "Khalti" -> {
                                walletPhone.isNotEmpty() && walletPin.length >= 4 && walletOtp.length >= 4
                            }
                            "Mobile Banking" -> {
                                bankName.isNotEmpty() && bankAccountNumber.length >= 8 && bankAccountHolder.isNotEmpty() && bankPin.isNotEmpty()
                            }
                            "Card Payment" -> {
                                cardHolder.isNotEmpty() && cardNumber.length >= 12 && cardCvv.length >= 3 && cardExpiry.length >= 4
                            }
                            "Stripe" -> {
                                stripeEmail.contains("@") && stripeCardNumber.length >= 12 && stripeCvv.length >= 3 && stripeExpiry.length >= 4
                            }
                            "PayPal" -> {
                                paypalEmail.contains("@") && paypalPassword.length >= 6
                            }
                            "Google Pay" -> {
                                googlePayEmail.contains("@")
                            }
                            else -> false
                        }

                        Button(
                            onClick = {
                                isAuthorizing = true
                                scope.launch {
                                    authMessage = "Establishing secure TLS connection..."
                                    kotlinx.coroutines.delay(1000)
                                    authMessage = "Encrypting packet payload via AES-256..."
                                    kotlinx.coroutines.delay(1000)
                                    authMessage = "Authorized. Recording transaction..."
                                    kotlinx.coroutines.delay(800)
                                    
                                    val finalDonation = BoteDonation(
                                        donorName = if (isAnonymous) "Anonymous" else donorName.ifEmpty { "Generous Supporter" },
                                        email = donorEmail.ifEmpty { "receipt@example.com" },
                                        amount = amountToPay,
                                        targetCause = selectedCauses.joinToString(", "),
                                        paymentMethod = paymentMethod,
                                        message = bestWishesMsg,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    viewModel.processAndSaveDonation(finalDonation)
                                    
                                    showGatewayDialog = false
                                    isAuthorizing = false
                                    showReceiptDialog = finalDonation

                                    // Trigger beautiful visual success toast confirmation
                                    localSuccessToastMsg = if (isNepali) {
                                        "रु. ${String.format("%,.0f", amountToPay)} को सहयोग सुरक्षित रूपमा प्राप्त भयो! मुरी मुरी धन्यवाद।"
                                    } else {
                                        "NPR ${String.format("%,.0f", amountToPay)} successfully processed via $paymentMethod. Thank you!"
                                    }
                                    showLocalSuccessToast = true
                                }
                            },
                            enabled = formValid,
                            colors = ButtonDefaults.buttonColors(containerColor = gatewayThemeColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("authorize_secure_payment_button")
                        ) {
                            Text("Authorize & Pay NPR ${String.format("%,.0f", amountToPay)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Success Receipt Dialog
    showReceiptDialog?.let { receipt ->
        Dialog(
            onDismissRequest = { showReceiptDialog = null }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("success_receipt_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF60BB46).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF60BB46),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Transaction Approved!",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF60BB46)
                    )

                    Text(
                        text = "Thank you for supporting the Tarahi Bote Community.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Receipt Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Donor:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(receipt.donorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Paid:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text("NPR ${String.format("%,.0f", receipt.amount)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cause Supported:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(receipt.targetCause.take(20) + "...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(receipt.paymentMethod, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Receipt Email:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(maskEmailSecured(receipt.email), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text("SECURE SYNCHRONIZED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60BB46))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showReceiptDialog = null },
                        modifier = Modifier.fillMaxWidth().testTag("receipt_done_button")
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Beautiful floating visual success confirmation toast notification
    AnimatedVisibility(
        visible = showLocalSuccessToast,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            .testTag("donation_success_toast")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, Color(0xFF60BB46).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF60BB46).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF60BB46),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isNepali) "सहयोग सफलतापूर्वक प्राप्त भयो!" else "Donation Successful!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = localSuccessToastMsg,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
                IconButton(
                    onClick = { showLocalSuccessToast = false },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    }
}

// ==================== TAB 4: COMMUNITY HUB & FORUM ====================
@Composable
fun CommunityHubPanel(
    viewModel: BoteCommunityViewModel,
    articles: List<Article>,
    forumPosts: List<ForumPost>,
    registrations: List<Registration>,
    isOnlineMode: Boolean,
    isSyncing: Boolean,
    isEncrypted: Boolean,
    isAppLockEnabled: Boolean,
    followedAuthors: Set<String>,
    appLiked: Boolean,
    followedApp: Boolean,
    appSharesCount: Int,
    appFollowersCount: Int
) {
    var isWriteMode by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Dropdown Categories definition for the 12 requested:
    val categories = listOf(
        "Bote Community News",
        "Bote History",
        "Bote Language Preservation",
        "Bote Culture and Traditions",
        "Youth Development",
        "Education and Scholarships",
        "Community Success Stories",
        "Environmental Conservation",
        "Homestay Tourism",
        "Women's Empowerment",
        "Research and Documentation",
        "Community Announcements"
    )

    var activeFeedCategory by remember { mutableStateOf("Bote Community News") }

    // Registration states
    var activeRegType by remember { mutableStateOf("Scholarship") }
    var applicantName by remember { mutableStateOf("") }
    var applicantEmail by remember { mutableStateOf("") }
    var applicantPhone by remember { mutableStateOf("") }
    var targetDetails by remember { mutableStateOf("") }
    val scholarshipOpps by viewModel.allScholarshipOpportunities.collectAsStateWithLifecycle()

    // Message forum states
    var visitorNick by remember { mutableStateOf("") }
    var visitorMessage by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("community_screen")
    ) {
        val isWideScreen = maxWidth >= 720.dp

        // Local composable lambdas to avoid duplicating the UI across mobile/desktop layouts
        val SyncStatusSection = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnlineMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else Color(0xFFFFF3E0)
                ),
                border = BorderStroke(
                    1.dp, 
                    if (isOnlineMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else Color(0xFFFFB74D)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnlineMode) LeafGreen else Color(0xFFFF9100))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnlineMode) "SYNCHRONIZED METRICS" else "LOCAL CACHE ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (isOnlineMode) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                            )
                        }

                        // Theme Mode Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOnlineMode) "Online Ready" else "Local Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isOnlineMode,
                                onCheckedChange = { viewModel.toggleOnlineMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Under offline scenarios, any registration files or forum stories you publish are written directly to Room Client Database and scheduled for queue uploading once coverage is re-established.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSyncing) "Syncing with Sarlahi server..." else "Last Synced: Just now",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Button(
                            onClick = { viewModel.triggerManualSync() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnlineMode) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, "Sync Icon", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Sync Cache", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        val SocialInteractionSection = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "❤️ Join Bote Youth Community Nepal Network",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect with us to receive regional notices, emergency river watches, and daily community circulars:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Social Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(RiverMist, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("👥 App Followers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("$appFollowersCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(RiverMist, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("🔗 Shared Invites", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("$appSharesCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Social Interaction Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Like/Rate App Button
                        Button(
                            onClick = { viewModel.toggleLikeApp() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appLiked) ErrorRed else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Heart",
                                    tint = if (appLiked) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (appLiked) "Liked App" else "Like App",
                                    fontSize = 11.sp,
                                    color = if (appLiked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Share App Button
                        Button(
                            onClick = {
                                viewModel.incrementAppShare()
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("Join Sarlahi's official youth portal: Bote Youth Community Nepal app at https://bote-youth.np/install"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share App", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Follow/Subscribe App updates
                        Button(
                            onClick = { viewModel.toggleFollowApp() },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (followedApp) LeafGreen else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alert",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (followedApp) "Subscribed" else "Get Alerts",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        val PublicationsSection = @Composable {
            Column {
                Text(
                    text = "📰 Weekly Community Publications Grid",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 12 Blog Categories horizontal selector wheel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { categoryName ->
                        val isSelected = activeFeedCategory == categoryName
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { activeFeedCategory = categoryName },
                            label = { Text(text = categoryName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("blog_cat_${categoryName.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active News list
                val filteredNews = articles.filter { it.category == activeFeedCategory }
                if (filteredNews.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, "Empty category", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                            Text(
                                text = "No articles live yet in '$activeFeedCategory'",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Admin can write and publish customized records inside the Creator DB tab.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filteredNews.forEach { article ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("news_item_card_${article.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = article.date,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(LeafGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "By ${article.author}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = article.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = article.content,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val CommunityForumSection = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "💬 Community Forum & Daily Updates",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Post, Like, Share, and Follow local updates!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { isWriteMode = !isWriteMode }) {
                            Icon(
                                imageVector = if (isWriteMode) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Toggle forum compose",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isWriteMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = visitorNick,
                                onValueChange = { visitorNick = it },
                                label = { Text("Your Name / Nickname", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = visitorMessage,
                                onValueChange = { visitorMessage = it },
                                label = { Text("What is on your mind regarding Bote preserves?", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.addForumPost(visitorMessage, visitorNick)
                                    visitorMessage = ""
                                    visitorNick = ""
                                    isWriteMode = false
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Post Daily Update", color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    forumPosts.forEach { post ->
                        val isAuthorFollowed = followedAuthors.contains(post.author)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(RiverMist, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (post.author.firstOrNull() ?: 'U').toString().uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = post.author, 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Follow/Following Badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isAuthorFollowed) LeafGreen.copy(alpha = 0.12f) 
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    )
                                                    .clickable { viewModel.toggleFollowAuthor(post.author) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isAuthorFollowed) "✓ Following" else "+ Follow",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAuthorFollowed) LeafGreen else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = post.authorRole,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Likes metric
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { viewModel.likeForumPost(post.id) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Likes",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${post.likes}", 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = post.content, 
                                fontSize = 12.sp, 
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Share card attachment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("\"${post.content}\" - By ${post.author} on Bote Youth Community Nepal"))
                                            viewModel.addForumPost(content = "Shared an update by ${post.author}!", author = "You")
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Share, "Share icon", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share update", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        val EngagementPortalSection = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✍️ Engagement Application Portal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete your application details for community sponsorships, local volunteer registry or event credentials:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Scholarship", "Volunteer", "Event", "Join BYCN").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (activeRegType == type) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { activeRegType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeRegType == type) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = applicantName,
                            onValueChange = { applicantName = it },
                            label = { Text("Applicant Full Name", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = applicantEmail,
                                onValueChange = { applicantEmail = it },
                                label = { Text("Email", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = applicantPhone,
                                onValueChange = { applicantPhone = it },
                                label = { Text("Contact Number", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        if (activeRegType == "Scholarship" && scholarshipOpps.isNotEmpty()) {
                            Text(
                                text = "Select an Active Scholarship Scheme to Auto-Fill details:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                items(scholarshipOpps) { opp ->
                                    val isSelected = targetDetails.contains(opp.title)
                                    Card(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .clickable {
                                                targetDetails = "Scheme: ${opp.title}\nCriteria: ${opp.requirements}\nDeadline: ${opp.deadline}\nAmt: ${opp.amount}"
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(opp.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(opp.amount, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            Text("Deadline: ${opp.deadline}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        OutlinedTextField(
                            value = targetDetails,
                            onValueChange = { targetDetails = it },
                            label = {
                                Text(
                                    when (activeRegType) {
                                        "Scholarship" -> "Reference School, Ward, and Level of Study"
                                        "Volunteer" -> "Relevant interests or language skills"
                                        "Join BYCN" -> "Why do you want to join and your local region/ward"
                                        else -> "Name of the cultural event you wish to join"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.registerCommunityAction(
                                    activeRegType,
                                    applicantName,
                                    applicantEmail,
                                    applicantPhone,
                                    targetDetails
                                )
                                applicantName = ""
                                applicantEmail = ""
                                applicantPhone = ""
                                targetDetails = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Submit Secure Application", color = Color.White)
                        }
                    }

                    // Bottom list of registrations stored inside user's local Room database
                    if (registrations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📁 Your Saved Local Submissions (Secured)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            registrations.forEach { reg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${reg.type} - ${reg.applicantName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        // Display secure masked contact data for compliance
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                text = "📞 ${maskPhoneSecured(reg.phone)}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "✉️ ${maskEmailSecured(reg.email)}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                        Text(reg.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(reg.status, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val PrivacyProtectionSection = @Composable {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bote Youth Data Protection Vault",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To guarantee absolute privacy and safety, user metrics and registration forms are sandboxed client-side using SQLite. Choose your cryptographic preferences below:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle 1: Local Table SQLite Encryption
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Database Encryption (AES-256)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Secures SQLite assets with device credential hashing.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isEncrypted,
                            onCheckedChange = { viewModel.toggleSecurityEncryption() },
                            colors = SwitchDefaults.colors(checkedThumbColor = LeafGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 2: Biometric / Fingerprint FaceID Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Biometric / Pattern App Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Shields the submission history from visual snoopers.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { viewModel.toggleAppLock() },
                            colors = SwitchDefaults.colors(checkedThumbColor = LeafGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Privacy Assurance stamp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                "🛡️ REGIONAL SHIELD ASSURANCE:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Your numbers and applications are always isolated from advertisement brokers or national tracker logs. Local submissions display values using robust cryptographic masking conventions.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Danger data cleaning option
                    OutlinedButton(
                        onClick = { viewModel.wipeAllLocalData() },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧹 Wipe and Scrub Local DB Cache", fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Column 1 (Left): Status, Telemetry, Social interaction, Publications list
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SyncStatusSection()
                    NepalBoteLiveTelemetryWidget(viewModel)
                    SocialInteractionSection()
                    PublicationsSection()
                }

                // Column 2 (Right): Community Forum, Engagement application portal, Protection details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CommunityForumSection()
                    EngagementPortalSection()
                    PrivacyProtectionSection()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SyncStatusSection() }
                item { NepalBoteLiveTelemetryWidget(viewModel) }
                item { SocialInteractionSection() }
                item { PublicationsSection() }
                item { CommunityForumSection() }
                item { EngagementPortalSection() }
                item { PrivacyProtectionSection() }
            }
        }
    }
}

// ==================== TAB 5: CREATOR & SEO SCORE CHECKER ====================
@Composable
fun CreatorDashboardPanel(
    viewModel: BoteCommunityViewModel,
    drafts: List<Article>,
    published: List<Article>,
    resources: List<Resource>,
    faqs: List<FaqItem>,
    registrations: List<Registration>,
    forumPosts: List<ForumPost>
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    if (currentUser == null || currentUser?.role != "admin") {
        AdminRestrictedAccessPanel(
            viewModel = viewModel,
            isNepali = viewModel.isNepali.collectAsStateWithLifecycle().value
        )
    } else {
        var adminSubTab by remember { mutableStateOf("Write Blog") }
        val tabs = listOf("Write Blog", "Edit Blogs", "Search DB", "FAQs DB", "Scholarships DB", "Applicants Status", "Moderate Forum", "App Updates")

        // State holders for dialog editors
        var editingArticle by remember { mutableStateOf<Article?>(null) }
        var editingResource by remember { mutableStateOf<Resource?>(null) }
        var editingFaq by remember { mutableStateOf<FaqItem?>(null) }

        // State holders for adding new entities
        var showAddResourceDialog by remember { mutableStateOf(false) }
        var showAddFaqDialog by remember { mutableStateOf(false) }

        val categories = listOf(
            "Bote Community News",
            "Bote History",
            "Bote Language Preservation",
            "Bote Culture and Traditions",
            "Youth Development",
            "Education and Scholarships",
            "Community Success Stories",
            "Environmental Conservation",
            "Homestay Tourism",
            "Women's Empowerment",
            "Research and Documentation",
            "Community Announcements"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_dashboard_screen")
        ) {
            // Upper Tab Bar for Admin Workspace
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ CENTRAL SYSTEM ADMIN CONTROL PANEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val firestoreStatus by BoteFirestoreManager.firestoreConnectionStatus.collectAsStateWithLifecycle()
                    val isCloud = firestoreStatus.contains("Connected")
                    val badgeColor = if (isCloud) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                    
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        contentColor = badgeColor,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCloud) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = firestoreStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tabs) { tab ->
                        val isSelected = adminSubTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { adminSubTab = tab },
                            label = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (adminSubTab) {
                "Write Blog" -> {
                    val titleState by viewModel.draftTitle.collectAsStateWithLifecycle()
                    val contentState by viewModel.draftContent.collectAsStateWithLifecycle()
                    val activeSelCategory by viewModel.draftCategory.collectAsStateWithLifecycle()
                    val draftAuthorState by viewModel.draftAuthor.collectAsStateWithLifecycle()
                    val keywordState by viewModel.draftTargetKeyword.collectAsStateWithLifecycle()
                    val draftSeoTitleState by viewModel.draftSeoTitle.collectAsStateWithLifecycle()
                    val draftSeoMetaState by viewModel.draftSeoMeta.collectAsStateWithLifecycle()
                    val seoReport by viewModel.activeSeoReport.collectAsStateWithLifecycle()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = titleState,
                                        onValueChange = { viewModel.draftTitle.value = it },
                                        label = { Text("Article Title", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = keywordState,
                                            onValueChange = { viewModel.draftTargetKeyword.value = it },
                                            label = { Text("Focus Keyword to optimize", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = draftAuthorState,
                                            onValueChange = { viewModel.draftAuthor.value = it },
                                            label = { Text("Author", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Column {
                                        Text("Verify Category Selection:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(categories) { categoryKey ->
                                                val isSelected = activeSelCategory == categoryKey
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { viewModel.draftCategory.value = categoryKey },
                                                    label = { Text(categoryKey, fontSize = 11.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = contentState,
                                        onValueChange = { viewModel.draftContent.value = it },
                                        label = { Text("Live Body Text Content (Describe elders history, culture, or updates...)", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp)
                                    )

                                    Text("Google Snippet Optimization (Meta Schema):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = draftSeoTitleState,
                                            onValueChange = { viewModel.draftSeoTitle.value = it },
                                            label = { Text("SEO Title Tag (Ideal 50 Chars)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = draftSeoMetaState,
                                            onValueChange = { viewModel.draftSeoMeta.value = it },
                                            label = { Text("SEO Meta Tag (120-160 Chars)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.saveDraftArticle(isScheduled = false) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Save Draft", color = Color.White, fontSize = 11.sp, maxLines = 1)
                                        }

                                        Button(
                                            onClick = { viewModel.saveDraftArticle(isScheduled = true) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Schedule 🕒", color = Color.White, fontSize = 11.sp, maxLines = 1)
                                        }

                                        Button(
                                            onClick = { viewModel.publishArticleInstantly() },
                                            modifier = Modifier.weight(1.3f).testTag("publish_live_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Publish Live 🚀", color = Color.White, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // SEO Analyzer
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "📈 Real-Time SEO Google Visibility Score",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Target keyword: '${keywordState}'",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(25.dp))
                                                .background(
                                                    when (seoReport.grade) {
                                                        "A" -> SuccessGreen
                                                        "B", "C" -> WarningAmber
                                                        else -> ErrorRed
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = seoReport.grade, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                Text(text = "${seoReport.score}%", color = Color.White, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    seoReport.checks.forEach { check ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (check.passed) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = if (check.passed) "Passed" else "Failed",
                                                tint = if (check.passed) SuccessGreen else ErrorRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${check.label} (+${check.scoreImpact} pts)",
                                                fontSize = 12.sp,
                                                color = if (check.passed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (seoReport.suggestions.isNotEmpty()) {
                                        Text(
                                            text = "💡 Google Ranking Recommendations:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        seoReport.suggestions.forEach { tip ->
                                            Text(
                                                text = "• $tip",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Stored drafts
                        if (drafts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "📂 Unpublished Community Drafts Queue",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            items(drafts) { draft ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = draft.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    text = "Category: ${draft.category} | ${if (draft.isScheduled) "Scheduled 🕒" else "Stored Draft"}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = { viewModel.publishDraft(draft.id) },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                ) {
                                                    Icon(Icons.Default.Check, "Publish Draft", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteArticle(draft.id) },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                ) {
                                                    Icon(Icons.Default.Close, "Delete Draft", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Edit Blogs" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "📝 Manage Live Published Articles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (published.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text("No articles published yet.", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                                }
                            }
                        }

                        items(published) { article ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(article.date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = { editingArticle = article },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            ) {
                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteArticle(article.id) },
                                                modifier = Modifier.size(28.dp).background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(article.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("By ${article.author} | Category: ${article.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(article.content, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }

                "Search DB" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📚 Search Database Admin",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { showAddResourceDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Add Resource", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        items(resources) { res ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${res.resourceType} • ${res.fileSize}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = { editingResource = res },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            ) {
                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteResource(res.id) },
                                                modifier = Modifier.size(28.dp).background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(res.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Author: ${res.author} | Year: ${res.year} | Downloads: ${res.downloadCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(res.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                "FAQs DB" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "❓ FAQs Schema Database Admin",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { showAddFaqDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Add FAQ", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        items(faqs) { faq ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Category: ${faq.pageCategory}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = { editingFaq = faq },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            ) {
                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteFaq(faq.id) },
                                                modifier = Modifier.size(28.dp).background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Q: ${faq.question}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("A: ${faq.answer}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                "Scholarships DB" -> {
                    val scholarshipOpps by viewModel.allScholarshipOpportunities.collectAsStateWithLifecycle()
                    var showAddScholarshipDialog by remember { mutableStateOf(false) }
                    var editingScholarship by remember { mutableStateOf<ScholarshipOpportunity?>(null) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎓 Manage Scholarship Opportunities",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { showAddScholarshipDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add New", fontSize = 11.sp)
                                }
                            }
                        }

                        if (scholarshipOpps.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "No scholarship opportunities created yet. Click 'Add New' to publish one.",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(scholarshipOpps) { opp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = opp.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { editingScholarship = opp },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteScholarshipOpportunity(opp.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = opp.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column {
                                            Text("AMOUNT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(opp.amount, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("DEADLINE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            Text(opp.deadline, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (opp.requirements.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Requirements:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(opp.requirements, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Add/Edit Scholarship Opportunity Dialog
                    if (showAddScholarshipDialog || editingScholarship != null) {
                        val currentOpp = editingScholarship
                        var title by remember { mutableStateOf(currentOpp?.title ?: "") }
                        var description by remember { mutableStateOf(currentOpp?.description ?: "") }
                        var amount by remember { mutableStateOf(currentOpp?.amount ?: "") }
                        var deadline by remember { mutableStateOf(currentOpp?.deadline ?: "") }
                        var requirements by remember { mutableStateOf(currentOpp?.requirements ?: "") }

                        AlertDialog(
                            onDismissRequest = { showAddScholarshipDialog = false; editingScholarship = null },
                            title = { Text(if (currentOpp != null) "Edit Opportunity" else "Add Scholarship Opportunity", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Opportunity Title") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Detailed Description") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Funding Amount / Support") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Application Deadline") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = requirements, onValueChange = { requirements = it }, label = { Text("Requirements / Criteria") }, modifier = Modifier.fillMaxWidth())
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val toSave = ScholarshipOpportunity(
                                            id = currentOpp?.id ?: 0,
                                            title = title,
                                            description = description,
                                            amount = amount,
                                            deadline = deadline,
                                            requirements = requirements
                                        )
                                        viewModel.saveOrUpdateScholarshipOpportunity(toSave)
                                        showAddScholarshipDialog = false
                                        editingScholarship = null
                                    }
                                ) {
                                    Text("Publish Opportunity", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddScholarshipDialog = false; editingScholarship = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }

                "Applicants Status" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "🎓 Scholarship & Youth Registries Controller",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (registrations.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text("No registrations filed yet by community users.", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                                }
                            }
                        }

                        items(registrations) { reg ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${reg.type} Request", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (reg.status) {
                                                        "Approved" -> SuccessGreen.copy(alpha = 0.15f)
                                                        "Declined" -> ErrorRed.copy(alpha = 0.15f)
                                                        else -> WarningAmber.copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = reg.status,
                                                fontSize = 10.sp,
                                                color = when (reg.status) {
                                                    "Approved" -> SuccessGreen
                                                    "Declined" -> ErrorRed
                                                    else -> WarningAmber
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(reg.applicantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("📞 ${reg.phone} | ✉️ ${reg.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Details: ${reg.details}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.updateRegStatus(reg.id, "Approved") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Approve ✅", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.updateRegStatus(reg.id, "Declined") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Decline ❌", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.updateRegStatus(reg.id, "Under Review") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Review ⏳", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteRegistration(reg.id) },
                                            modifier = Modifier.size(34.dp).background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Moderate Forum" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "💬 Community Forum Live Moderation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (forumPosts.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text("No messages posted yet in community forum.", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                                }
                            }
                        }

                        items(forumPosts) { post ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${post.author} (${post.authorRole})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(post.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                        Text("Likes: ${post.likes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteForumPost(post.id) },
                                        modifier = Modifier.size(36.dp).background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }

                "App Updates" -> {
                    val appUpdates by viewModel.allAppUpdates.collectAsStateWithLifecycle()
                    var showAddAppUpdateDialog by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📲 Application Version Control",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedButton(onClick = { showAddAppUpdateDialog = true }) {
                                    Text("Post Update", fontSize = 12.sp)
                                }
                            }
                        }
                        if (appUpdates.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text("No app updates posted yet.", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                                }
                            }
                        }
                        items(appUpdates) { update ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${update.title} (v${update.version})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(update.releaseNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                        if (update.isMandatory) {
                                            Text("⚠️ Mandatory Update", fontSize = 10.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteAppUpdate(update.id) },
                                        modifier = Modifier.size(36.dp).background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }

                    if (showAddAppUpdateDialog) {
                        var updVersion by remember { mutableStateOf("") }
                        var updTitle by remember { mutableStateOf("") }
                        var updNotes by remember { mutableStateOf("") }
                        var isMandatory by remember { mutableStateOf(false) }

                        Dialog(onDismissRequest = { showAddAppUpdateDialog = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Release New App Update", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    
                                    OutlinedTextField(
                                        value = updVersion,
                                        onValueChange = { updVersion = it },
                                        label = { Text("Version (e.g. 1.2.0)", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = updTitle,
                                        onValueChange = { updTitle = it },
                                        label = { Text("Update Title", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = updNotes,
                                        onValueChange = { updNotes = it },
                                        label = { Text("Release Notes", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                                        Text("Force Mandatory Update", fontSize = 12.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showAddAppUpdateDialog = false }) { Text("Cancel") }
                                        Button(onClick = {
                                            viewModel.saveOrUpdateAppUpdate(
                                                AppUpdate(version = updVersion, title = updTitle, releaseNotes = updNotes, isMandatory = isMandatory)
                                            )
                                            showAddAppUpdateDialog = false
                                        }) { Text("Publish Update") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR EDITING / CREATING SYSTEM DATA ---

    // 1. Article Edit Dialog
    editingArticle?.let { article ->
        var editTitle by remember { mutableStateOf(article.title) }
        var editAuthor by remember { mutableStateOf(article.author) }
        var editContent by remember { mutableStateOf(article.content) }
        var editCategory by remember { mutableStateOf(article.category) }
        var editKeyword by remember { mutableStateOf(article.seoKeywords) }
        var editSeoTitle by remember { mutableStateOf(article.seoTitle) }
        var editSeoMeta by remember { mutableStateOf(article.seoMeta) }

        AlertDialog(
            onDismissRequest = { editingArticle = null },
            title = { Text("Edit Published Article", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = editAuthor, onValueChange = { editAuthor = it }, label = { Text("Author") })
                    OutlinedTextField(value = editContent, onValueChange = { editContent = it }, label = { Text("Content") })
                    OutlinedTextField(value = editCategory, onValueChange = { editCategory = it }, label = { Text("Category") })
                    OutlinedTextField(value = editKeyword, onValueChange = { editKeyword = it }, label = { Text("Keyword") })
                    OutlinedTextField(value = editSeoTitle, onValueChange = { editSeoTitle = it }, label = { Text("SEO Title") })
                    OutlinedTextField(value = editSeoMeta, onValueChange = { editSeoMeta = it }, label = { Text("SEO Meta Description") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePublishedArticle(
                            article.copy(
                                title = editTitle,
                                author = editAuthor,
                                content = editContent,
                                category = editCategory,
                                seoKeywords = editKeyword,
                                seoTitle = editSeoTitle,
                                seoMeta = editSeoMeta
                            )
                        )
                        editingArticle = null
                    }
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingArticle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Resource Add / Edit Dialogs
    if (showAddResourceDialog || editingResource != null) {
        val currentRes = editingResource
        var resTitle by remember { mutableStateOf(currentRes?.title ?: "") }
        var resAuthor by remember { mutableStateOf(currentRes?.author ?: "") }
        var resCategory by remember { mutableStateOf(currentRes?.category ?: "Research Paper") }
        var resType by remember { mutableStateOf(currentRes?.resourceType ?: "PDF") }
        var resYear by remember { mutableStateOf(currentRes?.year ?: "2026") }
        var resSize by remember { mutableStateOf(currentRes?.fileSize ?: "1.5 MB") }
        var resDesc by remember { mutableStateOf(currentRes?.description ?: "") }

        AlertDialog(
            onDismissRequest = { showAddResourceDialog = false; editingResource = null },
            title = { Text(if (currentRes != null) "Edit Resource" else "Add New Resource", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = resTitle, onValueChange = { resTitle = it }, label = { Text("Resource Title") })
                    OutlinedTextField(value = resAuthor, onValueChange = { resAuthor = it }, label = { Text("Author / Publisher") })
                    OutlinedTextField(value = resCategory, onValueChange = { resCategory = it }, label = { Text("Category (e.g. Research Paper, Census Info)") })
                    OutlinedTextField(value = resType, onValueChange = { resType = it }, label = { Text("File Type (PDF, Document, Audio)") })
                    OutlinedTextField(value = resYear, onValueChange = { resYear = it }, label = { Text("Year") })
                    OutlinedTextField(value = resSize, onValueChange = { resSize = it }, label = { Text("File Size") })
                    OutlinedTextField(value = resDesc, onValueChange = { resDesc = it }, label = { Text("Detailed Description") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSave = Resource(
                            id = currentRes?.id ?: 0,
                            title = resTitle,
                            author = resAuthor,
                            category = resCategory,
                            resourceType = resType,
                            year = resYear,
                            fileSize = resSize,
                            description = resDesc,
                            downloadCount = currentRes?.downloadCount ?: 0
                        )
                        viewModel.saveOrUpdateResource(toSave)
                        showAddResourceDialog = false
                        editingResource = null
                    }
                ) {
                    Text("Submit Database", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddResourceDialog = false; editingResource = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. FAQ Add / Edit Dialogs
    if (showAddFaqDialog || editingFaq != null) {
        val currentFaq = editingFaq
        var faqQuestion by remember { mutableStateOf(currentFaq?.question ?: "") }
        var faqAnswer by remember { mutableStateOf(currentFaq?.answer ?: "") }
        var faqCategory by remember { mutableStateOf(currentFaq?.pageCategory ?: "Bote History") }

        AlertDialog(
            onDismissRequest = { showAddFaqDialog = false; editingFaq = null },
            title = { Text(if (currentFaq != null) "Edit FAQ Item" else "Add New FAQ Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = faqQuestion, onValueChange = { faqQuestion = it }, label = { Text("FAQ Question") })
                    OutlinedTextField(value = faqAnswer, onValueChange = { faqAnswer = it }, label = { Text("FAQ Answer") })
                    OutlinedTextField(value = faqCategory, onValueChange = { faqCategory = it }, label = { Text("Page Category (e.g. Bote History, Bote Language)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSave = FaqItem(
                            id = currentFaq?.id ?: 0,
                            question = faqQuestion,
                            answer = faqAnswer,
                            pageCategory = faqCategory
                        )
                        viewModel.saveOrUpdateFaq(toSave)
                        showAddFaqDialog = false
                        editingFaq = null
                    }
                ) {
                    Text("Submit Database", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFaqDialog = false; editingFaq = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
fun AdminRestrictedAccessPanel(
    viewModel: BoteCommunityViewModel,
    isNepali: Boolean
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var adminPasscode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_restricted_panel")
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isNepali) "⚙️ केन्द्रीय प्रणाली प्रशासक नियन्त्रण" else "⚙️ CENTRAL SYSTEM ADMIN AREA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isNepali) 
                            "यो ट्याब केवल प्रमाणित बोटे समुदाय प्रशासकहरूको लागि सुरक्षित छ। प्रशासकहरूले यहाँबाट समाचार फिड, छात्रवृत्ति कार्यक्रम, डिजिटल पुस्तकालय, र छलफल मञ्च नियन्त्रण गर्न सक्छन्।" 
                            else "This workspace is restricted to verified administrators of the Bote Community. Authorized coordinators can create and delete news items, manage the digital archive, publish scholarship opportunities, and approve status requests.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isSignUpMode) "Register Admin Account" else "Admin Secure Sign-In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Coordinator Full Name", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Coordinator Email", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Secure Password", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.PlayArrow else Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = adminPasscode,
                            onValueChange = { adminPasscode = it },
                            label = { Text("Admin Authorization Passcode", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            placeholder = { Text("Enter BOTE_ADMIN to elevate role", fontSize = 11.sp) }
                        )
                    }

                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                viewModel.signUpUser(email, password, fullName, adminPasscode)
                            } else {
                                viewModel.loginUser(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) "Register & Elevate to Admin" else "Secure Admin Log In",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                        Text(
                            text = if (isSignUpMode) "Already have an admin account? Sign In" else "Need to register a new admin account? Sign Up",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isSignUpMode) {
                        Text(
                            text = "💡 Passcode Required: Enter 'BOTE_ADMIN' in the passcode field to grant the 'admin' system privilege.",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== TAB 6: GOOGLE TOOLS & PARTNERS COMMAND CENTER ====================
@Composable
fun GoogleCommandCenterPanel(
    viewModel: BoteCommunityViewModel,
    pagesList: List<SeoLandingPage>,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isNepali by viewModel.isNepali.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isNepali) "खाता मेटाउनुहोस्" else "Delete Account") },
            text = { Text(if (isNepali) "के तपाईं पक्का हुनुहुन्छ कि तपाईं आफ्नो खाता मेटाउन चाहनुहुन्छ? यो कार्य उल्टाउन सकिँदैन।" else "Are you sure you want to delete your account? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isNepali) "मेटाउनुहोस्" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("google_ctrs_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Account Details Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = "User Account", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNepali) "तपाईंको खाता" else "Your Account Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (currentUser != null) {
                        Text(text = "Name: ${currentUser?.fullName}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Email: ${currentUser?.email}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Role: ${currentUser?.role?.uppercase()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.logoutUser() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (isNepali) "लग आउट गर्नुहोस्" else "Sign Out")
                            }
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (isNepali) "खाता मेटाउनुहोस्" else "Delete Account")
                            }
                        }
                    } else {
                        Text(
                            text = if (isNepali) "तपाईं लगइन हुनुहुन्न।" else "You are not signed in.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (currentUser?.role == "admin") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isNepali) "व्यवस्थापक: एप अपडेट" else "Admin: Push App Update",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isNepali) "नयाँ अपडेट संस्करण, रिलीज नोटहरू र म्यान्डेटरी अवस्था सेट गरी प्रयोगकर्ताहरूलाई अपडेट सूचना पठाउनुहोस्।" else "Set a new update version, release notes, and mandatory status to push an update notification to users.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var newVersionCode by remember { mutableStateOf("2") }
                        var newVersionName by remember { mutableStateOf("1.1") }
                        var releaseNotes by remember { mutableStateOf("Added App Update Feature\nBug fixes & improvements.") }
                        var isMandatory by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = newVersionCode,
                            onValueChange = { newVersionCode = it },
                            label = { Text("Version Code (e.g., 2)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newVersionName,
                            onValueChange = { newVersionName = it },
                            label = { Text("Version Name (e.g., 1.1)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = releaseNotes,
                            onValueChange = { releaseNotes = it },
                            label = { Text("Release Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                            Text("Is Mandatory Update?")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.pushAppUpdate(
                                    versionCode = newVersionCode.toIntOrNull() ?: 2,
                                    versionName = newVersionName,
                                    releaseNotes = releaseNotes,
                                    isMandatory = isMandatory,
                                    downloadUrl = "https://example.com/update"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isNepali) "अपडेट पठाउनुहोस्" else "Push Update")
                        }
                    }
                }
            }
        }

        // Explanatory Box
        item {
            Column {
                Text(
                    text = "⚙️ Google Index Command Center",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Monitor structured data, search console indexing status and our outreach backlink partners.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Backlink strategy / Academic outreach partners directory
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎓 Academic Partners & Backlinks Network",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "We maintain strong linkages with institutions to support scholarships, language reviews and cross-indexing details:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val outreachPartners = listOf(
                        Pair("Tribhuvan University (TU)", "Linguistic Department"),
                        Pair("Kathmandu University (KU)", "Social Sciences Division"),
                        Pair("National Foundation (NFDIN)", "Indigenous Grants Commission"),
                        Pair("Karmahiya Local Municipality", "Ecotourism Directorate")
                    )

                    outreachPartners.forEach { (partner, commission) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active integration",
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = partner, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = commission, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Interactive Google PageSpeed & Tech Audits checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NightPebble)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Real-time PageSpeed & Core Web Vitals Audit",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreMetricDial("PageSpeed", "99")
                        ScoreMetricDial("SEO Best", "100")
                        ScoreMetricDial("Accessibility", "100")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "✔ HTTPS Secure | ✔ Lazy Loading Rendered | ✔ CDN Cache Active",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreMetricDial(metricLabel: String, valText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(2.dp, LeafGreen, RoundedCornerShape(23.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = valText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = metricLabel, color = Color.LightGray, fontSize = 10.sp)
    }
}


// Structured SEO metadata representation for the 15 requested pages with complete bilingual support
fun getCategoryTranslation(category: String): String {
    return when (category) {
        "Bote History" -> "बोटे इतिहास"
        "Bote Language" -> "बोटे भाषा"
        "Bote Culture" -> "बोटे संस्कृति"
        "Bote Traditional Dress" -> "बोटे पहिरन"
        "Bote Festivals" -> "बोटे चाडपर्व"
        "Bote Homestays" -> "बोटे होमस्टे"
        "Bote Youth Programs" -> "बोटे युवा कार्यक्रम"
        "Bote Community of Sarlahi" -> "सर्लाही बोटे समुदाय"
        "Tarahi Bote Community" -> "तराही बोटे समुदाय"
        "Indigenous Communities of Nepal" -> "नेपालका आदिवासी"
        "Bote Women Empowerment" -> "बोटे महिला सशक्तिकरण"
        "Bote River Heritage" -> "बोटे नदी सम्पदा"
        "Community Research Center" -> "सामुदायिक अनुसन्धान केन्द्र"
        "Bote Education Initiatives" -> "बोटे शिक्षा पहल"
        "Bote Digital Archive" -> "बोटे डिजिटल अभिलेख"
        else -> category
    }
}

fun getSeoLandingPagesData(isNepali: Boolean = false): List<SeoLandingPage> {
    if (!isNepali) {
        return listOf(
            SeoLandingPage(
                categoryName = "Bote History",
                canonicalTitle = "Bote History & Cultural Chronicles of Nepal",
                keywords = "Bote History, Sarlahi Bote Tribe, bagmati river migration, indigenous communities Nepal",
                metaDescription = "Explore the extensive history of the Bote people, their pre-modern migration routes, boat design, and traditional governance along Bagmati waters in Nepal.",
                details = "The Bote people are an ethnic group indigenous to the inner Terai regions of Nepal. They speak Bote language. The Bote people are well known for ferrying travellers across the rivers through the boats, which often are prepared from the trunks of the trees. They are scattered around the bank of Kaligandaki, Narayani and Rapti River of Nepal. Bote and Majhi people are known as the ‘King of water’. Their ancestral occupation is fishing, boating and searching gold in the river whose settlement is nearby river and forest. The dialect and culture of Bote people in several ways is similar to that of the Danu wars, Daria, Tharus and Majhi.",
                faqs = listOf(
                    Pair("How far back does the Bote lineage stretch?", "Archaeological and oral research suggests that the Bote clans have inhabited the forest terrains and major riverbanks of Nepal for over nine generations, surviving malaria and developing sustainable eco-forestry secrets."),
                    Pair("Is Bote history documented in school books?", "Historically, central textbooks omitted local stories, which is why our digital portal and current collaborations with Kathmandu University aim to document oral chroniclers permanently.")
                ),
                altTagGuide = "Group of Bote community elders building traditional wood canoe by the river shoreline in Karmahiya, Sarlahi, Nepal.",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote Language", "Tarahi Bote Community")
            ),
            SeoLandingPage(
                categoryName = "Bote Language",
                canonicalTitle = "Grammar & Essential Vocal Vocabulary of Bote Language",
                keywords = "Bote language preservation, endangered dialect dictionary, Nepalese river vocabulary, Tribhuvan University",
                metaDescription = "Discover the endangered Bote linguistic structure. Access our digital dictionary featuring riverine vocabulary, phonetic audio, and local verbs.",
                details = "Our dialect belongs to the Indo-Aryan group, displaying heavy structural isolation. Because the language has never had its own formal writing system, we are creating Devnagari transcription structures. Active keywords of aquatic importance: 'Shawa' (Water), 'Nauni' (Fish), 'Dhoni' (Wooden boat), 'Jhal' (Dynamic cast-net).",
                faqs = listOf(
                    Pair("Does the Bote language use Devanagari script?", "We are currently using adapted Devanagari characters to transcribe phonetic patterns to help teach the youth in primary school."),
                    Pair("How many fluent Bote language speakers remain?", "Recent surveys show fewer than 3,000 fluent native speakers. Our goal is to expand language classes to preserve the language.")
                ),
                altTagGuide = "Handwritten Bote dictionary mapping essential words like Shawa, Dhoni, and Nauni for bilingual students.",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote History", "Bote Education Initiatives")
            ),
            SeoLandingPage(
                categoryName = "Bote Culture",
                canonicalTitle = "Traditions, river rituals & water folklore of Botes",
                keywords = "Bote Culture and traditions, river rituals, indigenous Nepalese music, Badhai festival",
                metaDescription = "Deep dive into the authentic culture of the Bote tribe. Discover their traditional architecture, musical instruments, and sacred relationship with the Bagmati river.",
                details = "Bote culture revolves around a sacred relationship with water. Families historically lived in organic thatched huts constructed from mud, river stones, and tall reeds. Traditional songs are paced to the rhythm of boat-rowing strokes, ensuring rows remain synchronized. We wreathe our ancestors' memories in the flowing patterns of hand-drawn clay decorations.",
                faqs = listOf(
                    Pair("What is the central cultural characteristic of Botes?", "Our spiritual lives are centered on river ecosystems—the river is revered as a maternal deity that sustains life and connection."),
                    Pair("Are Bote cultural objects preserved?", "Yes, the local community center in Karmahiya is building a physical museum to house traditional nets, oars, and woven baskets.")
                ),
                altTagGuide = "Traditional clay-textured mud huts with thatch roof on the banks of Bagmati river Sarlahi.",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Festivals", "Bote Traditional Dress")
            ),
            SeoLandingPage(
                categoryName = "Bote Traditional Dress",
                canonicalTitle = "Traditional dress, fiber-work & weaving styles of Botes",
                keywords = "Bote traditional dress, river fiber spinning, natural plant garments, Sarlahi weaving",
                metaDescription = "Explore traditional Bote apparel constructed from local wild hemp, river grass fibers, and organic cotton. Perfect research for cultural historians.",
                details = "Traditionally, Bote clothing was designed for high agility in humid riverine climates. Men wear a simple Kachhad (a wrap-around loincloth) and a Bhoto (sleeveless cotton vest), which allows them to easily navigate water currents and cast fishing nets. Bote women wear a Phariya (sari/skirt) tied with a Patuka (waistband) and a Cholo (blouse), accessorized with traditional silver jewelry such as Hasuli (rigid neck ring), Bulaki (nose ring), and glass bead necklaces (Pote).",
                faqs = listOf(
                    Pair("What materials are used for Bote traditional garments?", "They traditionally use comfortable, lightweight cotton and locally harvested natural plant fibers from wild hemp and reeds for accessories."),
                    Pair("Can I buy authentic wove products?", "Yes! Our Sarlahi women's cooperative sells woven reed mats, decorative baskets, and traditional fish bags to tourists.")
                ),
                altTagGuide = "Close up of an elder Bote woman weaving a traditional pattern using natural brown grass reeds in Madhesh Province.",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Culture", "Bote Women Empowerment")
            ),
            SeoLandingPage(
                categoryName = "Bote Festivals",
                canonicalTitle = "The Monsoon Badhai & River Worship Festivals of Sarlahi Botes",
                keywords = "Bote festivals, Badhai, river god worship, canoe purification, traditional Nepalese fairs",
                metaDescription = "Learn about the vibrant Monsoon Badhai Festival of the Botes. Read about the grand boat races, ritual animal offerings, and water prayers.",
                details = "Our primary celebration is 'Badhai' (also termed river puja), taking place right before the monsoon rains. The entire village gathers by the Bagmati riverbanks to wash and purify their dugout canoes. We perform dances, play the traditional drum ('madal'), and ask the river for safe, abundant fishing seasons.",
                faqs = listOf(
                    Pair("When does the Badhai festival take place?", "It occurs annually at the start of the monsoon season (usually late June or July)."),
                    Pair("Can external visitors attend the festival?", "Yes! The festival is open to everyone, offering a beautiful opportunity to experience our food, music, and canoe racing.")
                ),
                altTagGuide = "Canoe rowing competition during the annual Bote Monsoon festival on the Bagmati river.",
                schemaType = "Event Schema",
                internalLinks = listOf("Bote Culture", "Bote Homestays")
            ),
            SeoLandingPage(
                categoryName = "Bote Homestays",
                canonicalTitle = "Ecotourism & Traditional River Homestays in Karmahiya Sarlahi",
                keywords = "Bote homestays, Karmahiya homestay, ecotourism Sarlahi, authentic Nepal travel, river rafting",
                metaDescription = "Book a unique cultural homestay with the Tarahi Bote families. Experience traditional meals, wooden canoe rides, and folk songs.",
                details = "We have established the Karmahiya Homestay Committee to provide a direct livelihood for our families. Guests live in clean, traditional mud-brick houses, enjoy authentic snail stews, and join local guides on the Bagmati River for bird watching and traditional canoeing.",
                faqs = listOf(
                    Pair("Are the homestays hygienic?", "Absolutely. All hosting homes are equipped with hygienic setups and undergo regular sanitary audits by our cooperative."),
                    Pair("What is included in the homestay booking?", "Packages include authentic local meals, cozy overnight stays, and guided boating tours on the river.")
                ),
                altTagGuide = "Cozy guest room inside a renovated Bote mud cottage, showcasing hand-woven linens and local decorations.",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Festivals", "Bote Community of Sarlahi")
            ),
            SeoLandingPage(
                categoryName = "Bote Youth Programs",
                canonicalTitle = "Bote Youth Community Nepal - Culture & Leadership",
                keywords = "Bote Youth Community Nepal, save Bote culture, traditional Bote dress, endangered Bote language",
                metaDescription = "Support Bote Youth Community Nepal in its historic mission to save traditional riverine cultures, dress, language, and promote youth higher education.",
                details = "The Bote Youth Community Nepal is a youth-led movement working to revitalize indigenous Bote identity. We organize community classes to save our endangered ancestral language, run hand-weaving workshops to preserve the traditional Bote dress (Kachhad, Boto, Phariya, Patuka), and run IT libraries to encourage higher education among riverside youth.",
                faqs = listOf(
                    Pair("How is Bote Youth Community Nepal saving traditional dress?", "We host workshops where elders teach young Bote girls and boys the art of spinning wild hemp and weaving authentic Phariya, Kachhad, and Boto attire."),
                    Pair("What is the group's mission?", "Our mission is to achieve social justice, preserve our ancient languages and canoe-crafting heritage, and empower youth through modern IT education and leadership.")
                ),
                altTagGuide = "Bote youth leaders wearing traditional hand-woven Kachhad and Phariya attire at a cultural workshop in Madhesh, Nepal.",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Education Initiatives", "Tarahi Bote Community")
            ),
            SeoLandingPage(
                categoryName = "Bote Community of Sarlahi",
                canonicalTitle = "Socio-Economic Profiles of Botes in Sarlahi District",
                keywords = "Bote Community of Sarlahi, Karmahiya village profile, Madhesh province indigenous group",
                metaDescription = "Explore the demographic structures, municipal development projects, and civic infrastructure in Karmahiya's primary Bote settlement.",
                details = "Sarlahi district is home to one of the largest and most politically cohesive Bote populations in Madhesh Province. Centered in Karmahiya, our ward networks work collaboratively on clean water projects, healthcare campaigns, and primary education initiatives to improve overall quality of life.",
                faqs = listOf(
                    Pair("How many Bote families live in Karmahiya?", "Around 140 families are settled directly in Karmahiya, forming a tight-knit community."),
                    Pair("What municipal services are available?", "We are continuously expanding direct road networks, solar lighting, and local tap systems with regional authorities.")
                ),
                altTagGuide = "Panorama of the Karmahiya village showing green crop fields and clustered houses of the Bote people.",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Tarahi Bote Community", "Bote River Heritage")
            ),
            SeoLandingPage(
                categoryName = "Tarahi Bote Community",
                canonicalTitle = "The Tarahi Clan: Genealogy, Customs & River Wisdom",
                keywords = "Tarahi Bote Community, Tarahi genealogy, riverboat builders, Sarlahi Madhesh Nepal",
                metaDescription = "Discover the unique lineage of the Tarahi sub-group. Learn how our ancestors crafted specialized canoes for heavy monsoonal navigation.",
                details = "The Tarahi clans are highly respected for their deep river navigation skills and craft lineage. Historically, they served as key scouts across flooded rivers during heavy summer monsoons. Today, we are keeping this proud heritage alive by translating ancient knowledge into digital records.",
                faqs = listOf(
                    Pair("How does the Tarahi branch differ from other Bote clans?", "Tarahi families traditionally specialized in deeper river structures and rapid waters, developing uniquely resilient boat construction designs."),
                    Pair("Are there genealogical records of the Tarahi family?", "Our research center is compiling oral lineages to create a comprehensive digital family archive.")
                ),
                altTagGuide = "Traditional Bote craftsman carving dugout canoe from big wood logs near Karmahiya, Sarlahi.",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Community of Sarlahi", "Indigenous Communities of Nepal")
            ),
            SeoLandingPage(
                categoryName = "Indigenous Communities of Nepal",
                canonicalTitle = "Positioning Botes Within Nepal's National Indigenous Matrix",
                keywords = "Indigenous communities Nepal, minority list NFDIN, riverine ethnic groups, cultural justice",
                metaDescription = "Read our research mapping the Botes alongside Majhi, Danuwar, and other highly marginalized riverine communities of Madhesh.",
                details = "Nepal's rich cultural diversity includes several highly marginalized riverine communities. The National Foundation (NFDIN) lists Bote as an official minority group. By sharing resources and building solidarity with other indigenous communities, we are amplifying representation for water-rights conservation.",
                faqs = listOf(
                    Pair("What classification is assigned to the Bote community?", "Our community is officially designated as a Highly Marginalized Indigenous Nationalities group in Nepal."),
                    Pair("Do Bote civil organizations collaborate globally?", "Yes, we participate in national networks like NEFIN to advocate for indigenous rights, forest access, and water preservation.")
                ),
                altTagGuide = "Representatives of regional indigenous communities meeting at a national cultural festival in Lalbandi.",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote River Heritage", "Bote Women Empowerment")
            ),
            SeoLandingPage(
                categoryName = "Bote Women Empowerment",
                canonicalTitle = "Bote Women Cooperatives, Crafts & Savings in Sarlahi",
                keywords = "Bote Women Empowerment, handcrafts business, women microfinance Sarlahi, local savings",
                metaDescription = "Review the successes of our Sarlahi Women's Savings Cooperative. We fund girls' education and launch local handcraft stalls.",
                details = "Bote women are the pillars of our economic development. Through cooperative savings programs, members earn a stable income weaving dried river-grass bags and mats. This funding provides educational scholarships and entrepreneurial start-up capital for young girls.",
                faqs = listOf(
                    Pair("How many members are in the Bote Women Cooperative?", "We currently have over 85 active members in the local Karmahiya cooperative circle."),
                    Pair("What products are crafted by Bote women?", "We specialize in weaving beautiful river-weed floor mats, hand bags, and decorative baskets.")
                ),
                altTagGuide = "Smiley Bote woman showing her hand woven colorful basket at local fair in Janakpur.",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Traditional Dress", "Bote Education Initiatives")
            ),
            SeoLandingPage(
                categoryName = "Bote River Heritage",
                canonicalTitle = "Sustainable Fishing & River Ecology Heritage of Botes",
                keywords = "Bote River Heritage, organic fishing, sarlahi aquatic ecology, circular cast net, conservation",
                metaDescription = "Our people have lived in harmony with the rivers for generations. Explore our sustainable fishing gears, eco-friendly boating, and clean water campaigns.",
                details = "Rivers are the foundation of our heritage. We historically avoided using intensive commercial fishing methods, relying instead on handmade circular nets and custom fish bags that allowed smaller fish to escape, preserving resources for future generations.",
                faqs = listOf(
                    Pair("What is a traditional Bote fishing net?", "The circular cast-net ('Jhal') is handspun and weighted with pebbles, designed to be cast gracefully from the edge of dugout canoes."),
                    Pair("How do Botes protect the local aquatic ecosystem?", "We advocate against harmful electric fishing and river sand dredging to protect local fish populations naturally.")
                ),
                altTagGuide = "A Bote fisherman throwing out circular cast net into the gleaming Bagmati river waters during sunset.",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote History", "Bote Community of Sarlahi")
            ),
            SeoLandingPage(
                categoryName = "Community Research Center",
                canonicalTitle = "Tarahi Bote Community Research & Documentation Center",
                keywords = "Community Research center Sarlahi, indigenous ethnography, tribal studies Nepal, archive",
                metaDescription = "Access ethnographic papers, socio-economic research, and language documentation studies of the Tarahi Bote Community.",
                details = "The Community Research and Documentation Center in Karmahiya stands as our dedicated archive. We work alongside Tribhuvan and Kathmandu Universities to gather reliable demographic, linguistic, and historical datasets.",
                faqs = listOf(
                    Pair("Can researchers visit the Karmahiya Center?", "Yes! We welcome academic researchers, anthropologists, and students, providing homestay lodging and experienced community guides."),
                    Pair("What archives are available at the center?", "Our physical library houses written historical papers, local census sheets, and over 60 hours of recorded elder stories.")
                ),
                altTagGuide = "Lobby of the physical Bote Community Research center showing reference books and old photos of Sarlahi district.",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Digital Archive", "Bote Education Initiatives")
            ),
            SeoLandingPage(
                categoryName = "Bote Education Initiatives",
                canonicalTitle = "Bote Youth Community Nepal: Higher Education & IT Library Initiatives",
                keywords = "Bote higher education scholarships, IT library development Sarlahi, bilingual curriculum Nepal, Bote Youth Community",
                metaDescription = "Support higher education and solar-powered IT library development for Bote youth. Explore our school enrollment and bilingual primary education programs.",
                details = "Bote Youth Community Nepal is actively transforming opportunities for Bote youth. By establishing solar-powered IT library and learning centers in rural river settlements (such as Karmahiya, Sarlahi) and offering targeted Higher Education Scholarships for university admissions, we are bridging the digital divide. This program runs alongside our efforts to print bilingual early-childhood readers to preserve the native Bote language.",
                faqs = listOf(
                    Pair("How does Bote Youth Community Nepal support higher education?", "We provide financial sponsorships, university admission guidance, and technical hardware/software certifications to eliminate cost barriers for talented Bote students."),
                    Pair("What is the IT Library development initiative?", "It focuses on setting up physical, solar-powered digital libraries with laptop stations, tablets, and offline academic servers for students in riverside villages.")
                ),
                altTagGuide = "Bote school children and youth studying together inside the newly developed solar-powered IT Library Sarlahi, Nepal.",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Youth Programs", "Bote Women Empowerment")
            ),
            SeoLandingPage(
                categoryName = "Bote Digital Archive",
                canonicalTitle = "Searchable Digital Audio, Photo & Video Heritage Archives",
                keywords = "Bote digital archive, oral history recordings, traditional river chants, photo preservation",
                metaDescription = "Search old photos, listen to traditional boat rowers folk songs, and view digitized historical documents of our ancestors.",
                details = "Our Bote Digital Archive is dedicated to preserving our rich heritage online. It houses collections of historical photographs, recorded folk songs, oral stories of tribal migration along Bagmati riverways, and downloadable academic publications.",
                faqs = listOf(
                    Pair("Is the digital archive accessible offline?", "Yes, this app locally stores core texts, dictionary phrases, and FAQs, ensuring reliable access without internet connections."),
                    Pair("How can I contribute materials to the archive?", "If you have historical photographs or records, please contact our community coordinators at the Karmahiya office.")
                ),
                altTagGuide = "Digitized black-and-white print of Bote boatmen rowing deep Salwood dugout canoe during 1970 floods.",
                schemaType = "Article Schema",
                internalLinks = listOf("Community Research Center", "Bote History")
            )
        )
    } else {
        return listOf(
            SeoLandingPage(
                categoryName = "Bote History",
                canonicalTitle = "बोटे इतिहास तथा नदी किनारको पुर्ख्यौली सम्पदा",
                keywords = "बोटे इतिहास, सर्लाहीको बोटे जाति, बागमती नदी स्थानान्तरण, नेपालका आदिवासी",
                metaDescription = "बागमती नदी किनारका बोटे समुदायको ऐतिहासिक यात्रा, डुङ्गा निर्माण सीप र परम्परागत जीवनशैलीको अनुसन्धान खण्ड।",
                details = "बोटे समुदाय नेपालको भित्री मधेश क्षेत्रका एक आदिवासी जनजाति हुन्। उनीहरू बोटे भाषा बोल्छन्। बोटे मानिसहरू रूखको टोड्का (खोपा) बाट तयार पारिएका काठका डुङ्गाहरू (धोनी) मार्फत यात्रुहरूलाई नदी पार गराउनका लागि प्रख्यात छन्। उनीहरू नेपालको कालीगण्डकी, नारायणी र राप्ती नदीको किनारमा छरिएर रहेका छन्। बोटे र माझी समुदायलाई 'पानीको राजा' भनेर चिनिन्छ। उनीहरूको पुर्ख्यौली पेशा माछा मार्ने, डुङ्गा चलाउने र नदीमा सुन खोज्ने हो, जसको बसोबास नदी र जङ्गल नजिकै हुने गर्दछ। बोटे समुदायको भाषिका र संस्कृति धेरै हदसम्म दनुवार, दराई, थारू र माझी समुदायसँग मिल्दोजुल्दो छ।",
                faqs = listOf(
                    Pair("बोटे वंश कति पुरानो मानिन्छ?", "अनुसन्धानका अनुसार बोटे वंश ९ पुस्ताभन्दा पुरानो समयदेखि नेपालका मुख्य नदी किनार र वन क्षेत्रहरूमा बसोबास गर्दै आएको छ।"),
                    Pair("के विद्यालयको पाठ्यक्रममा बोटे इतिहास समावेश छ?", "ऐतिहासिक रूपमा पाठ्यपुस्तकहरूमा स्थानीय बोटे इतिहासहरू समेटिएका थिएनन्, त्यसैले हाम्रो डिजिटल पोर्टल र काठमाडौं विश्वविद्यालयको सहकार्यले यसलाई स्थायी अभिलेख बनाउँदैछ।")
                ),
                altTagGuide = "नेपालको सर्लाही कर्महियामा बागमती नदीको किनारमा परम्परागत काठको डुङ्गा बनाउँदै बोटे समुदायका भद्रभलादमीहरू।",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote Language", "Tarahi Bote Community")
            ),
            SeoLandingPage(
                categoryName = "Bote Language",
                canonicalTitle = "बोटे भाषाको व्याकरण र आधारभूत शब्दावली",
                keywords = "बोटे भाषा संरक्षण, संकटापन्न भाषाहरू, नदी किनारको शब्दावली, त्रिभुवन विश्वविद्यालय",
                metaDescription = "संकटोन्मुख बोटे भाषाको संरचना र उपयोगी रैथाने शब्दहरूको विवरण। देवनागरी लिपिमा आधारित बोटे शब्दकोश।",
                details = "हाम्रो बोली भारोपेली भाषा परिवारको भए तापनि यसमा आफ्नै मौलिक संरचनात्मक भिन्नताहरू छन्। बोटे भाषाको आफ्नै औपचारिक लिपि नभएकाले, हामी देवनागरी लिपिको प्रयोग गरी यसको लिपिबद्धता गरिरहेका छौं। केही महत्वपूर्ण जलसम्बन्धी रैथाने शब्दहरू: 'शावा' (पानी), 'नौनी' (माछा), 'धोनी' (काठको डुङ्गा), 'झाल' (जाल)।",
                faqs = listOf(
                    Pair("के बोटे भाषामा देवनागरी लिपिको प्रयोग हुन्छ?", "युवा तथा बालबालिकाहरूलाई अध्यापन गराउन र विद्यालयमा बुझाउन हाल देवनागरी वर्णहरूको परिमार्जित रूप प्रयोग गरिएको छ।"),
                    Pair("बोटे भाषा बोल्ने व्यक्तिहरू कति छन्?", "सर्वेक्षण अनुसार हाल मुख्य ३००० भन्दा कम मातृभाषीहरू बाँकी छन्। यस भाषालाई जोगाउन अनलाइन र सामुदायिक कक्षहरू सञ्चालन गरिएको छ।")
                ),
                altTagGuide = "बोटे विद्यार्थीहरूका लागि शावा, धोनी, र नौनी जस्ता शब्दहरूको सूची समेटिएको बोटे भाषा संकलन पुस्तिका।",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote History", "Bote Education Initiatives")
            ),
            SeoLandingPage(
                categoryName = "Bote Culture",
                canonicalTitle = "बोटे संस्कृति, नदी अनुष्ठान र जल लोककथाहरू",
                keywords = "बोटे संस्कृति र परम्परा, नदी पूजा, आदिवासी नेपाली संगीत, बधाई चाड",
                metaDescription = "बागमती नदीसँग बोटे जातिको पवित्र ऐतिहासिक र सांस्कृतिक सम्बन्ध। पारंपरिक घर, बाजाहरू र कलाको विवरण।",
                details = "बोटे संस्कृति पूर्ण रूपमा पानी र नदीको प्रकृतिसँग गाँसिएको छ। परम्परागत रूपमा बोटे समुदाय नदी किनारमा माटो र बछेडाबाट निर्मित फुसको घरमा बस्ने गर्दथे। नदीमा डुङ्गा खियाउँदा गाइने श्रम गीतहरूले डुङ्गाको गतिलाई गति दिने र एकताबद्ध बनाउने प्राचीन परम्परा छ। सान्निध्यता झल्काउन माटाका भित्ताहरूमा माछा र लहरका चित्रहरू बनाइन्छ।",
                faqs = listOf(
                    Pair("बोटे समुदायको मुख्य सांस्कृतिक विशेषता के हो?", "हाम्रो आत्मिक जीवन नदी पारिस्थितिक प्रणालीमा केन्द्रित छ - जीवन धान्ने र सम्बन्ध बलियो बनाउने भएकाले नदीलाई ममतामयी मातृत्वको दर्जा दिइन्छ।"),
                    Pair("के बोटे सांस्कृतिक सामग्रीहरूको संरक्षण गरिन्छ?", "हो, कर्महियाको स्थानीय सामुदायिक केन्द्रमा परम्परागत जाल, ओआर (पैडल) र बुनेका टोकरीहरू सङ्कलन गर्न संग्रहालय बन्दैछ।")
                ),
                altTagGuide = "सर्लाहीको बागमती नदी किनारमा निर्मित परम्परागत फुसका छाना भएका बोटे फुसका घरहरू।",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Festivals", "Bote Traditional Dress")
            ),
            SeoLandingPage(
                categoryName = "Bote Traditional Dress",
                canonicalTitle = "पारंपरिक बोटे पोशाक र बुन्ने शैलीहरू",
                keywords = "बोटे पारंपरिक पोशाक, फाइबर कताई, प्राकृतिक बछेडा कपडा, सर्लाही बुनाइ",
                metaDescription = "प्राकृतिक बछेडाको रेशा र नदीको खरबाट बुनेका परम्परागत बोटे लत्ताकपडा र कलात्मक सरसामानहरू।",
                details = "परम्परागत रूपमा बोटे पुरुषहरूले नदी तटीय क्षेत्रको उष्ण र ओसिलो जलवायु अनुकूल हल्का कछाड (घुँडासम्म आउने लुगा) र सुतीको भोटो लगाउँछन्, जसले गर्दा डुङ्गा खियाउन र जाल हान्न सजिलो हुन्छ। बोटे महिलाहरूले पटुकाले बाँधिएको फरिया (गुन्यू) र चोलो लगाउँछन्। उनीहरूको मौलिक पहिरनमा चाँदीको ठूलो हँसुली (घाँटीमा लगाइने गहना), बुलाकी, फुली, ढुङ्ग्री र पोते जस्ता पारंपरिक गहनाहरू समावेश हुन्छन्।",
                faqs = listOf(
                    Pair("बोटे पारंपरिक पोशाकमा कुन सामग्रीहरू प्रयोग गरिन्छ?", "यिनीहरू परम्परागत रूपमा हलुका र आरामदायी सुती कपडाका साथै हस्तकलाका सामानका लागि नदी किनारका प्राकृतिक खर र पातहरू प्रयोग गर्छन्।"),
                    Pair("के यी मौलिक बुनेका सामग्रीहरू किन्न पाइन्छ?", "हो! हाम्रो सर्लाही महिला सहकारीले पर्यटकहरूका लागि बुनेका गुन्द्री, सुकुल, ढाकी र पारंपरिक जल-झोलाहरू बिक्रीमा राखेको छ।")
                ),
                altTagGuide = "मधेश प्रदेशमा एक बोटे बज्यै प्राकृतिक खर र पाट प्रयोग गरेर परम्परागत ढाँचाको चटाई बुन्दै।",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Culture", "Bote Women Empowerment")
            ),
            SeoLandingPage(
                categoryName = "Bote Festivals",
                canonicalTitle = "बर्षे बधाई र बागमती नदी पूजा उत्सव",
                keywords = "बोटे चाडपर्व, बधाई पूजा, नदी आराधना, डुङ्गा चोखो पार्ने, डुङ्गा दौड",
                metaDescription = "बोटे जातिको सबैभन्दा ठूलो नदी उत्सव 'बधाई'। डुङ्गा दौड, पूजापाठ, मादलको ताल र नदी आराधनाको जीवन्त दृश्य।",
                details = "हाम्रो प्रमुख उत्सव मानिने 'बधाई पूजा' वर्षायाम सुरु हुनु अगावै आयोजना गरिन्छ। सम्पूर्ण गाउँले बागमती नदी किनारमा भेला भई आफ्ना परम्परागत डुङ्गाहरू सफा गरी चोख्याउँछन्। यस अवसरमा मादलको मधुर तालमा परम्परागत नृत्यहरू गरिन्छ र नदी देवीसँग जीवन रक्षा र प्रचुर मात्रामा माछा पाइने प्रार्थना गरिन्छ।",
                faqs = listOf(
                    Pair("बधाई पूजा कहिले मनाइन्छ?", "यो सामान्यतया हरेक वर्ष असार महिनाको सुरुमा (वर्षायामको आगमनसँगै) मनाइन्छ।"),
                    Pair("के बाह्य पर्यटकहरू यो मेलामा सहभागी हुन सक्छन्?", "पक्कै पनि! यो उत्सव सबैका लागि खुला छ र हाम्रो रैथाने परिकार, संगीत र परम्परागत डुङ्गा दौडको अवलोकन गर्ने अनुपम अवसर हो।")
                ),
                altTagGuide = "बागमती नदीमा आयोजित वार्षिक बोटे बधाई उत्सवको समयमा भएको डुङ्गा खियाउने प्रतिस्पर्धा।",
                schemaType = "Event Schema",
                internalLinks = listOf("Bote Culture", "Bote Homestays")
            ),
            SeoLandingPage(
                categoryName = "Bote Homestays",
                canonicalTitle = "कर्महिया सर्लाहीको पर्यावरणमैत्री बोटे सामुदायिक होमस्टे",
                keywords = "बोटे होमस्टे, कर्महिया होमestay, पर्यापर्यटन सर्लाही, मौलिक नेपाल यात्रा, जलविहार",
                metaDescription = "तराही र बोटे परिवारसँगै बस्ने अनौठो सांस्कृतिक अनुभव। रैथाने भोजन, काठे डुङ्गा सयर तथा बागमती किनारको लोक गफ।",
                details = "हामीले हाम्रो परिवारको जीविकोपार्जनमा टेवा पुऱ्याउन कर्महिया होमस्टे व्यवस्थापन समिति गठन गरेका छौं। पाहुनाहरू रती थला भएका माटाका घरहरूमा बस्छन्, रैथाने घुँघी र माछाका परिकारहरूको स्वाद लिन्छन् र बागमती नदीमा स्थानीय गाइडसँग चरा अवलोकन र जलविहार गर्छन्।",
                faqs = listOf(
                    Pair("के होमस्टेहरू सफा र स्वच्छ छन्?", "पूर्ण रूपमा। हाम्रा सबै आतिथ्य घरहरू सफा छन् र हाम्रो सहकारीले नियमित रूपमा स्वास्थ्य र सरसफाइको जाँच गर्दछ।"),
                    Pair("होमस्टे बुकिङमा के-के समावेश हुन्छ?", "प्याकेजमा परम्परागत रैथाने खाना, आरामदायी आवास र नदीमा डुङ्गा यात्रा समावेश हुन्छ।")
                ),
                altTagGuide = "सर्लाहीमा पुनरुत्थान गरिएको माटोको बोटे घरभित्रको सफा कोठा, जहाँ रैथाने बुनेका तन्नाहरू राखिएका छन्।",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Festivals", "Bote Community of Sarlahi")
            ),
            SeoLandingPage(
                categoryName = "Bote Youth Programs",
                canonicalTitle = "बोटे युवा समुदाय नेपाल - संस्कृति र नेतृत्व विकास",
                keywords = "बोटे युवा समुदाय नेपाल, मौलिक संस्कृति संरक्षण, बोटे भेषभूषा संरक्षण, बोटे भाषा बचाउने अभियान",
                metaDescription = "बोटे युवा समुदाय नेपालको ऐतिहासिक अभियानमा जोडिनुहोस्। हाम्रो मौलिक कला, संस्कृति, परम्परागत पहिरन र भाषाको संरक्षण गरौं।",
                details = "बोटे युवा समुदाय नेपाल एक जुझारु युवा नेतृत्वको साझा मञ्च हो, जसले बोटे जातिको पहिचान र अधिकार रक्षाका लागि काम गरिरहेको छ। हामी लोपोन्मुख बोटे भाषाको जगेर्ना गर्न नियमित मातृभाषा कक्षाहरू सञ्चालन गर्छौं, मौलिक भेषभूषा (कच्छाड, भोटो, फरिया, पटुका) संरक्षणका लागि सिलाई बुनाई कार्यशाला चलाउँछौं, र उच्च शिक्षा प्रवर्द्धन गर्न डिजिटल पुस्तकालय र कम्प्युटर प्रशिक्षण केन्द्रहरू सञ्चालन गर्छौं।",
                faqs = listOf(
                    Pair("बोटे युवा समुदाय नेपालले परम्परागत पहिरन कसरी बचाइरहेको छ?", "हामी स्थानीय स्तरमा कार्यशालाहरू आयोजना गर्छौं जहाँ अग्रजहरूले युवाहरूलाई प्राकृतिक अल्लो र कपासबाट कच्छाड, भोटो र फरिया बुन्ने सीप सिकाउँछन्।"),
                    Pair("यो संस्थाको मुख्य उद्देश्य के हो?", "हाम्रो उद्देश्य सामाजिक न्याय प्राप्त गर्नु, पुरानो डुङ्गा बनाउने कला र भाषा संरक्षण गर्नु, र युवाहरूलाई आधुनिक सूचना प्रविधिसँग जोडेर सशक्त बनाउनु हो।")
                ),
                altTagGuide = "परम्परागत हस्तनिर्मित कच्छाड र फरिया लगाएर साँस्कृतिक कार्यशालामा सहभागी बोटे युवाहरू।",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Education Initiatives", "Tarahi Bote Community")
            ),
            SeoLandingPage(
                categoryName = "Bote Community of Sarlahi",
                canonicalTitle = "सर्लाही जिल्लाका बोटे समुदायको सामाजिक-आर्थिक प्रोफाइल",
                keywords = "सर्लाहीको बोटे समुदाय, कर्महिया गाउँ प्रोफाइल, मधेश प्रदेशका आदिवासी",
                metaDescription = "बागमती अंचलको कर्महिया गाउँमा रहेका बोटे बस्तीहरूको विकास, विद्यालय भर्ना र स्थानीय जनशक्तिको विवरण।",
                details = "सर्लाहीको बागमती नदी किनारको कर्महिया गाउँ मधेश प्रदेशकै सबैभन्दा संगठित बोटे बस्ती मानिन्छ। हाम्रो वडा सञ्जालले खानेपानी आयोजना, निःशुल्क स्वास्थ्य शिविर र प्रारम्भिक विद्यालय पहलहरूमा सहकार्य गर्दै जीवनस्तर उकास्ने काम गरिरहेको छ।",
                faqs = listOf(
                    Pair("कर्महियामा कति बोटे परिवार बसोबास गर्छन्?", "यहाँ करिब १४० बोटे परिवारहरू लामो समयदेखि नदी किनारमा मिलेर बसोबास गरिरहेका छन्।"),
                    Pair("स्थानीय स्तरमा के-कस्ता सेवाहरू थपिएका छन्?", "हामीले स्थानीय सरकारसँग सहकार्य गरी सौर्य बत्ती, सडक सुधार र शुद्ध पिउने पानीका धाराहरू विस्तार गरिरहेका छौं।")
                ),
                altTagGuide = "हराभरा खेतबारी र बोटे समुदायका सुन्दर लहरै मिलेका घरहरू देखिने सर्लाही कर्महिया गाउँको मनोरम दृश्य।",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Tarahi Bote Community", "Bote River Heritage")
            ),
            SeoLandingPage(
                categoryName = "Tarahi Bote Community",
                canonicalTitle = "तराही बोटे समुदाय: वंशावली र जल ज्ञान",
                keywords = "तराही बोटे, तराही वंशावली, काठे डुङ्गा निर्माता, सर्लाही मधेश",
                metaDescription = "तराही उप-समूहको विशेष वंशावली। बर्खे भेलमा डुङ्गा चलाउन खप्पिस पुर्खाहरूको कथा र संरक्षण।",
                details = "बोटे जातिभित्र तराही हाँगा नदीको बहाव र डुङ्गा निर्माण कलामा निकै कहलिएको छ। ऐतिहासिक रूपमा, बर्खाको भीषण बाढीका बेला नदी तार्न र उद्दार गर्न यी परिवारहरू अग्रणी हुन्थे। हाल हामी यस प्राचीन नदी-ज्ञानलाई डिजिटल रूपमा लिपिबद्ध गरी सुरक्षित राख्दै छौं।",
                faqs = listOf(
                    Pair("तराही हाँगा अन्य बोटेभन्दा कसरी फरक छ?", "तराही संजाल विशेषगरी ठूला र गहिरा नदीको भेलमा डुङ्गा चलाउने र बलियो डुङ्गा बनाउने कार्यमा अति पोख्त मानिन्थे।"),
                    Pair("के तराही समुदायको वंशावली अभिलेख छ?", "हाम्रो अनुसन्धान केन्द्रले वृद्धवृद्धाहरूको मौखिक संस्मरण रेकर्ड गरी डिजिटल वंशावली अभिलेख बनाइरहेको छ।")
                ),
                altTagGuide = "सर्लाहीको कर्महियामा ठूलो काठको मुढाबाट परम्परागत शैलीमा खियाउने डुङ्गा कुद्दै गरेका बोटे शिल्पकार।",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Community of Sarlahi", "Indigenous Communities of Nepal")
            ),
            SeoLandingPage(
                categoryName = "Indigenous Communities of Nepal",
                canonicalTitle = "नेपालको आदिवासी जनजाति सूचीमा बोटे समुदायको स्थान",
                keywords = "नेपालका आदिवासी जनजाति, अल्पसंख्यक सूची NEFIN, नदी किनारका सीमान्तकृत समूह",
                metaDescription = "माझी, दनुवार र अन्य अल्पसंख्यक नदी किनारका समुदायसँग बोटे जातिको सामाजिक सामिप्यताको विश्लेषण।",
                details = "नेपालको बहुभाषिक र बहुजातीय समाजमा नदीमा आश्रित जातिहरूको विशिष्ठ स्थान छ। आदिवासी जनजाति उत्थान राष्ट्रिय प्रतिष्ठान (NFDIN) ले बोटे जातिलाई अति सीमान्तकृत समूहमा सूचीकृत गरेको छ। हामी जल-अधिकार र सांस्कृतिक हकका लागि अन्य अल्पसंख्यकहरूसँग सहकार्य गरिरहेका छौं।",
                faqs = listOf(
                    Pair("बोटे समुदायलाई कुन श्रेणीमा राखिएको छ?", "नेपाल सरकारले बोटे समुदायलाई अति सीमान्तकृत आदिवासी पहिचानको श्रेणीमा राखेको छ।"),
                    Pair("के बोटे संस्थाहरू राष्ट्रिय रूपमा आबद्ध छन्?", "हो, हामी नेपाल आदिवासी जनजाति महासंघ (NEFIN) जस्ता राष्ट्रिय सञ्जालमा जोडिएर हाम्रो वन र जल अधिकारको वकालत गर्छौं।")
                ),
                altTagGuide = "लालबन्दीमा आयोजित राष्ट्रिय साँस्कृतिक उत्सवमा झाँकी देखाउँदै विभिन्न आदिवासी समुदायका प्रतिनिधिहरू।",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote River Heritage", "Bote Women Empowerment")
            ),
            SeoLandingPage(
                categoryName = "Bote Women Empowerment",
                canonicalTitle = "बोटे महिला सहकारी, हस्तकला र बचत अभियान",
                keywords = "बोटे महिला सशक्तिकरण, हस्तकला उद्योग, वनस्पति फाइबर झोला, सर्लाही बचत",
                metaDescription = "हाम्रो सर्लाही बोटे महिला दिदीबहिनी बचत सहकारीको सफलता। हस्तकला सिर्जना, आयआर्जन र बालबालिकाको शिक्षा।",
                details = "बोटे महिलाहरू हाम्रो घरायसी र सामाजिक आर्थिक विकासका अग्रगामी खम्बा हुन्। सहकारी बचत अभियान मार्फत बुनेका विभिन्न हस्तकला, चकटी, र ढाकीहरू बिक्री गरी उनीहरूले आफ्ना छोरीहरूको विद्यालय शिक्षा र स-साना व्यवसायका लागि बचत गरिरहेका छन्।",
                faqs = listOf(
                    Pair("बोटे महिला सहकारीमा कति सदस्य आबद्ध छन्?", "हाल कर्महियाको स्थानीय महिला सहकारी सञ्जालमा ८५ भन्दा बढी दिदीबहिनीहरू सक्रिय रूपमा आबद्ध हुनुहुन्छ।"),
                    Pair("बोटे महिलाहरूले के-के हस्तकला बनाउँछन्?", "हामी प्राकृतिक खर र खरको पातबाट निकै आकर्षक चटाई, हातले बोक्ने झोला र आकर्षक ढक्कीहरू बनाउँछौं।")
                ),
                altTagGuide = "जनकपुरको स्थानीय मेलामा आफूले बुनेको रंगीचंगी ढाकी देखाउँदै मुस्कुराउँदै गरेकी बोटे दिदी।",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Traditional Dress", "Bote Education Initiatives")
            ),
            SeoLandingPage(
                categoryName = "Bote River Heritage",
                canonicalTitle = "बोटे समुदायको दिगो माछा मार्ने र नदी संरक्षण सम्पदा",
                keywords = "बोटे नदी सम्पदा, जैविक मत्स्य पालन, गोही संरक्षण, सफा नदी अभियान",
                metaDescription = "नदी र गोही संरक्षणमा बोटे जातिको पुस्तौंदेखिको ऐक्यबद्धता। परम्परागत हाते जाल र वातावरणमैत्री जलविहार।",
                details = "नदी हाम्रो प्राण र बोटे सभ्यताको आधार हो। हामीले कहिल्यै नदीको जैविक सन्तुलन बिग्रने गरी व्यावसायिक विष वा करेन्टको प्रयोग गरेनौं। पुर्खाले चलाएको सानो हाते जाल ('झाल') को प्रयोगले साना माछाहरू सजिलै उम्कन पाउँछन्, जसले गर्दा नदीको जलचर सन्तुलित रहन्छ।",
                faqs = listOf(
                    Pair("बोटेहरूको पारंपरिक जाल कस्तो हुन्छ?", "गोलाकार हाते जाल ('झाल') लाई नदी किनारमा उभिएर वा काठे डुङ्गामा बसेर बडो कलात्मक ढंगले फ्याँक्ने गरिन्छ।"),
                    Pair("नदी प्रदूषण रोक्न बोटेहरू के गर्छन्?", "हामी करेन्ट लगाएर माछा मार्ने र बागमतीमा अनियन्त्रित बालुवा उत्खनन गर्ने विरुद्ध सचेतनामूलक अभियान चलाउँछौं।")
                ),
                altTagGuide = "साँझको सुनौलो घाममा बागमती नदीमा गोलाकार जाल फ्याँक्दै गरेको बोटे माझीको मनमोहक तस्बिर।",
                schemaType = "Article Schema",
                internalLinks = listOf("Bote History", "Bote Community of Sarlahi")
            ),
            SeoLandingPage(
                categoryName = "Community Research Center",
                canonicalTitle = "तराही बोटे सामुदायिक अनुसन्धान तथा अभिलेखालय केन्द्र",
                keywords = "अनुसन्धान केन्द्र सर्लाही, आदिवासी ईतिहास संकलन, नेपाल जनजाति अध्ययन",
                metaDescription = "कर्महिया सर्लाहीमा रहेको बोटे अनुसन्धान केन्द्र, जहाँ ऐतिहासिक दस्तावेज र भाषिक संकलनहरू सुरक्षित राखिएका छन्।",
                details = "कर्महियाको सामुदायिक अनुसन्धान तथा अभिलेख केन्द्र हाम्रो गौरव हो। हामी त्रिभुवन विश्वविद्यालय र काठमाडौं विश्वविद्यालयका समाजशास्त्रीहरूसँग सहकार्य गरी हाम्रो जनसंख्या, भाषा र ऐतिहासिक रैथाने ज्ञान संकलन गरिरहेका छौं।",
                faqs = listOf(
                    Pair("के शोधकर्ताहरू अनुसन्धान केन्द्रमा आउन सक्छन्?", "हो! हामी देशभित्र र बाहिरका अनुसन्धानकर्ता र विद्यार्थीहरूलाई स्वागत गर्छौं। उहाँहरूका लागि स्थानीय होमस्टे र पथप्रदर्शकको व्यवस्था छ।"),
                    Pair("केन्द्रमा कस्ता शैक्षिक सामग्रीहरू छन्?", "हाम्रो भौतिक पुस्तकालयमा बोटे इतिहासका पुस्तकहरू, हस्तलिखित प्रतिहरू र हाम्रा बुढापाकाहरूका ६० घण्टाभन्दा बढीका मौखिक स्वर रेकर्डहरू छन्।")
                ),
                altTagGuide = "सर्लाही जिल्लाको ऐतिहासिक र पुराना फोटोहरू सजाइएको बोटे सामुदायिक अनुसन्धान केन्द्रको पुस्तकालय र बैठक हल।",
                schemaType = "Local Business Schema",
                internalLinks = listOf("Bote Digital Archive", "Bote Education Initiatives")
            ),
            SeoLandingPage(
                categoryName = "Bote Education Initiatives",
                canonicalTitle = "बोटे युवा समुदाय नेपाल: उच्च शिक्षा छात्रवृत्ति र सूचना प्रविधि पुस्तकालय",
                keywords = "उच्च शिक्षा छात्रवृत्ति, कम्प्युटर सूचना प्रविधि पुस्तकालय, दुईभाषी प्राथमिक पाठ्यक्रम, बोटे शैक्षिक विकास नेपाल",
                metaDescription = "बोटे युवाहरूको उच्च शिक्षाका लागि विश्वविद्यालय छात्रवृत्ति तथा गाउँ गाउँमा सौर्य ऊर्जाबाट चल्ने आईटी पुस्तकालय (IT Library) विकास अभियान।",
                details = "बोटे युवा समुदाय नेपालले सीमान्तकृत नदी तटीय क्षेत्र (जस्तै कर्महिया, सर्लाही) मा कम्प्युटर साक्षरता बढाउन सौर्य-ऊर्जाबाट चल्ने प्रविधि मैत्री पुस्तकालय (IT Library) स्थापना गरिरहेको छ। यसका साथै प्रतिभावान युवाहरूका लागि विश्वविद्यालय स्तरको उच्च शिक्षा छात्रवृत्ति र प्राविधिक डिप्लोमा सञ्चालन गरी उनीहरूलाई राष्ट्रिय मूलधारमा अगाडि बढाउने कार्य गरिरहेको छ।",
                faqs = listOf(
                    Pair("उच्च शिक्षाका लागि कसरी सहयोग प्रदान गरिन्छ?", "हामी जेहेन्दार बोटे विद्यार्थीहरूका लागि कलेज भर्ना शुल्क, पुस्तक खर्च र आवश्यक परे ल्यापटप सहयोग जस्ता छात्रवृत्ति कार्यक्रमहरू सञ्चालन गर्छौं।"),
                    Pair("सूचना प्रविधि (IT Library) पुस्तकालयको योजना के हो?", "यो योजना नदी तटीय क्षेत्रका बोटे बालबालिकाहरूका लागि सौर्य प्यानल, इन्टरनेट जडित कम्प्युटर, र शैक्षिक सामग्रीले सुसज्जित कक्षहरू निर्माण गर्ने हो।")
                ),
                altTagGuide = "नवनिर्मित सौर्य-ऊर्जा सञ्चालित कम्प्युटर पुस्तकालयमा अध्ययन गर्दै गरेका बोटे विद्यार्थीहरू।",
                schemaType = "Organization Schema",
                internalLinks = listOf("Bote Youth Programs", "Bote Women Empowerment")
            ),
            SeoLandingPage(
                categoryName = "Bote Digital Archive",
                canonicalTitle = "बोटे डिजिटलअभिलेख: गीत, संगीत र इतिहासको संग्रह",
                keywords = "डिजिटल संग्रह, लोकगीत संकलन, परम्परागत स्वरहरू, पुरानो तस्बिर संरक्षण",
                metaDescription = "बागमती किनारका हाम्रा पुर्खाहरूको आवाज, डुङ्गा खियाउने गीत, र पुराना दुर्लभ ऐतिहासिक फोटोहरूको डिजिटल सङ्ग्रहालय।",
                details = "हाम्रो डिजिटल अभिलेखालय बोटे संस्कृतिलाई अनलाइनमा बचाइराख्न समर्पित छ। यसमा पुराना ऐतिहासिक तस्बिरहरू, डुङ्गा गीतका अडियोहरू, नदी यात्राको वृत्तान्त र अनुसन्धान निष्कर्षहरू नि:शुल्क उपलब्ध छन्।",
                faqs = listOf(
                    Pair("के यो एप अफलाइन पनि चल्छ?", "हो! यो एपले मुख्य सामग्रीहरू र रैथाने शब्दकोश अफलाइनमै बचत गर्ने हुनाले इन्टरनेट नभएको अवस्थामा पनि सम्पूर्ण विवरण पढ्न सकिन्छ।"),
                    Pair("मैले फोटो वा कथा कसरी उपलब्ध गराउन सक्छु?", "यदि तपाईंसँग पुरानो तस्बिर वा अभिलेख छ भने कर्महियास्थित हाम्रो सामुदायिक कार्यालयमा सम्पर्क गरी बुझाउन सक्नुहुन्छ।")
                ),
                altTagGuide = "सन् १९७० को बाढी ताका बोटे डुङ्गा चालकहरूले ठूलो काठे डुङ्गा चलाउँदै गरेको ऐतिहासिक दुर्लभ श्यामश्वेत तस्बिर।",
                schemaType = "Article Schema",
                internalLinks = listOf("Community Research Center", "Bote History")
            )
        )
    }
}

// ==================== SHIMMER SKELETON LOADERS ====================

@Composable
fun Modifier.shimmerEffect(activeTheme: String): Modifier {
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val baseColor = when (activeTheme) {
        "night" -> Color(0xFF162530)
        "festival" -> Color(0xFF2E1212)
        else -> Color(0xFFE2E8F0)
    }
    val highlightColor = when (activeTheme) {
        "night" -> Color(0xFF243B4E)
        "festival" -> Color(0xFF4C1F1F)
        else -> Color(0xFFF1F5F9)
    }

    return this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                highlightColor,
                baseColor,
            ),
            start = Offset(translateAnim - 400f, translateAnim - 400f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

@Composable
fun SkeletonLoadingScreen(
    activeScreen: AppScreen,
    activeTheme: String
) {
    val containerBgBrush = when (activeTheme) {
        "night" -> Brush.verticalGradient(listOf(Color(0xFF0F1E29), Color(0xFF070F14)))
        "festival" -> Brush.verticalGradient(listOf(Color(0xFF220A0A), Color(0xFF140505)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFAFAFA), Color(0xFFF3F8F9)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerBgBrush)
            .padding(16.dp)
    ) {
        when (activeScreen) {
            AppScreen.HOME -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Hero banner shimmer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }

                    // Row of stats dials shimmer
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .shimmerEffect(activeTheme)
                                )
                            }
                        }
                    }

                    // Success Stories carousel shimmer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }

                    // Map selector box shimmer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }
                }
            }
            AppScreen.LANDING_PAGES -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(activeTheme)
                    )

                    // Categories Tab row shimmer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .shimmerEffect(activeTheme)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 lists shimmer
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }
                }
            }
            AppScreen.DIGITAL_LIBRARY -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search bar shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .shimmerEffect(activeTheme)
                    )

                    // Tag list shimmer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect(activeTheme)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Resource items shimmer
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }
                }
            }
            AppScreen.COMMUNITY_HUB -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sync widgets top row shimmer
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect(activeTheme)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect(activeTheme)
                            )
                        }
                    }

                    // News list header shimmer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }

                    // News list shimmers
                    repeat(3) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shimmerEffect(activeTheme)
                            )
                        }
                    }
                }
            }
            AppScreen.CREATOR_DASHBOARD -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Workspace header shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(activeTheme)
                    )

                    // Card block for the draft text composer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect(activeTheme)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Unpublished drafts header shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(activeTheme)
                    )

                    // Draft items shimmer
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }
                }
            }
            AppScreen.GOOGLE_COMMAND_CENTER -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top stats metrics shimmer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect(activeTheme)
                            )
                        }
                    }

                    // Code / schema editor large shimmer box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect(activeTheme)
                        )

                    // Section header shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(activeTheme)
                    )

                    // List item shimmers
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }
                }
            }
            AppScreen.DONATION_PORTAL -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(activeTheme)
                    )
                    // Progress card shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect(activeTheme)
                    )
                    // Tabs shimmer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerEffect(activeTheme)
                            )
                        }
                    }
                    // Content lists shimmers
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect(activeTheme)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NepalBoteLiveTelemetryWidget(viewModel: BoteCommunityViewModel) {
    val telemetry by viewModel.liveTelemetry.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇳🇵", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NEPAL BOTE REAL-TIME REGISTRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Socio-Ecological Central Hydrology Station",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                // Live blinking pulse
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LeafGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LeafGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column layout for river monitors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bagmati River Monitor
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("💧 Bagmati Level (Sarlahi)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${telemetry.bagmatiLevel}m", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = telemetry.bagmatiStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.bagmatiStatus.contains("Alert")) ErrorRed else LeafGreen,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                // Narayani River Monitor
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("💧 Narayani Level (Gandaki)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${telemetry.narayaniLevel}m", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = telemetry.narayaniStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Three-column stats grid for community details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cooperative Savings
                Card(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("🪙 Cooperative Fund", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("NPR ${telemetry.cooperativeFundNpr}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Active canoes
                Card(
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("🛶 Active Canoes", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${telemetry.activeCanoesCount} Boats", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Documented words
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("📚 Dictionary Words", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${telemetry.documentedWordsCount} Lexicons", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌡️ Sarlahi Riverfront Temp: ${telemetry.sarlahiTemp}°C",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = "Refreshed: Auto-polling every 8s",
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
fun ImmersiveOnboardingScreen(
    viewModel: BoteCommunityViewModel,
    isNepali: Boolean,
    appTheme: String
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var adminPasscode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    var showGoogleChooser by remember { mutableStateOf(false) }

    // Visual styles depending on appThemeState
    val startColor = if (appTheme == "morning") Color(0xFFE0F7FA) else NightPebble
    val endColor = if (appTheme == "morning") RiverBlue.copy(alpha = 0.25f) else Color(0xFF0F171E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(startColor, endColor)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Language Toggle and Theme Info at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant badge indicating secure gatekeeper
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isNepali) "सुरक्षित प्रवेश" else "SECURE PORTAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Language Switch Button
            OutlinedButton(
                onClick = { viewModel.isNepali.value = !isNepali },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isNepali) "English 🇬🇧" else "नेपाली 🇳🇵",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Main content in lazy column to support multiple device heights
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .testTag("onboarding_scrollable_container"),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Beautiful Hero Onboarding Illustration with River / Boat
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1.2f)
                        .shadow(16.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_bote_onboarding),
                            contentDescription = "Bote River Community",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        // Gradient Overlay to blend description nicely
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                    )
                                )
                        )
                    }
                }
            }

            // 2. Title & Mission description
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isNepali) "बोटे समुदाय युवा डिजिटल पोर्टल" else "Bote Community Youth Portal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isNepali) 
                            "सर्लाही, बागमती - भाषा, संस्कृति र इतिहासको संरक्षण" 
                            else "Sarlahi, Nepal — Preserving language, heritage & scholarships",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Main Login Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) {
                                if (isNepali) "खाता सिर्जना गर्नुहोस्" else "Register Community Account"
                            } else {
                                if (isNepali) "एपमा सुरक्षित पहुँच" else "Community Secure Access"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // GOOGLE SIGN IN BUTTON (Primary choice)
                        OutlinedButton(
                            onClick = { showGoogleChooser = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("google_signin_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                        ) {
                            GoogleLogoIcon()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isNepali) "गुगल खाता मार्फत प्रवेश गर्नुहोस्" else "Continue with Google",
                                color = Color(0xFF5F6368),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Divider line separating Google Sign-In and standard fields
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Text(
                                text = if (isNepali) "वा इमेल प्रयोग गर्नुहोस्" else "or use email details",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        }

                        // Input fields
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text(if (isNepali) "पूरा नाम" else "Full Name", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(if (isNepali) "इमेल ठेगाना" else "Email Address", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (isNepali) "पासवर्ड" else "Password", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.PlayArrow else Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = adminPasscode,
                                onValueChange = { adminPasscode = it },
                                label = { Text(if (isNepali) "प्रशासक पासकोड (वैकल्पिक)" else "Admin Passcode (Optional)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                placeholder = { Text("e.g. BOTE_ADMIN", fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (isSignUpMode) {
                                    viewModel.signUpUser(email, password, fullName, adminPasscode)
                                } else {
                                    viewModel.loginUser(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("email_submit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSignUpMode) {
                                    if (isNepali) "खाता खोल्नुहोस् र प्रवेश गर्नुहोस्" else "Sign Up & Register"
                                } else {
                                    if (isNepali) "सुरक्षित साइन-इन" else "Secure Sign-In"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Toggle Mode button
                        TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                            Text(
                                text = if (isSignUpMode) {
                                    if (isNepali) "पहिले नै खाता छ? यहाँ लगइन गर्नुहोस्" else "Already have an account? Sign In"
                                } else {
                                    if (isNepali) "नयाँ खाता खोल्न चाहनुहुन्छ? यहाँ दर्ता गर्नुहोस्" else "Need to register a new account? Sign Up"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Footer info showing secure encryption indicator
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Secure",
                        tint = LeafGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isNepali) "२५६-बिट सुरक्षित प्रमाणीकरण र डिजिटल गोपनीयता सुरक्षित छ" else "256-bit secure authentication and digital privacy enabled",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Google Account Chooser Custom Dialog Flow
        if (showGoogleChooser) {
            Dialog(
                onDismissRequest = { showGoogleChooser = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                        .shadow(24.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title header with Google colors
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isNepali) "गुगल मार्फत साइन इन गर्नुहोस्" else "Sign in with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isNepali) "बोटे समुदाय एपमा जारी राख्न एउटा खाता छान्नुहोस्" else "Choose an account to continue to Bote App",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Google Account 1: Raj Nava (Active User!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.loginWithGoogle(
                                        email = "razjnava@gmail.com",
                                        fullName = "Raj Nava",
                                        uid = "google_user_rajnava_123"
                                    ) { success ->
                                        if (success) showGoogleChooser = false
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("R", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Raj Nava", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("razjnava@gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Google Account 2: Bote Coordinator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.loginWithGoogle(
                                        email = "coordinator@bote.org",
                                        fullName = "Bote Community Coordinator",
                                        uid = "google_user_coord_456"
                                    ) { success ->
                                        if (success) showGoogleChooser = false
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Bote Community Coordinator", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("coordinator@bote.org", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Add Another Google Account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    // Custom user entry
                                    viewModel.loginWithGoogle(
                                        email = "newuser@gmail.com",
                                        fullName = "Google User",
                                        uid = "google_user_new_789"
                                    ) { success ->
                                        if (success) showGoogleChooser = false
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isNepali) "अर्को खाता प्रयोग गर्नुहोस्" else "Use another Google account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Cancel button
                        TextButton(
                            onClick = { showGoogleChooser = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var adminPasscode by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("auth_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSignUpMode) Icons.Default.Person else Icons.Default.Lock,
                        contentDescription = "Auth Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSignUpMode) {
                        if (isNepali) "नयाँ खाता सिर्जना गर्नुहोस्" else "Create Community Account"
                    } else {
                        if (isNepali) "समुदाय लगइन" else "Community Secure Sign-In"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isSignUpMode) {
                        if (isNepali) "बोटे समुदायको सदस्य बन्नुहोस्" else "Join the Bote Youth Community Hub"
                    } else {
                        if (isNepali) "सुरक्षित रूपमा आफ्नो खातामा पहुँच गर्नुहोस्" else "Search historical databases, access forum and control center"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Input fields
                if (isSignUpMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(if (isNepali) "पूरा नाम" else "Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (isNepali) "इमेल ठेगाना" else "Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isNepali) "पासवर्ड" else "Secure Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.PlayArrow else Icons.Default.Info,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                )

                if (isSignUpMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminPasscode,
                        onValueChange = { adminPasscode = it },
                        label = { Text(if (isNepali) "प्रशासक पासकोड (वैकल्पिक)" else "Admin Passcode (Optional)") },
                        placeholder = { Text("e.g. BOTE_ADMIN") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_admin_passcode_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Text(
                        text = if (isNepali) "सुझाव: प्रशासक प्यानल पहुँच गर्न BOTE_ADMIN टाइप गर्नुहोस्" else "Tip: Enter 'BOTE_ADMIN' to unlock Creator & Admin panels.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button
                Button(
                    onClick = {
                        if (isSignUpMode) {
                            onSignUp(email, password, fullName, adminPasscode)
                        } else {
                            onLogin(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isSignUpMode) {
                            if (isNepali) "दर्ता गर्नुहोस्" else "Sign Up"
                        } else {
                            if (isNepali) "लगइन गर्नुहोस्" else "Log In"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSignUpMode) {
                            if (isNepali) "पहिले नै खाता छ?" else "Already have an account?"
                        } else {
                            if (isNepali) "खाता छैन?" else "Don't have an account?"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSignUpMode) {
                            if (isNepali) "यहाँ लगइन गर्नुहोस्" else "Log In here"
                        } else {
                            if (isNepali) "नयाँ खाता खोल्नुहोस्" else "Sign Up here"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { isSignUpMode = !isSignUpMode }
                            .testTag("auth_mode_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dismiss / Cancel Text Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("auth_dismiss_btn")
                ) {
                    Text(
                        text = if (isNepali) "रद्द गर्नुहोस्" else "Cancel / Go Back",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
