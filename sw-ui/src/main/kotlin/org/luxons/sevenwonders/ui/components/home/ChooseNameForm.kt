package org.luxons.sevenwonders.ui.components.home

import com.palantir.blueprintjs.Intent
import com.palantir.blueprintjs.bpButton
import com.palantir.blueprintjs.bpInputGroup
import kotlinx.css.*
import kotlinx.html.js.onSubmitFunction
import org.luxons.sevenwonders.ui.redux.RequestChooseName
import org.luxons.sevenwonders.ui.redux.connectDispatch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import styled.css
import styled.styledDiv
import styled.styledForm

private interface ChooseNameFormProps : RProps {
    var chooseUsername: (String) -> Unit
}

private data class ChooseNameFormState(var username: String = "") : RState

private class ChooseNameForm(props: ChooseNameFormProps) : RComponent<ChooseNameFormProps, ChooseNameFormState>(props) {

    override fun ChooseNameFormState.init(props: ChooseNameFormProps) {
        username = ""
    }

    override fun RBuilder.render() {
        styledForm {
            css {
                display = Display.flex
                flexDirection = FlexDirection.row
            }
            attrs.onSubmitFunction = { e -> chooseUsername(e) }
            randomNameButton()
            spacer()
            bpInputGroup(
                large = true,
                placeholder = "Username",
                rightElement = submitButton(),
                value = state.username,
                onChange = { e ->
                    val input = e.currentTarget as HTMLInputElement
                    setState(transformState = { ChooseNameFormState(username = input.value) })
                },
            )
        }
    }

    private fun submitButton(): ReactElement = buildElement {
        bpButton(
            minimal = true,
            icon = "arrow-right",
            intent = Intent.PRIMARY,
            onClick = { e -> chooseUsername(e) },
        )
    }

    private fun RBuilder.randomNameButton() {
        bpButton(
            title = "Generate random name",
            large = true,
            icon = "random",
            intent = Intent.PRIMARY,
            onClick = { e -> fillRandomUsername() },
        )
    }

    private fun fillRandomUsername() {
        setState(ChooseNameFormState(username = randomGreekName()))
    }

    private fun chooseUsername(e: Event) {
        e.preventDefault()
        props.chooseUsername(state.username)
    }

    // TODO this is so bad I'm dying inside
    private fun RBuilder.spacer() {
        styledDiv {
            css {
                margin(2.px)
            }
        }
    }
}

val chooseNameForm: RClass<RProps> = connectDispatch(ChooseNameForm::class) { dispatch, _ ->
    chooseUsername = { name -> dispatch(RequestChooseName(name)) }
}
