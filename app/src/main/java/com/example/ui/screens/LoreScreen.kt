package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CruiserRed
import com.example.ui.theme.DroneViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TerminalDark
import com.example.ui.theme.TerminalGreen

@Composable
fun LoreScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalDark)
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
                    modifier = Modifier.testTag("lore_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TerminalGreen
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[ CLASSIFIED ARCHIVE // AX-01 ]",
                    color = TerminalGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TerminalSectionCard(
                        title = "PROJECT VOOMINIGHT SPECIFICATION",
                        content = """
                            DESIGNATION: AX-01 VOOMINIGHT HEAVY INTERCEPTOR
                            MANUFACTURER: Orbit Defense Command (ODC)
                            PROPULSION: Dual Sub-Warp Ion Thrusters
                            PRIMARY WEAPON: Photonic Plasma Blaster ('|')
                            SHIELD ARRAY: Hexagonal Polarized Deflector Field
                            OVERDRIVE MATRIX: Full-Spectrum EMP Shockwave Generator
                            
                            The AX-01 was developed as Humanity's final automated response system against the invading fleet. Piloted by neuro-linked tactical commanders, its adaptive modular core absorbs enemy kinetic scrap to continuously reinforce hull plating and weapon harmonics.
                        """.trimIndent()
                    )
                }

                item {
                    TerminalSectionCard(
                        title = "THREAT DOSSIER: INVASION FLEET",
                        content = """
                            [v] RECON DRONE
                            Threat Level: Low | Speed: Fast
                            Swarm units designed to overwhelm perimeter sensor arrays. Rapid zig-zag flight trajectories.

                            [W] HEAVY CRUISER
                            Threat Level: High | Armor: Reinforced
                            Armored frontline dreadnoughts capable of sustaining multiple laser impacts and returning charged plasma fire.

                            [o] KINETIC ASTEROID
                            Threat Level: Environmental | Mass: Dense
                            Deep space meteoroids trapped in gravity wake. Direct kinetic impacts will fracture asteroids into two high-velocity sub-fragments.

                            [M] TITAN MOTHERSHIP
                            Threat Level: Extreme | Class: Sector Dreadnought
                            Colossal fleet command flagships armed with tri-directional heavy plasma cannons. Appears every 5 sectors.
                        """.trimIndent()
                    )
                }

                item {
                    TerminalSectionCard(
                        title = "ORIGINAL C-ENGINE FIRMWARE v1.0",
                        content = """
                            /* AX-01 VOOMINIGHT DEFENSE SYSTEM */
                            // Compiled on Space Grid Engine 60x15
                            #define SCREEN_WIDTH 60
                            #define SCREEN_HEIGHT 15
                            
                            // AX-01 Geometry: buffer[y][x] = 'A'; buffer[y][x-1] = '<'; buffer[y][x+1] = '>';
                            // Scoring Formula: score += (enemy.type * 15) * level_multiplier;
                            // Shield Matrix: Absorbs 25-45 kinetic impact prior to hull breach.
                        """.trimIndent()
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalSectionCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalDark),
        border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = ">> $title",
                color = TerminalGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                color = TerminalGreen.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
