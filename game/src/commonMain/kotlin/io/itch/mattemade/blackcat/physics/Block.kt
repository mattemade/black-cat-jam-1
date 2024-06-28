package io.itch.mattemade.blackcat.physics

import com.lehaine.littlekt.math.Rect
import io.itch.mattemade.utils.disposing.HasContext
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

class Block(world: World, val rect: Rect, friction: Float = 0.4f, userData: String? = null, filter: Filter.() -> Unit = {
    categoryBits = ContactBits.ALL_NORMAL_BITS
    maskBits = ContactBits.ALL_NORMAL_BITS
}): HasContext<Body> {

    private val hx = rect.width / 2f
    private val hy = rect.height / 2f

    private val body = world.createBody(BodyDef(
        type = BodyType.STATIC,
    ).apply {
        position.set(rect.x + hx, rect.y + hy)
    }).apply {
        createFixture(FixtureDef(
            shape = PolygonShape().apply {
                setAsBox(hx, hy)
            },
            friction = friction,
            filter = Filter().apply(filter),
            userData = userData
        ))
    }

    override val context: Map<Any, Body> = mapOf(Body::type to body)
}