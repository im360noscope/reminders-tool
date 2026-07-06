package com.zacksimpson.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightThemeTokens

data class TextEditorRequest(val title: String, val initialValue: String = "")

/**
 * Reusable single-line text entry backed by the LightOS keyboard. Returns the entered
 * text as the screen result on submit, or nothing if the user backs out.
 *
 * Push it with `navigateTo({ TextEditorScreen(it, TextEditorRequest("...")) }) { text -> ... }`.
 */
class TextEditorScreen(
    sealedActivity: SealedLightActivity,
    private val request: TextEditorRequest,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val textState = rememberTextFieldState(request.initialValue)
        val keyboardOptions = rememberKeyboardOptions()
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            LightTextInputEditor(
                title = request.title,
                state = textState,
                keyboardOptionsFlow = keyboardOptions,
                onSubmit = { goBack(it.toString()) },
                onBack = { goBack(null) },
                // LightTextInputEditor's editorKey defaults to `title`, which it uses to key
                // a viewModel() call for the embedded keyboard. Since this screen is a
                // SimpleLightScreen (no per-screen ViewModelStoreOwner), that viewModel()
                // resolves against the single Activity-wide store — so a fixed key like
                // "Task name" reused across separate pushes (add one task, then another)
                // hands back the *first* push's cached keyboard view-model, still wired to
                // that first push's (now-abandoned) TextFieldState. The keyboard renders and
                // responds, but keystrokes go nowhere visible. `this` is a fresh instance
                // per push, so it keys each editor uniquely.
                // Qualified: inside SwipeBackContainer's content lambda, unqualified `this`
                // would resolve to its BoxScope receiver instead of this screen.
                editorKey = this@TextEditorScreen,
                modifier = Modifier.background(LightThemeTokens.colors.background),
            )
            }
        }
    }
}
