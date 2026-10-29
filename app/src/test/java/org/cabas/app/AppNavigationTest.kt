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
@Config(sdk = [35], qualifiers = "ca-w360dp-h800dp")
class AppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        try { compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
        catch (e: Throwable) { throw AssertionError("'$text' not found. Screen: " + compose.onRoot(useUnmergedTree = true).printToString().replace("\n", " | "), e) }
    }

    @Test fun opensCategoriesAppearanceHelpPrivacyAndActualVersion() {
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Categories").performClick()
        waitForText("Cultura")
        compose.onNodeWithText("Nova categoria").assertIsEnabled()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Moviments recurrents").performClick()
        waitForText("Nou moviment recurrent")
        compose.onNodeWithText("Nou moviment recurrent").assertIsEnabled()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Còpies i exportació").performClick()
        compose.onNodeWithText("Fer una còpia de seguretat").assertIsDisplayed()
        compose.onNodeWithText("Encara no has fet cap còpia de seguretat.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Configuració").performClick()
        compose.onNodeWithText("Tema fosc").performClick()
        compose.onNodeWithText("Protegir Cabàs").assertExists()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Evolució").performClick()
        compose.onNodeWithText("Balanç de l’any").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cerca").performClick()
        compose.onNodeWithText("Escriu una paraula o tria un filtre per buscar en tots els mesos.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Com funciona").performClick()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Privacitat i dades").performClick()
        compose.onNodeWithContentDescription("Menú").performClick()
        compose.onNodeWithText("Quant a Cabàs").performClick()
        compose.onNodeWithText("Versió " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tornar").performClick()
        waitForText("Comença a omplir el teu cabàs")
        compose.onNodeWithContentDescription("Mes següent").assertIsDisplayed()
        compose.onNodeWithText("Despeses per categoria").assertDoesNotExist()
        // Dialog forms are verified on a device: the text cursor animation keeps Robolectric busy.
        compose.onNodeWithText("Afegir moviment").assertIsDisplayed().assertHasClickAction()
    }
}
