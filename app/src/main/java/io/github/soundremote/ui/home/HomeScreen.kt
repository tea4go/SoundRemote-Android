package io.github.soundremote.ui.home

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import io.github.soundremote.R
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.ListItemSupport
import io.github.soundremote.ui.theme.SoundRemoteTheme
import io.github.soundremote.ui.theme.resolveHotkeyPalette
import io.github.soundremote.util.ConnectionState
import io.github.soundremote.util.HotkeyDescription
import io.github.soundremote.util.Key
import io.github.soundremote.util.TestTag

private val listItemPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUIState,
    @StringRes messageId: Int?,
    onSendHotkey: (hotkeyId: Int) -> Unit,
    onSendKey: (Key) -> Unit,
    onNavigateToEditHotkey: (hotkeyId: Int) -> Unit,
    onConnect: (address: String) -> Unit,
    onDisconnect: () -> Unit,
    onSetMuted: (muted: Boolean) -> Unit,
    onMessageShown: () -> Unit,
    onNavigateToHotkeyList: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Messages
    messageId?.let { id ->
        val message = stringResource(id)
        showSnackbar(message, SnackbarDuration.Short)
        onMessageShown()
    }

    var address by rememberSaveable(uiState.serverAddress, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.serverAddress))
    }
    val onAddressChange: (TextFieldValue) -> Unit = { newAddressValue ->
        cleanAddressInput(newAddressValue, address)?.let { address = it }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    IconToggleButton(
                        checked = uiState.muted,
                        onCheckedChange = { onSetMuted(it) }
                    ) {
                        if (uiState.muted) {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_mute),
                                contentDescription = stringResource(R.string.action_unmute_app)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_up),
                                contentDescription = stringResource(R.string.action_mute_app)
                            )
                        }
                    }
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.navigation_menu)
                            )
                        }
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.testTag(TestTag.NAVIGATION_MENU)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_events)) },
                                onClick = dropUnlessResumed(lifecycleOwner) {
                                    showMenu = false
                                    onNavigateToEvents()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_settings)) },
                                onClick = dropUnlessResumed(lifecycleOwner) {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_about)) },
                                onClick = dropUnlessResumed(lifecycleOwner) {
                                    showMenu = false
                                    onNavigateToAbout()
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            // 键盘弹起时隐藏 FAB，把有限的垂直空间腾给地址输入框（尤其在横屏下必要）
            @OptIn(ExperimentalLayoutApi::class)
            if (!WindowInsets.isImeVisible) {
                FloatingActionButton(
                    onClick = dropUnlessResumed {
                        onNavigateToHotkeyList()
                    },
                    modifier = Modifier
                        .padding(bottom = 48.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_edit_filled),
                        stringResource(R.string.action_edit_hotkeys),
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
            .testTag("homeScreen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                AddressEdit(
                    address = address,
                    recentAddresses = uiState.recentServersAddresses,
                    onChange = onAddressChange,
                    onConnect = { onConnect(address.text) },
                    modifier = Modifier.weight(1f)
                )
                ConnectButton(
                    connectionState = uiState.connectionState,
                    onConnect = { onConnect(address.text) },
                    onDisconnect = onDisconnect,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp))
            LazyColumn(
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    // fill = false 允许在空间不足时收缩到 0，把空间让给上面的输入框
                    .weight(1f, fill = false),
            ) {
                itemsIndexed(items = uiState.hotkeys, key = { _, item -> item.id }) { idx, hotkey ->
                    val palette = resolveHotkeyPalette(hotkey.colorIndex, idx)
                    HotkeyItem(
                        name = hotkey.name,
                        description = hotkey.description.asString(),
                        onClick = { onSendHotkey(hotkey.id) },
                        onLongClick = dropUnlessResumed {
                            onNavigateToEditHotkey(hotkey.id)
                        },
                        backgroundColor = palette.content,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp))
            MediaBar(onSendKey)
        }
    }
}

/**
 * Filter out leading zeroes and everything else except digits and dots
 */
