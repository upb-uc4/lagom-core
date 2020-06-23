package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.impl.actor.User

case class OnUserCreate(user: User) extends UserEvent
