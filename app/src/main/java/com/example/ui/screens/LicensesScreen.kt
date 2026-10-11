package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SpaceCard
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class LegalTab(val title: String, val icon: ImageVector) {
    APACHE_2("Apache 2.0", Icons.Default.Gavel),
    EULA("Software License", Icons.Default.VerifiedUser),
    PRIVACY("Privacy Policy", Icons.Default.Policy),
    TERMS("Terms of Service", Icons.Default.Gavel),
    COMPANY("Company & Contact", Icons.Default.Language)
}

@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(LegalTab.APACHE_2) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("licenses_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "LEGAL & SOFTWARE LICENSES",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "AX-01 RZK Software Company",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegalTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = tab }
                            .testTag("tab_${tab.name.lowercase()}"),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SpaceCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else SpaceCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    LegalTab.APACHE_2 -> {
                        item {
                            ApacheLicenseSection()
                        }
                    }
                    LegalTab.EULA -> {
                        item {
                            EulaSection()
                        }
                    }
                    LegalTab.PRIVACY -> {
                        item {
                            PrivacyPolicySection()
                        }
                    }
                    LegalTab.TERMS -> {
                        item {
                            TermsOfServiceSection()
                        }
                    }
                    LegalTab.COMPANY -> {
                        item {
                            CompanyContactsSection(
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                onSendEmail = { email ->
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$email")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApacheLicenseSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = NeonCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "OPEN SOURCE COMPLIANCE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Apache License, Version 2.0",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Copyright 2026 Sumon Mahmud (rzk) - AX-01 RZK Software Company",
                color = NeonAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = """
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
                """.trimIndent(),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = SpaceDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "TERMS & CONDITIONS SUMMARY",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = """
• Grant of Copyright License: Perpetual, worldwide, non-exclusive, no-charge, royalty-free license to reproduce, prepare derivative works, publicly display, and distribute.
• Grant of Patent License: Explicit grant of patent rights from contributors.
• Redistribution: Permitted provided that recipients receive a copy of this license, modified files carry prominent notices, and copyright/attribution notices are retained.
• Disclaimer of Warranty: Software provided on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
• Limitation of Liability: Licensors and contributors are not liable for direct, indirect, special, incidental, or consequential damages.
                        """.trimIndent(),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EulaSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Software Licenses and End-User License Agreement (EULA)",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Effective Date: October 4, 2026 | AX-01 RZK Software Company",
                color = NeonCyan,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Text(
                text = """
Welcome to AX-01 RZK Software Company ("we," "our," or "us"). This Software Licenses document outlines the terms and conditions under which you may access, install, and use our software products, applications, and services. By downloading, installing, or using any software provided by AX-01 RZK Software Company, you agree to be bound by the terms of this End-User License Agreement (EULA).

1. Scope of License:
Subject to your compliance with this agreement and payment of any applicable fees, AX-01 RZK Software Company grants you a limited, non-exclusive, non-transferable, revocable license to use our software products strictly for your internal business operations or personal use, depending on the specific product tier purchased.

2. License Types and Tiers:
• Individual / Developer License: Permits a single named user to install and use the software on up to two authorized devices.
• Team / Enterprise License: Permits multiple authorized users within a single organization to access and utilize the software concurrently, subject to the seat limit.
• Evaluation / Trial License: Granted on a temporary basis (typically 14 to 30 days) solely for testing and evaluation purposes ("AS IS").

3. Restrictions on Use:
You agree not to copy, modify, adapt, translate, create derivative works, reverse engineer, decompile, disassemble, rent, lease, lend, sell, sublicense, or distribute the software except to the extent permitted by applicable law.

4. Intellectual Property Rights:
All intellectual property rights, titles, and interests in and to the software, documentation, source code, designs, and trademarks remain the exclusive property of AX-01 RZK Software Company and its licensors.

5. Updates and Maintenance:
AX-01 RZK Software Company may provide updates, patches, and enhancements subject to this agreement.

6. Termination:
This license is effective until terminated. Your rights terminate automatically if you fail to comply with any terms.

7. Disclaimer of Warranties & Limitation of Liability:
The software is provided "AS IS" and "AS AVAILABLE" with all faults and without warranty of any kind.
                """.trimIndent(),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PrivacyPolicySection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Privacy Policy for AX-01 RZK Software Company",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Effective Date: October 4, 2026 | Last Updated: October 4, 2026",
                color = NeonCyan,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Text(
                text = """
At AX-01 RZK Software Company ("we," "our," or "us"), we are committed to protecting your privacy and ensuring that your personal information is handled in a safe and responsible manner. This Privacy Policy outlines how we collect, use, disclose, and safeguard your information when you visit our website, use our software applications, or engage with our services.

1. Information We Collect:
• Personal Data: Identifying details provided when registering or contacting customer support.
• Usage Data: Information on how you interact with our apps, diagnostic logs, and performance metrics.
• Device Information: Hardware models, operating system versions, and mobile network information.

2. How We Use Your Information:
• Provide, operate, and maintain software products and services.
• Process transactions and manage user accounts.
• Improve, personalize, and expand features and user experience.
• Respond to technical support inquiries and deliver service notices.
• Comply with legal obligations and enforce agreements.

3. Sharing and Disclosure of Information:
We do NOT sell, trade, or rent personal information to third parties. Data is only shared with trusted service providers under strict confidentiality agreements, or as required by law.

4. Data Security & Rights:
We implement robust administrative, technical, and physical security measures. Users hold the rights to Access, Rectification, Erasure, Restriction, and Data Portability.
                """.trimIndent(),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun TermsOfServiceSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Terms of Service for AX-01 RZK Software Company",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Effective Date: October 4, 2026 | Last Updated: October 4, 2026",
                color = NeonCyan,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Text(
                text = """
Welcome to AX-01 RZK Software Company ("we," "our," or "us"). These Terms of Service govern your access to and use of our website, software products, mobile applications, and related services.

1. Acceptance of Terms:
By downloading, creating an account, or using our Services, you acknowledge that you have read, understood, and agreed to be bound by these Terms and our Privacy Policy.

2. Account Registration and Security:
You agree to provide accurate, complete information, maintain credentials securely, and accept responsibility for account activities.

3. Acceptable Use Policy:
You agree not to use the Services for unlawful purposes, attempt unauthorized access, disrupt service performance, or transmit malicious code.

4. Intellectual Property Rights:
All content, features, functionality, software code, designs, and trademarks are the exclusive property of AX-01 RZK Software Company.

5. Subscription, Fees, and Payment:
Fees are described at time of purchase and are non-refundable except as required by law.

6. Termination:
We may suspend or terminate access immediately without notice if you breach these Terms.
                """.trimIndent(),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun CompanyContactsSection(
    onOpenUrl: (String) -> Unit,
    onSendEmail: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AX-01 RZK SOFTWARE COMPANY",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Copyright © 2026 Sumon Mahmud (rzk). All Rights Reserved.",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Surface(
                color = SpaceDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "\"শূন্য থেকে শুরু শূন্যতেই শেষ\"",
                        color = NeonGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automated Data Feed every 24 Hours. AI Quantum security. App Life Time 1000 years.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "OFFICIAL CONTACT CHANNELS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            ContactChannelRow(
                icon = Icons.Default.Email,
                title = "Email",
                value = "ax01rzk@gmail.com",
                accentColor = NeonCyan,
                onClick = { onSendEmail("ax01rzk@gmail.com") }
            )

            ContactChannelRow(
                icon = Icons.Default.Language,
                title = "Official Website",
                value = "https://www.ax01rzk.com",
                accentColor = NeonGreen,
                onClick = { onOpenUrl("https://www.ax01rzk.com") }
            )

            ContactChannelRow(
                icon = Icons.Default.Phone,
                title = "SMS Support",
                value = "01990761212 / 0199076122",
                accentColor = NeonAmber,
                onClick = {}
            )

            ContactChannelRow(
                icon = Icons.Default.Share,
                title = "Facebook Page",
                value = "facebook.com/share/1MG3yKLLFF/",
                accentColor = NeonCyan,
                onClick = { onOpenUrl("https://www.facebook.com/share/1MG3yKLLFF/") }
            )

            ContactChannelRow(
                icon = Icons.Default.VideoLibrary,
                title = "YouTube Channel",
                value = "youtube.com/@ax-01rzk-youtube",
                accentColor = NeonAmber,
                onClick = { onOpenUrl("https://www.youtube.com/@ax-01rzk-youtube") }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = SpaceBlack,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "This software and its source code are proprietary and confidential. Unauthorized copying, modification, or distribution of this file, via any medium, is strictly prohibited.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactChannelRow(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
