package io.github.soundremote.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import io.github.soundremote.R

/**
 * 字符串首选项（如密码），点击弹出输入对话框。
 * 显示时以 "•" 遮蔽实际值，避免明文暴露。
 */
@Composable
internal fun StringPreference(
    title: String,
    summary: String,
    value: String,
    onPreferenceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    defaultValue: String? = null,
    mask: Boolean = true,
) {
    var showEdit by rememberSaveable { mutableStateOf(false) }
    val displayText = if (mask && value.isNotEmpty()) "•".repeat(value.length.coerceAtMost(12)) else value
    val hintText = if (defaultValue == null) {
        summary
    } else {
        val defaultValueText = stringResource(R.string.pref_default_value_template).format(defaultValue)
        "$summary\n$defaultValueText"
    }

    PreferenceItem(
        title = title,
        summary = displayText,
        hint = hintText,
        onClick = { showEdit = true },
        modifier = modifier,
    )
    if (showEdit) {
        var editValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value))
        }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text(title) },
            text = {
                val editFocusRequester = remember { FocusRequester() }
                SideEffect { editFocusRequester.requestFocus() }
                TextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    singleLine = true,
                    modifier = Modifier.focusRequester(editFocusRequester),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onPreferenceChange(editValue.text)
                    showEdit = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
