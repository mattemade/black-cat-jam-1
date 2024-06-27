package io.itch.mattemade.blackcat.physics

import io.itch.mattemade.blackcat.cat.Cat
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.dynamics.contacts.Contact

class ContactListener: ContactListener {
    override fun beginContact(contact: Contact) {

    }

    override fun endContact(contact: Contact) {

    }

    override fun postSolve(contact: Contact, impulse: ContactImpulse) {
    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) {
        val platform = contact.getUserData<Platform>()
        val cat = contact.getUserData<Cat>()
        val ladder = contact.getUserData<Ladder>()
        if (platform != null && cat != null) {
            if (cat.platformInContact != platform) {
                val catLandsOnThisPlatform = (cat.state == Cat.State.FALLING || cat.state == Cat.State.FREEFALLING) && cat.bottom <= platform.rect.y
                if (catLandsOnThisPlatform) {
                    cat.platformInContact = platform
                }
                contact.isEnabled = catLandsOnThisPlatform
            } else if (cat.platformToFallThrough == platform) {
                contact.isEnabled = false
                if (cat.bottom >= platform.middleY) {
                    cat.platformInContact = null
                    cat.platformToFallThrough = null
                }
            }
        }
    }

    private inline fun <reified T> Contact.getUserData(): T? =
        (getFixtureA()?.userData as? T) ?: (getFixtureB()?.userData as? T)
}