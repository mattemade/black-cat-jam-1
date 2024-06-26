package io.itch.mattemade.blackcat.cat

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.input.InputMapController
import com.lehaine.littlekt.util.seconds
import io.itch.mattemade.blackcat.assets.CatAnimations
import io.itch.mattemade.blackcat.input.GameInput
import io.itch.mattemade.blackcat.physics.ContactBits
import io.itch.mattemade.utils.animation.SignallingAnimationPlayer
import io.itch.mattemade.utils.disposing.Disposing
import io.itch.mattemade.utils.disposing.HasContext
import io.itch.mattemade.utils.disposing.Self
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import kotlin.math.abs
import kotlin.time.Duration

class Cat(
    initialPosition: Vec2,
    world: World,
    private val animations: CatAnimations,
    private val controller: InputMapController<GameInput>
) : Disposing by Self(), HasContext<Body> {

    private val size = Vec2(444f*16f/1920f, 366f*9f/1080f) // 3.7 x 3.05
    private val tempVec2 = Vec2()


    private val body = world.createBody(
        BodyDef(
            type = BodyType.DYNAMIC,
            userData = this,
        ).apply {
            position.set(initialPosition)
        }
    )
    private val fixture = body.createFixture(FixtureDef(
        shape = PolygonShape().apply {
            setAsBox(size.x, size.y)
        },
        filter = Filter().apply {
            maskBits = ContactBits.CAT_BIT
        },
        friction = 4f,
    )) ?: error("Cat fixture is null! Should not happen!")

    override val context: Map<Any, Body> = mapOf(Body::class to body)
    private var currentAnimation: SignallingAnimationPlayer = animations.idle
        set(value) {
            value.restart()
            field = value
        }
    private var facingRight = true
    private var state: State = State.IDLE
        set(value) {
            if (field != value) {
                currentAnimation = when (value) {
                    State.IDLE -> animations.idle
                    State.WALKING -> animations.walk
                    State.JUMPING -> animations.jump
                    State.FALLING -> animations.fall
                    State.FREEFALLING -> animations.freefall
                    State.LANDING -> animations.land
                    State.CROUCHING -> animations.crouch
                    State.CROUCH_IDLE -> animations.crouchIdle
                    State.STANDING -> animations.stand
                    State.CRAWLING -> animations.crawl
                    State.ATTACKING -> animations.attack
                }
                field = value
            }
        }

    fun update(dt: Duration) {
        val xMovement = controller.axis(GameInput.HORIZONTAL)
        val yMovement = controller.axis(GameInput.VERTICAL)
        //body.position.x -= xMovement * dt.seconds * 1000f
        //body.position.y -= yMovement * dt.seconds * 500f

        var timeMultiplier = 1.0

        if (controller.pressed(GameInput.JUMP)) {
            if (state == State.WALKING || state == State.IDLE) {
                body.applyLinearImpulse(tempVec2.set(0f, -12f), body.position, true)
                state = State.JUMPING
            } else if (state == State.CROUCHING || state == State.CRAWLING) {
                // if standing on the branch
                // temprorarily disable the branch contact points
                state = State.FREEFALLING
                // after some time enable them back
            }
        }


        if (controller.pressed(GameInput.ATTACK)) {
            if (state == State.JUMPING || state == State.FALLING || state == State.FREEFALLING) {
                // TODO jump attack
                state = State.FREEFALLING
                body.applyLinearImpulse(tempVec2.set(if (xMovement > 0 || facingRight) -12f else 12f, 0f), body.position, true)
            } else {
                state = State.ATTACKING
            }
        }

        if (state == State.ATTACKING) {
            // TODO do nothing?
        } else if (body.linearVelocityY < 0) {
            state = State.JUMPING
        } else if (body.linearVelocityY > 0) {
            if (state == State.JUMPING) {
                state = State.FALLING
            } else if (state != State.FALLING && state != State.FREEFALLING) {
                // TODO
                state = State.FREEFALLING
            }
        } else if (body.linearVelocityX != 0f) {
            if (yMovement < 0) {
                state = State.CRAWLING
            } else {
                state = State.WALKING
            }
        } else { // horizontal velocity 0
            if (state == State.FALLING || state == State.FREEFALLING) {
                state = State.LANDING
            } else if (yMovement < 0) {
                if (state == State.CRAWLING) {
                    state = State.CROUCH_IDLE
                } else if (state != State.CROUCH_IDLE && state != State.CROUCHING) {
                    state = State.CROUCHING
                }
            } else if (state == State.CROUCHING || state == State.CROUCH_IDLE) {
                state = State.STANDING
            } else if (state != State.LANDING && state != State.STANDING) {
                state = State.IDLE
            }
        }

        if (xMovement != 0f) {
            val currentSpeed = abs(body.linearVelocityX)
            if (currentSpeed < 10f) {
                body.applyForceToCenter(tempVec2.set(-xMovement * dt.seconds * 4000f, 0f))
            }
            facingRight = xMovement > 0f
            if (state == State.WALKING || state == State.CRAWLING) {
                timeMultiplier = currentSpeed / 7.0
            }
        }


        currentAnimation.update(dt * timeMultiplier)
        if (currentAnimation.currentKeyFrame == null) {
            state = when (state) {
                State.LANDING -> State.IDLE
                State.STANDING -> State.IDLE
                State.CROUCHING -> State.CROUCH_IDLE
                State.ATTACKING -> State.IDLE
                else -> state.also { println("unexpected animation end in state $state") }
            }
        }
    }

    fun render(batch: Batch) {
        currentAnimation.currentKeyFrame?.let { frame ->
            batch.draw(frame, body.position.x, body.position.y, width = size.x, height = size.y, flipX = facingRight)
        }
    }

    private enum class State {
        IDLE,
        WALKING,
        JUMPING,
        LANDING,
        FALLING,
        FREEFALLING,
        CROUCHING,
        CROUCH_IDLE,
        STANDING,
        CRAWLING,
        ATTACKING,
    }

}