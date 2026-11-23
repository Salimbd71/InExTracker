package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.components.PremiumAppHeader
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.PrimaryGreen

@Composable
fun LegalDocumentScreen(
    initialTab: Int = 0, // 0 = Privacy Policy, 1 = Terms & Conditions
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumAppHeader(
            title = if (selectedTab == 0) strings.privacyPolicyTitle else strings.termsConditionsTitle,
            subtitle = "InEx Tracker v${BuildConfig.VERSION_NAME}",
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("legal_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == "en") "Privacy Policy" else "প্রাইভেসি পলিসি", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == "en") "Terms & Conditions" else "শর্তাবলী", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        val sections = remember(selectedTab, lang) {
            if (selectedTab == 0) privacyPolicySections(lang) else termsSections(lang)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "legal_effective_banner") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PrimaryGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (lang == "en") {
                            "Effective Date: November 2026 • Contact: techbark353@gmail.com"
                        } else {
                            "কার্যকর তারিখ: নভেম্বর ২০২৬ • যোগাযোগ: techbark353@gmail.com"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(sections.size, key = { idx -> "${selectedTab}_$idx" }) { idx ->
                val (heading, body) = sections[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun privacyPolicySections(lang: String): List<Pair<String, String>> {
    return if (lang == "en") {
        listOf(
            "1. Overview & Developer Contact" to
                "InEx Tracker is a personal income, expense, budget, and debt tracking application. We respect your privacy and handle all user data transparently in accordance with Google Play Developer Program Policies. Developer Contact: techbark353@gmail.com.",
            "2. Data We Collect & Process" to
                "• Account Information (Optional Google Sign-In): Name, email address, Google profile photo URL, and Firebase User ID (UID) used solely to authenticate your account and sync your records.\n• Financial Records Entered by You: Income/expense transactions, categories, payment methods, budgets, and debt/receivable entries (including optional contact name/phone you enter manually).\n• Advertising & Diagnostics: Google AdMob SDK may process your device's Advertising ID, IP address, and basic interaction telemetry to serve and measure ads.",
            "3. Offline Mode & Cloud Storage Security" to
                "• Offline Guest Mode: All records stay locally on your device in an encrypted-at-rest Room SQLite database.\n• Cloud Sync Mode: When signed in with Google, your records sync over HTTPS/TLS to Google Firebase Realtime Database (asia-southeast1) isolated strictly under your unique Firebase UID.\n• Optional App Lock: You may enable a 4-digit local PIN lock inside Profile Settings.",
            "4. Third-Party Services Used" to
                "InEx Tracker integrates the following Google services, each governed by Google's Privacy Policy:\n• Firebase Authentication (Google Sign-In)\n• Firebase Realtime Database\n• Google Mobile Ads (AdMob)\nWe never sell your personal or financial data to third parties or data brokers.",
            "5. Permissions Used" to
                "• Internet & Network State: Required for cloud backup sync, Google Sign-In, and AdMob ads.\n• Notifications (POST_NOTIFICATIONS, Optional): Used only for user-requested debt due-date reminders.\n• Zero Broad Storage Access: PDF report exports use Android's secure FileProvider & Storage Access Framework without requesting broad file/media storage permissions.",
            "6. Data Retention & Account Deletion (Play Policy)" to
                "You can delete individual transactions, debts, or budgets at any time. Signed-in users can permanently delete their entire Firebase cloud database and authentication account directly from Profile → Delete Account & Data, or by emailing techbark353@gmail.com.",
            "7. Children's Privacy & Policy Updates" to
                "InEx Tracker is not directed toward children under the age of 13 and does not knowingly collect data from children. Updates to this Privacy Policy will be reflected directly inside this screen with an updated effective date."
        )
    } else {
        listOf(
            "১. পরিচিতি ও ডেভেলপার যোগাযোগ" to
                "InEx Tracker একটি ব্যক্তিগত আয়-ব্যয়, বাজেট এবং দেনা-পাওনা হিসাব রাখার অ্যাপ। Google Play Developer Policy অনুযায়ী আপনার তথ্যের গোপনীয়তা রক্ষায় আমরা প্রতিশ্রুতিবদ্ধ। যোগাযোগের ইমেইল: techbark353@gmail.com।",
            "২. কী কী তথ্য সংগ্রহ ও প্রক্রিয়াজাত করা হয়" to
                "• অ্যাকাউন্ট তথ্য (Google Sign-In ব্যবহার করলে): আপনার নাম, ইমেইল, Google প্রোফাইল ছবি এবং Firebase UID শুধুমাত্র লগইন ও ক্লাউড সিঙ্কের জন্য ব্যবহৃত হয়।\n• আপনার এন্ট্রি করা হিসাবের তথ্য: আয়-ব্যয় লেনদেন, ক্যাটাগরি, বাজেট এবং দেনা-পাওনার তথ্য।\n• বিজ্ঞাপন (AdMob): Google Mobile Ads SDK বিজ্ঞাপন প্রদর্শনের জন্য ডিভাইসের Advertising ID ও সাধারণ ডায়াগনস্টিক তথ্য ব্যবহার করতে পারে।",
            "৩. অফলাইন ব্যবহার ও ক্লাউড নিরাপত্তা" to
                "• গেস্ট (অফলাইন) মোড: আপনার সব হিসাব শুধুমাত্র আপনার ফোনের লোকাল ডেটাবেজে সংরক্ষিত থাকে।\n• ক্লাউড সিঙ্ক মোড: Google দিয়ে লগইন করলে আপনার ডেটা HTTPS/TLS এনক্রিপশনের মাধ্যমে Firebase Realtime Database (asia-southeast1)-এ শুধুমাত্র আপনার ব্যক্তিগত UID-এর অধীনে সুরক্ষিত থাকে।",
            "৪. থার্ড-পার্টি সার্ভিস" to
                "অ্যাপটি নিচের Google সার্ভিসগুলো ব্যবহার করে:\n• Firebase Authentication\n• Firebase Realtime Database\n• Google AdMob\nআমরা কখনোই আপনার ব্যক্তিগত বা আর্থিক তথ্য কোনো তৃতীয় পক্ষের কাছে বিক্রি করি না।",
            "৫. অ্যাপ পারমিশনসমূহ" to
                "• Internet / Network State: ক্লাউড সিঙ্ক, Google লগইন ও বিজ্ঞাপনের জন্য।\n• Notifications (ঐচ্ছিক): দেনা-পাওনার নির্ধারিত তারিখের রিমাইন্ডারের জন্য।\n• কোনো অপ্রয়োজনীয় স্টোরেজ পারমিশন নেওয়া হয় না: PDF রিপোর্ট এক্সপোর্টের জন্য নিরাপদ FileProvider ও সিস্টেম শেয়ারিং ব্যবহার করা হয়।",
            "৬. ডেটা ও অ্যাকাউন্ট স্থায়ীভাবে মুছে ফেলা (Account Deletion)" to
                "আপনি যেকোনো সময় যেকোনো লেনদেন মুছে ফেলতে পারেন। এছাড়া প্রোফাইল সেটিংস থেকে 'অ্যাকাউন্ট ও ডেটা মুছুন' অপশন ব্যবহার করে আপনার ক্লাউড ও ডিভাইসের সব ডেটা স্থায়ীভাবে মুছে ফেলতে পারবেন অথবা techbark353@gmail.com-এ ইমেইল করতে পারেন।",
            "৭. শিশুদের গোপনীয়তা ও নীতিমালা পরিবর্তন" to
                "এই অ্যাপটি ১৩ বছরের কম বয়সী শিশুদের জন্য তৈরি নয়। নীতিমালায় কোনো পরিবর্তন হলে এই স্ক্রিনে তা আপডেট করা হবে।"
        )
    }
}

private fun termsSections(lang: String): List<Pair<String, String>> {
    return if (lang == "en") {
        listOf(
            "1. Acceptance of Terms" to
                "By downloading or using InEx Tracker, you agree to these Terms & Conditions and our Privacy Policy. If you do not agree, please discontinue use of the application.",
            "2. Personal Bookkeeping Tool (No Financial Advice)" to
                "InEx Tracker is a personal bookkeeping and calculation tool designed to assist with tracking daily expenses, budgets, and personal debts. It does not provide certified accounting, tax, legal, or financial advisory services.",
            "3. User Responsibilities & Backups" to
                "• You are responsible for the accuracy of the amounts, dates, and records you enter.\n• While cloud sync backs up data for signed-in users, we recommend exporting periodic PDF backups for critical financial records.",
            "4. Advertising & Service Availability" to
                "InEx Tracker is provided free of charge and supported by Google AdMob advertisements. We strive to keep cloud sync available 24/7, though temporary maintenance or network outages may affect sync availability.",
            "5. Acceptable Use & Limitation of Liability" to
                "You agree not to misuse, reverse-engineer, or disrupt the app's Firebase backend or ad integrations. To the maximum extent permitted by law, the developer is not liable for indirect losses resulting from device failure, forgotten PIN codes, or inaccurate manual entries.",
            "6. Contact" to
                "For support, feedback, or legal inquiries, contact techbark353@gmail.com."
        )
    } else {
        listOf(
            "১. শর্তাবলীর সম্মতি" to
                "InEx Tracker অ্যাপটি ব্যবহার করার মাধ্যমে আপনি এই শর্তাবলী এবং আমাদের প্রাইভেসি পলিসির সাথে সম্মত হচ্ছেন।",
            "২. ব্যক্তিগত হিসাব সহায়িকা" to
                "InEx Tracker একটি ব্যক্তিগত আয়-ব্যয়, বাজেট ও দেনা-পাওনা হিসাব রাখার টুল। এটি কোনো পেশাদার ট্যাক্স, আইনি বা আর্থিক পরামর্শদাতা সেবা নয়।",
            "৩. ব্যবহারকারীর দায়িত্ব ও ব্যাকআপ" to
                "• অ্যাপে এন্ট্রি করা প্রতিটি টাকার পরিমাণ ও তথ্যের সঠিকতা যাচাই করার দায়িত্ব ব্যবহারকারীর।\n• Google অ্যাকাউন্ট দিয়ে লগইন করলে অটো ক্লাউড ব্যাকআপ হয়; তবে গুরুত্বপূর্ণ হিসাবের জন্য নিয়মিত PDF রিপোর্ট সংরক্ষণ করার পরামর্শ দেওয়া হলো।",
            "৪. বিজ্ঞাপন ও সেবার প্রাপ্যতা" to
                "অ্যাপটি সম্পূর্ণ বিনামূল্যে ব্যবহারের জন্য Google AdMob বিজ্ঞাপনের মাধ্যমে পরিচালিত হয়। সার্ভার রক্ষণাবেক্ষণ বা ইন্টারনেট সমস্যার কারণে সাময়িকভাবে ক্লাউড সিঙ্ক বিঘ্নিত হতে পারে।",
            "৫. দায়বদ্ধতার সীমা" to
                "অ্যাপের কোনো অপব্যবহার বা অননুমোদিত পরিবর্তন নিষিদ্ধ। ডিভাইস নষ্ট হওয়া, ভুল তথ্য এন্ট্রি বা পিন ভুলে যাওয়ার কারণে সৃষ্ট ক্ষতির জন্য ডেভেলপার দায়ী থাকবেন না।",
            "৬. যোগাযোগ" to
                "যেকোনো সহায়তা বা প্রশ্নের জন্য ইমেইল করুন: techbark353@gmail.com।"
        )
    }
}
