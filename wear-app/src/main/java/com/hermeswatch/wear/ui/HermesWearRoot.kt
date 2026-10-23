package com.hermeswatch.wear.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import com.hermeswatch.wear.HermesWearViewModel
import com.hermeswatch.wear.state.WearEvent
import com.hermeswatch.wear.state.WearUiState
import com.hermeswatch.wear.ui.screens.AnswerScreen
import com.hermeswatch.wear.ui.screens.ConfirmScreen
import com.hermeswatch.wear.ui.screens.ErrorScreen
import com.hermeswatch.wear.ui.screens.TalkScreen
import com.hermeswatch.wear.ui.screens.WaitingScreen

@Composable
fun HermesWearRoot(viewModel: HermesWearViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HermesWearContent(state = state, onEvent = viewModel::onEvent)
}

/**
 * Screen host. Every non-Talk screen sits in a [SwipeToDismissBox] with Talk
 * revealed underneath, so the native Wear "swipe right to go back" gesture
 * works everywhere (and cancels while waiting). Screen changes cross-fade with
 * a slight scale — the same feel as system app transitions.
 */
@Composable
fun HermesWearContent(state: WearUiState, onEvent: (WearEvent) -> Unit) {
    val input = rememberTextInputLauncher(
        onText = { onEvent(WearEvent.Ask(it)) },
        onUnavailable = { onEvent(WearEvent.InputUnavailable) },
    )
    val onTalk = input::launch
    val onNewChat = { onEvent(WearEvent.NewChat) }

    BackHandler(enabled = state !is WearUiState.Talk) { onEvent(WearEvent.Dismiss) }

    val swipeState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = swipeState,
        onDismissed = { onEvent(WearEvent.Dismiss) },
        backgroundKey = "talk-background",
        contentKey = state.screenKey,
        hasBackground = state !is WearUiState.Talk,
    ) { isBackground ->
        if (isBackground) {
            TalkScreen(notice = null, onTalk = {}, onNewChat = {}, onClearNotice = {})
        } else {
            AnimatedContent(
                targetState = state,
                contentKey = { it.screenKey },
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(260), initialScale = 0.94f))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 1.04f))
                },
                label = "screen",
            ) { s ->
                when (s) {
                    is WearUiState.Talk -> TalkScreen(
                        notice = s.notice,
                        onTalk = onTalk,
                        onNewChat = onNewChat,
                        onClearNotice = { onEvent(WearEvent.ClearNotice) },
                    )
                    is WearUiState.Waiting -> WaitingScreen(
                        state = s,
                        onCancel = { onEvent(WearEvent.Dismiss) },
                    )
                    is WearUiState.Answer -> AnswerScreen(
                        state = s,
                        onAskAgain = onTalk,
                        onDone = { onEvent(WearEvent.Dismiss) },
                    )
                    is WearUiState.Confirm -> ConfirmScreen(
                        state = s,
                        onChoice = { yes -> onEvent(WearEvent.ConfirmChoice(yes)) },
                    )
                    is WearUiState.Error -> ErrorScreen(
                        state = s,
                        onRetry = { onEvent(WearEvent.Retry) },
                        onBack = { onEvent(WearEvent.Dismiss) },
                    )
                }
            }
        }
    }
}
