/*
 * Copyright 2026 Sumon Mahmud (rzk) - AX-01 RZK Software Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example

import com.example.ui.screens.LegalTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensesTest {
    @Test
    fun legalTabs_containAllRequiredSections() {
        val tabs = LegalTab.entries
        assertTrue(tabs.any { it == LegalTab.APACHE_2 })
        assertTrue(tabs.any { it == LegalTab.EULA })
        assertTrue(tabs.any { it == LegalTab.PRIVACY })
        assertTrue(tabs.any { it == LegalTab.TERMS })
        assertTrue(tabs.any { it == LegalTab.COMPANY })
        assertEquals(5, tabs.size)
    }

    @Test
    fun apache2Tab_hasCorrectTitle() {
        assertEquals("Apache 2.0", LegalTab.APACHE_2.title)
    }
}