private fun cleanAddressInput(newValue: TextFieldValue, oldValue: TextFieldValue): TextFieldValue? {
    if (newValue.text == oldValue.text) return newValue
    val newText = newValue.text.filter { it.isDigit() || it == '.' }.trimStart { it == '0' }
    if (newText != oldValue.text) return newValue.copy(text = newText)
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressEdit(
    address: TextFieldValue,
    recentAddresses: List<String>,
    onChange: (TextFieldValue) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRecentServers by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val enabled = true
    val singleLine = true
    // 用 BasicTextField + OutlinedTextFieldDefaults.DecorationBox 自组装：
    // Material 3 顶层 OutlinedTextField 强制 56dp 最小高度，这里用 DecorationBox 把
    // contentPadding 上下压到 4dp，让输入框整体更矮（约 32dp 内容 + 边框）。
    BasicTextField(
        value = address,
        onValueChange = onChange,
        textStyle = LocalTextStyle.current.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            color = LocalContentColor.current,
        ),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            keyboardType = KeyboardType.Number,
        ),
        keyboardActions = KeyboardActions(onAny = { onConnect() }),
        interactionSource = interactionSource,
        modifier = modifier,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = address.text,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        stringResource(R.string.server_address),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                trailingIcon = if (recentAddresses.isEmpty()) {
                    null
                } else {
                    {
                        Icon(
                            painterResource(R.drawable.ic_arrow_drop_down),
                            stringResource(R.string.action_recent_servers),
                            Modifier
                                .size(24.dp)
                                .rotate(if (showRecentServers) 180f else 0f)
                                .clickable { showRecentServers = !showRecentServers },
                        )
                    }
                },
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                    )
                },
                // 关键：默认 vertical padding 是 16dp（无 label 时也是 16），这里压到 4dp
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    )
    if (showRecentServers) {
        AlertDialog(
            title = {
                Text(stringResource(R.string.recent_servers_title))
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    for (i in recentAddresses.indices.reversed()) {
                        ListItemHeadline(
                            text = recentAddresses[i],
                            modifier = Modifier
                                .clickable {
                                    onChange(TextFieldValue(recentAddresses[i]))
                                    showRecentServers = false
                                }
                                .height(56.dp)
                                .fillMaxWidth()
                                .then(listItemPadding),
                        )
                    }
                }
            },
            onDismissRequest = { showRecentServers = false },
            dismissButton = {
                TextButton(
                    onClick = { showRecentServers = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun ConnectButton(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectedColor = colorResource(R.color.indicatorConnected)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        if (connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator()
        }
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                IconButton(
                    onClick = onConnect,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_forward),
                        stringResource(R.string.connect_caption),
                    )
                }
            }

            ConnectionState.CONNECTING,
            ConnectionState.CONNECTED -> {
                val tint = if (connectionState == ConnectionState.CONNECTED) {
                    connectedColor
                } else {
                    LocalContentColor.current
                }
                IconButton(
                    onClick = onDisconnect,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.disconnect_caption),
                        tint = tint
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HotkeyItem(
    name: String,
    description: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 名称：比默认 bodyLarge (16sp) 加大 4 号（20sp）并加粗，加强主界面视觉重点
        Text(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ListItemSupport(text = description)
    }
}

@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light",
    device = "id:Nexus 5"
)
@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark",
    device = "id:Nexus 5"
)
@Composable
private fun Portrait() {
    HomePreview()
}

@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light",
    device = "spec:parent=Nexus 5,orientation=landscape"
)
@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark",
    device = "spec:parent=Nexus 5,orientation=landscape"
)
@Composable
private fun Landscape() {
    HomePreview()
}

@Composable
private fun HomePreview() {
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var id = 0
    SoundRemoteTheme {
        HomeScreen(
            uiState = HomeUIState(
                serverAddress = "192.168.0.1",
                hotkeys = listOf(
                    HomeHotkeyUIState(
                        ++id,
                        "X",
                        HotkeyDescription.WithString("X"),
                        colorIndex = -1,
                    ),
                    HomeHotkeyUIState(
                        ++id,
                        "Volume up",
                        HotkeyDescription.WithLabelId("Ctrl + Alt + ", R.string.key_delete),
                        colorIndex = -1,
                    ),
                ),
                connectionState = connectionState,
                muted = true,
            ),
            messageId = null,
            onNavigateToEditHotkey = {},
            onConnect = { connectionState = ConnectionState.CONNECTING },
            onDisconnect = {
                connectionState = if (connectionState == ConnectionState.CONNECTING) {
                    ConnectionState.CONNECTED
                } else {
                    ConnectionState.DISCONNECTED
                }
            },
            onSetMuted = {},
            onSendHotkey = {},
            onSendKey = {},
            onMessageShown = {},
            onNavigateToHotkeyList = {},
            onNavigateToEvents = {},
            onNavigateToSettings = {},
            onNavigateToAbout = {},
            showSnackbar = { _, _ -> },
        )
    }
}
