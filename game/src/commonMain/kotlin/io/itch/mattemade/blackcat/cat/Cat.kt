package io.itch.mattemade.blackcat.cat

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.input.InputMapController
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.seconds
import com.soywiz.korma.geom.Angle
import io.itch.mattemade.blackcat.assets.CatAnimations
import io.itch.mattemade.blackcat.input.GameInput
import io.itch.mattemade.blackcat.physics.ContactBits
import io.itch.mattemade.blackcat.physics.Platform
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
    private val controller: InputMapController<GameInput>,
    private val onSignal: (String) -> Unit,
) : Disposing by Self(), HasContext<Body> {

    private val size = Vec2(444f * 16f / 1920f, 366f * 9f / 1080f) // 3.7 x 3.05
    private val hx = size.x / 2f
    private val hy = size.y / 2f
    private val physicalSize = Vec2(size.x * 0.4f, size.y * 0.5f)
    private val textureOffset = Vec2(physicalSize.x - size.x, (physicalSize.y - size.y) / 2f + 0.15f)
    private val physicalHw = physicalSize.x / 2f
    private val physicalHh = physicalSize.y / 2f
    private val tempVec2 = Vec2()
    private val lastStablePositions = Array(5) { Vec2() }
    private var currentStablePosition = 0
    private var shouldRespawnOnNextUpdate = false
    private var jumpingTimeLeft = 0f

    private val body = world.createBody(
        BodyDef(
            type = BodyType.DYNAMIC,
            userData = this,
        ).apply {
            position.set(initialPosition)
        }
    )

    var platformInContact: Platform? = null
    var platformToFallThrough: Platform? = null
    var climbingWall: Vec2? = null
    //val laddersInContact = mutableSetOf<Ladder>()

    val x get() = body.position.x
    val y get() = body.position.y
    val directionX get() = x + body.linearVelocityX / 10f
    val directionY
        get() =
            y + /*if (state == State.BACK_CLIMBING || state == State.WALL_CLIMBING) {
                body.linearVelocityY.sign * 1000f
            } else */if (state == State.CROUCHING || state == State.CROUCH_IDLE || state == State.CRAWLING) {
                3f
            } else {
                body.linearVelocityY / 10f
            }
    val top get() = body.position.y - physicalHh
    val bottom get() = body.position.y + physicalHh
    private val tempRect = Rect()
    val physicalRect: Rect get() = tempRect.set(x - physicalHw, y - physicalHw, physicalSize.x, physicalSize.y)

    private val fixture = body.createFixture(
        FixtureDef(
            shape = PolygonShape().apply {
                setAsBox(physicalHw, physicalHh)
            },
            filter = Filter().apply {
                categoryBits = ContactBits.CAT_BIT
                maskBits = ContactBits.BLOCK_BIT or ContactBits.PLATFORM_BIT or ContactBits.WALL_BIT
            },
            friction = 2f,
            userData = this
        )
    ) ?: error("Cat fixture is null! Should not happen!")

    override val context: Map<Any, Body> = mapOf(Body::class to body)
    private var currentAnimation: SignallingAnimationPlayer = animations.idle
        set(value) {
            value.restart()
            field = value
        }
    var facingLeft = true
        private set
    var state: State = State.IDLE
        private set(value) {
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
                    State.BACK_CLIMBING -> animations.climbback
                    State.WALL_CLIMBING -> animations.climbwall
                }
                field = value
            }
        }
    private var dashAvailable: Boolean = true

    fun update(dt: Duration, isNearLadder: Boolean, isOnLadder: Boolean, isDanger: Boolean) {
        val fakeLanding = shouldRespawnOnNextUpdate
        if (shouldRespawnOnNextUpdate) {
            shouldRespawnOnNextUpdate = false
            respawn()
        }

        val xMovement = controller.axis(GameInput.HORIZONTAL)
        val yMovement = controller.axis(GameInput.VERTICAL)
        val climbingArea = climbingWall

        if (ignoringWallContactFor > Duration.ZERO) {
            ignoringWallContactFor -= dt
        }

        if (isOnLadder/*laddersInContact.isNotEmpty()*/ && !controller.down(GameInput.JUMP)) {
            if (state == State.JUMPING || state == State.FALLING || state == State.FREEFALLING || state == State.WALL_CLIMBING || state == State.WALKING || state == State.IDLE || state == State.STANDING) {
                if (yMovement < 0 || (state == State.WALL_CLIMBING && (facingLeft && xMovement > 0f || !facingLeft && xMovement < 0f))) {
                    state = State.BACK_CLIMBING
                    body.linearVelocity.set(0f, 0f)
                    body.gravityScale = 0f
                    platformInContact = null
                    platformToFallThrough = null
                    //body.type = BodyType.STATIC
                }
            }
        }

        if (state != State.BACK_CLIMBING && climbingArea != null && ignoringWallContactFor <= Duration.ZERO && ((facingLeft && xMovement < 0f || !facingLeft && xMovement > 0f) || state == State.WALL_CLIMBING)) {
            val y = y
            if (top > climbingArea.y || bottom < climbingArea.x) {
                climbingWall = null
                body.gravityScale = 1f
                dashAvailable = true
                state = State.FREEFALLING
                body.applyLinearImpulse(tempVec2.set(if (facingLeft) -6f else 6f, 8f), body.worldCenter, true)
                // body.type = BodyType.DYNAMIC
            } else if (state != State.WALL_CLIMBING) {
                state = State.WALL_CLIMBING
                body.linearVelocity.set(0f, 0f)
                body.gravityScale = 0f
                //body.type = BodyType.STATIC
            }
        } else if (!isNearLadder) {
            climbingWall = null
            body.gravityScale = 1f
            if (state == State.BACK_CLIMBING) {
                state = State.FREEFALLING
            }
            // body.type = BodyType.DYNAMIC
        }


        if (state == State.BACK_CLIMBING || state == State.WALL_CLIMBING/*body.type == BodyType.STATIC*/) {
            if (state == State.BACK_CLIMBING) {
                if (climbingArea != null) {
                    // TODO; if go against the wall - set the state to wall climbing and update wall climbing
                    // updateClimbingWall(dt, xMovement, yMovement, climbingArea)
                }
                updateClimbing(dt, xMovement, yMovement)
            } else if (climbingArea != null) {
                updateClimbingWall(dt, xMovement, yMovement, climbingArea)
            }
        } else {
            updatePlatforming(dt, xMovement, yMovement, fakeLanding, isDanger)
        }
    }

    private var ignoringWallContactFor: Duration = 0f.seconds

    private fun updateClimbingWall(dt: Duration, xMovement: Float, yMovement: Float, climbingArea: Vec2) {
        var timeMultiplier = 1.0
        if (controller.pressed(GameInput.JUMP) /*|| (facingLeft && xMovement > 0f || !facingLeft && xMovement < 0f)*/) {
            climbingWall = null
            body.gravityScale = 1f
            if (yMovement <= 0f) {
                state = State.JUMPING
                jumpingTimeLeft = 0.2f
            } else {
                state = State.FREEFALLING
            }
            dashAvailable = true
            ignoringWallContactFor = 0.1f.seconds
        } else {
            val seconds = dt.seconds
            timeMultiplier = (tempVec2.set(0f, yMovement).length() * 0.6f).toDouble()
            body.linearVelocity.set(tempVec2.mulLocal(seconds * 400f))

        }
        body.isAwake = true
        currentAnimation.update(dt * timeMultiplier)
    }

    private fun updateClimbing(dt: Duration, xMovement: Float, yMovement: Float) {
        var timeMultiplier = 1.0
        if (controller.pressed(GameInput.JUMP)) {
            body.gravityScale = 1f
            if (yMovement <= 0f) {
                state = State.JUMPING
                jumpingTimeLeft = 0.2f
            } else {
                state = State.FREEFALLING
            }
            dashAvailable = true
        } else {
            val seconds = dt.seconds
            timeMultiplier = (tempVec2.set(xMovement, yMovement).length() * 0.6f).toDouble()
            body.linearVelocity.set(tempVec2.mulLocal(seconds * 400f))
        }
        body.isAwake = true
        if (xMovement != 0f) {
            facingLeft = xMovement < 0f
        }
        currentAnimation.update(dt * timeMultiplier)
    }

    private fun updatePlatforming(dt: Duration, xMovement: Float, yMovement: Float, fakeLanding: Boolean, isDanger: Boolean) {
        var timeMultiplier = 1.0

        platformInContact?.let { platform ->
            if (bottom > platform.middleY) {
                platformInContact = null
            }
        }

        platformToFallThrough?.let { platform ->
            if (bottom > platform.middleY) {
                platformToFallThrough = null
            }
        }


        if (controller.pressed(GameInput.JUMP)) {
            if (state == State.WALKING || state == State.IDLE || state == State.LANDING) {
                //body.applyLinearImpulse(tempVec2.set(0f, -16f), body.position, true)
                body.linearVelocityY = -16f
                state = State.JUMPING
                jumpingTimeLeft = 0.2f
                dashAvailable = true
                platformInContact = null
                platformToFallThrough = null
            } else if (state == State.CROUCHING || state == State.CROUCH_IDLE || state == State.CRAWLING) {
                // if standing on the branch
                // temporarily disable the branch contact points
                platformInContact?.let {
                    platformToFallThrough = platformInContact
                    body.applyLinearImpulse(tempVec2.set(0f, 6f), body.position, true)
                    state = State.FREEFALLING
                }
            } else if (state == State.JUMPING || state == State.FALLING || state == State.FREEFALLING) {
                // DASH
                if (dashAvailable) {
                    dashAvailable = false
                    state = State.FREEFALLING
                    body.applyLinearImpulse(
                        tempVec2.set(if (xMovement < 0 || facingLeft) -12f else 12f, 0f),
                        body.position,
                        true
                    )
                    onSignal("dash")
                }
            }
        } else if (controller.down(GameInput.JUMP) && state == State.JUMPING) {
            jumpingTimeLeft -= dt.seconds
            if (jumpingTimeLeft > 0) {
                body.linearVelocityY = -12f
            }
            // continue jumping if you can
        }


        if (controller.pressed(GameInput.ATTACK)) {
            if (state == State.JUMPING || state == State.FALLING || state == State.FREEFALLING) {
                state = State.FREEFALLING
                body.applyLinearImpulse(
                    tempVec2.set(if (xMovement < 0 || facingLeft) -12f else 12f, 0f),
                    body.position,
                    true
                )
            } else {
                // TODO: do we need an attack?
                state = State.ATTACKING
            }
            onSignal("dash")
        }

        if (state == State.ATTACKING) {
            // TODO do nothing?
        } else if (body.linearVelocityY < 0) {
            if (state != State.FREEFALLING && state != State.FALLING) {
                state = State.JUMPING
            }
        } else if (body.linearVelocityY > 0) {
            if (state == State.JUMPING) {
                state = State.FALLING
            } else if (state != State.FALLING && state != State.FREEFALLING) {
                state = State.FREEFALLING
            }
        } else if (body.linearVelocityX != 0f) {
            if (state == State.FALLING || state == State.FREEFALLING) {
                onSignal("land")
            }
            if (yMovement > 0) {
                state = State.CRAWLING
            } else {
                state = State.WALKING
            }
        } else { // vertical and horizontal velocity 0
            if (state == State.FALLING || state == State.FREEFALLING) {
                if (fakeLanding) {
                    state = State.IDLE
                } else {
                    state = State.LANDING
                }
            } else if (yMovement > 0) {
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
            val speedlimit =
                if (state == State.CRAWLING || state == State.CROUCHING || state == State.CROUCH_IDLE) 5f else 10f
            if (currentSpeed < speedlimit) {
                body.applyForceToCenter(tempVec2.set(xMovement * dt.seconds * 4000f, 0f))
            }
            facingLeft = xMovement < 0f
            if (state == State.WALKING || state == State.CRAWLING) {
                timeMultiplier = currentSpeed / 7.0
            }
        }

        if (!isDanger && (state == State.IDLE || state == State.WALKING)) {
            lastStablePositions[currentStablePosition].set(x, y)
            currentStablePosition = (currentStablePosition + 1) % lastStablePositions.size
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

    private val debugColor = Color.BLUE.toFloatBits()

    fun render(batch: Batch) {
        val x = body.position.x - hx// - textureOffset.x
        val y = body.position.y - hy + textureOffset.y
        //batch.draw(Textures.white, x, y, width = size.x, height = size.y)
        currentAnimation.currentKeyFrame?.let { frame ->
            batch.draw(frame, x, y, width = size.x, height = size.y, flipX = facingLeft)
        }
    }

    fun requestRespawn() {
        shouldRespawnOnNextUpdate = true
        onSignal("death")
    }

    private fun respawn() {
        state = State.IDLE
        climbingWall = null
        platformInContact = null
        platformToFallThrough = null
        body.linearVelocity.set(0f, 0f)
        currentStablePosition =
            (currentStablePosition + 1) % lastStablePositions.size // cycle to the last recently updated position
        //body.transform.set(lastStablePositions[currentStablePosition], Angle.ZERO)
        body.setTransform(lastStablePositions[currentStablePosition], Angle.ZERO)
        body.isAwake = true
    }

    enum class State {
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
        BACK_CLIMBING,
        WALL_CLIMBING,
    }

}