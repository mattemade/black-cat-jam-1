package io.itch.mattemade.blackcat.physics

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.math.Rect
import io.itch.mattemade.utils.disposing.HasContext
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.Filter
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

class Block(world: World, rect: Rect, friction: Float = .2f): HasContext<Body> {

    private val body = world.createBody(BodyDef(
        type = BodyType.STATIC,
    ).apply {
        position.set(rect.x, rect.y)
    }).apply {
        createFixture(FixtureDef(
            shape = PolygonShape().apply {
                setAsBox(hx = rect.width / 2f, hy = rect.height / 2f)
            },
            friction = friction
        ))
    }

    override val context: Map<Any, Body> = mapOf(Body::type to body)
}