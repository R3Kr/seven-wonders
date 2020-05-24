package org.luxons.sevenwonders.ui.components.game

import com.palantir.blueprintjs.*
import kotlinx.css.*
import kotlinx.css.properties.*
import org.luxons.sevenwonders.model.Action
import org.luxons.sevenwonders.model.PlayerMove
import org.luxons.sevenwonders.model.PlayerTurnInfo
import org.luxons.sevenwonders.model.api.PlayerDTO
import org.luxons.sevenwonders.model.cards.HandCard
import org.luxons.sevenwonders.ui.components.GlobalStyles
import org.luxons.sevenwonders.ui.redux.*
import react.*
import react.dom.*
import styled.css
import styled.getClassName
import styled.styledDiv

interface GameSceneStateProps : RProps {
    var playerIsReady: Boolean
    var players: List<PlayerDTO>
    var gameState: GameState?
    var preparedMove: PlayerMove?
    var preparedCard: HandCard?
}

interface GameSceneDispatchProps : RProps {
    var sayReady: () -> Unit
    var prepareMove: (move: PlayerMove) -> Unit
    var unprepareMove: () -> Unit
    var leaveGame: () -> Unit
}

interface GameSceneProps : GameSceneStateProps, GameSceneDispatchProps

private class GameScene(props: GameSceneProps) : RComponent<GameSceneProps, RState>(props) {

    override fun RBuilder.render() {
        styledDiv {
            css {
                background = "url('images/backgrounds/papyrus.jpg')"
                backgroundSize = "cover"
                +GlobalStyles.fullscreen
            }
            val turnInfo = props.gameState?.turnInfo
            if (turnInfo == null) {
                bpNonIdealState(icon = "error", title = "Error: no turn info data")
            } else {
                turnInfoScene(turnInfo)
            }
        }
    }

    private fun RBuilder.turnInfoScene(turnInfo: PlayerTurnInfo) {
        val board = turnInfo.table.boards[turnInfo.playerIndex]
        div {
            turnInfo.scoreBoard?.let {
                scoreTableOverlay(it, props.players, props.leaveGame)
            }
            actionInfo(turnInfo.message)
            boardComponent(board = board)
            handRotationIndicator(turnInfo.table.handRotationDirection)
            val hand = turnInfo.hand
            if (hand != null) {
                handComponent(
                    cards = hand,
                    wonderBuildability = turnInfo.wonderBuildability,
                    preparedMove = props.preparedMove,
                    prepareMove = props.prepareMove
                )
            }
            val card = props.preparedCard
            val move = props.preparedMove
            if (card != null && move != null) {
                preparedMove(card, move)
            }
            if (turnInfo.action == Action.SAY_READY) {
                sayReadyButton()
            }
            productionBar(gold = board.gold, production = board.production)
        }
    }

    private fun RBuilder.actionInfo(message: String) {
        styledDiv {
            css {
                classes.add("bp3-dark")
                display = Display.inlineBlock // so that the cards below don't overlap, but the width is not full
                margin(all = 0.4.rem)
            }
            bpCard(elevation = Elevation.TWO) {
                attrs {
                    this.className = GlobalStyles.getClassName { it::noPadding }
                }
                bpCallout(intent = Intent.PRIMARY, icon = "info-sign") { +message }
            }
        }
    }

    private fun RBuilder.preparedMove(card: HandCard, move: PlayerMove) {
        bpOverlay(isOpen = true, onClose = props.unprepareMove) {
            preparedMove(card, move, props.unprepareMove) {
                css { +GlobalStyles.fixedCenter }
            }
        }
    }

    private fun RBuilder.sayReadyButton(): ReactElement {
        val isReady = props.playerIsReady
        val intent = if (isReady) Intent.SUCCESS else Intent.PRIMARY
        return styledDiv {
            css {
                position = Position.absolute
                bottom = 6.rem
                left = 50.pct
                transform { translate(tx = (-50).pct) }
            }
            bpButtonGroup {
                bpButton(
                    large = true,
                    disabled = isReady,
                    intent = intent,
                    icon = if (isReady) "tick-circle" else "play",
                    onClick = { props.sayReady() }
                ) {
                    +"READY"
                }
                // not really a button, but nice for style
                bpButton(
                    large = true,
                    icon = "people",
                    disabled = isReady,
                    intent = intent
                ) {
                    +"${props.players.count { it.isReady }}/${props.players.size}"
                }
            }
        }
    }
}

fun RBuilder.gameScene() = gameScene {}

private val gameScene: RClass<GameSceneProps> =
        connectStateAndDispatch<GameSceneStateProps, GameSceneDispatchProps, GameSceneProps>(
            clazz = GameScene::class,
            mapDispatchToProps = { dispatch, _ ->
                prepareMove = { move -> dispatch(RequestPrepareMove(move)) }
                unprepareMove = { dispatch(RequestUnprepareMove()) }
                sayReady = { dispatch(RequestSayReady()) }
                leaveGame = { dispatch(RequestLeaveGame()) }
            },
            mapStateToProps = { state, _ ->
                playerIsReady = state.currentPlayer?.isReady == true
                players = state.gameState?.players ?: emptyList()
                gameState = state.gameState
                preparedMove = state.gameState?.currentPreparedMove
                preparedCard = state.gameState?.currentPreparedCard
            }
        )
