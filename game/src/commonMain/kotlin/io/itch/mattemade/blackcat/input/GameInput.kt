package io.itch.mattemade.blackcat.input

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.input.GameAxis
import com.lehaine.littlekt.input.GameButton
import com.lehaine.littlekt.input.InputMapController
import com.lehaine.littlekt.input.Key

enum class GameInput {
    LEFT,
    RIGHT,
    HORIZONTAL, // mapped from LEFT and RIGHT based on axis

    UP,
    DOWN,
    VERTICAL, // mapped from UP and DOWN based on axis, for climbing

    JUMP,
    ATTACK,

    PAUSE,
    ANY,
}

fun Context.bindInputs(): InputMapController<GameInput> =
    InputMapController<GameInput>(input).apply {
        // the 'A' and 'left arrow' keys and the 'x-axis of the left stick' with trigger the 'MOVE_LEFT' input type
        val anyKey = mutableListOf<Key>()
        fun List<Key>.any(): List<Key> = this.also { anyKey.addAll(this) }


        addBinding(GameInput.RIGHT, listOf(Key.D, Key.ARROW_RIGHT).any(), axes = listOf(GameAxis.LX), buttons = listOf(GameButton.RIGHT))
        addBinding(GameInput.LEFT, listOf(Key.A, Key.ARROW_LEFT).any(), axes = listOf(GameAxis.LX), buttons = listOf(GameButton.LEFT))
        addAxis(GameInput.HORIZONTAL, GameInput.RIGHT, GameInput.LEFT)

// creates an axis based off the DOWN and UP input types
        addBinding(GameInput.UP, listOf(Key.W, Key.ARROW_UP).any(), axes = listOf(GameAxis.LY), buttons = listOf(GameButton.UP))
        addBinding(GameInput.DOWN, listOf(Key.S, Key.ARROW_DOWN).any(), axes = listOf(GameAxis.LY), buttons = listOf(GameButton.DOWN))
        addAxis(GameInput.VERTICAL, GameInput.DOWN, GameInput.UP)

        addBinding(GameInput.JUMP, listOf(Key.SPACE, Key.K, Key.Z).any(), buttons = listOf(GameButton.XBOX_A))
        // TODO: do we need an attack?
        //addBinding(GameInput.ATTACK, listOf(Key.SHIFT_LEFT, Key.J, Key.X).any(), buttons = listOf(GameButton.XBOX_X))

        addBinding(GameInput.PAUSE, listOf(Key.P).any(), buttons = listOf(GameButton.START))

        addBinding(GameInput.ANY, anyKey)

        mode = InputMapController.InputMode.GAMEPAD

        input.addInputProcessor(this)
    }