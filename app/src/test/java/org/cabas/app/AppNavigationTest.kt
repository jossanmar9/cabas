package org.cabas.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class AppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun opensCategoriesAppearanceHelpPrivacyAndActualVersion() {
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Categories").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Necessitats").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nova categoria").assertIsEnabled()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Moviments recurrents").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Nou moviment recurrent").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nou moviment recurrent").assertIsEnabled()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Còpies i exportació").performClick()
        compose.onNodeWithText("Fer una còpia de seguretat").assertIsDisplayed()
        compose.onNodeWithText("Encara no has fet cap còpia de seguretat.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Aspecte").performClick()
        compose.onNodeWithText("Tema fosc").performClick()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Com funciona").performClick()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Privacitat i dades").performClick()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Quant a Cabàs").performClick()
        compose.onNodeWithText("Versió " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tornar").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Comença a omplir el teu cabàs").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Mes següent").assertIsDisplayed()
        compose.onNodeWithText("Despeses per categoria").assertDoesNotExist()
        compose.onNodeWithText("Afegir moviment").performClick()
        compose.onNodeWithText("Import en euros").assertExists()
        compose.onNodeWithText("Cancel·lar").performClick()
        compose.onNodeWithText("Import en euros").assertDoesNotExist()
    }
}
