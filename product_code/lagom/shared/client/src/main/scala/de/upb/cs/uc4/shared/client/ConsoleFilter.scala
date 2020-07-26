package de.upb.cs.uc4.shared.client

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply


class ConsoleFilter extends Filter[ILoggingEvent] {
  override def decide(event: ILoggingEvent): FilterReply = {
    if (event.getThrowableProxy.getClassName == classOf[CustomException].getName) {FilterReply.DENY}
  else FilterReply.NEUTRAL
  }
}