package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.impl.actor.User

case class OnUserUpdate(user: User) extends UserEvent
