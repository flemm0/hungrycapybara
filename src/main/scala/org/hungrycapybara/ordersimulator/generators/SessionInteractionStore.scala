package org.hungrycapybara.ordersimulator.generators

import java.util.UUID
import scala.collection.mutable
import scala.util.Random

final case class ActiveSession(
    sessionId: String,
    customerId: String
)

final case class SessionSnapshot(
    sessionId: String,
    customerId: String,
    restaurantId: Option[String],
    cartItemsCount: Int,
    checkoutStarted: Boolean
)

final class SessionInteractionStore private (
    private val sessions: mutable.Map[String, SessionState]
):
  def activeSessionCount: Int = synchronized {
    sessions.size
  }

  def startSession(customerId: String): ActiveSession = synchronized {
    val sessionId = UUID.randomUUID().toString
    sessions.update(sessionId, SessionState(customerId = customerId))
    ActiveSession(sessionId = sessionId, customerId = customerId)
  }

  def endRandomSession(): Option[ActiveSession] = synchronized {
    randomSessionId.flatMap { sessionId =>
      sessions.remove(sessionId).map { state =>
        ActiveSession(sessionId = sessionId, customerId = state.customerId)
      }
    }
  }

  def randomActiveSession(): Option[ActiveSession] = synchronized {
    randomSessionId.map { sessionId =>
      val state = sessions(sessionId)
      ActiveSession(sessionId = sessionId, customerId = state.customerId)
    }
  }

  def randomSnapshot(): Option[SessionSnapshot] = synchronized {
    randomSessionId.flatMap(snapshot)
  }

  def randomCheckoutReadySession(): Option[SessionSnapshot] = synchronized {
    randomSessionIdWhere { case (_, state) =>
      state.checkoutStarted && state.cartItemsCount > 0
    }.flatMap(snapshot)
  }

  def snapshot(sessionId: String): Option[SessionSnapshot] = synchronized {
    sessions.get(sessionId).map { state =>
      SessionSnapshot(
        sessionId = sessionId,
        customerId = state.customerId,
        restaurantId = state.restaurantId,
        cartItemsCount = state.cartItemsCount,
        checkoutStarted = state.checkoutStarted
      )
    }
  }

  def assignRestaurant(sessionId: String, restaurantId: String): Unit = synchronized {
    sessions.get(sessionId).foreach { state =>
      state.restaurantId = Some(restaurantId)
    }
  }

  def recordCartCreated(sessionId: String, restaurantId: String): Unit = synchronized {
    sessions.get(sessionId).foreach { state =>
      state.restaurantId = Some(restaurantId)
      state.cartItemsCount = 0
      state.checkoutStarted = false
    }
  }

  def recordCartItemDelta(sessionId: String, restaurantId: String, delta: Int): Int = synchronized {
    sessions.get(sessionId).map { state =>
      state.restaurantId = Some(restaurantId)
      state.cartItemsCount = math.max(0, state.cartItemsCount + delta)
      state.cartItemsCount
    }.getOrElse(0)
  }

  def recordCartAbandoned(sessionId: String): Unit = synchronized {
    sessions.get(sessionId).foreach { state =>
      state.cartItemsCount = 0
      state.checkoutStarted = false
    }
  }

  def tryStartCheckout(sessionId: String): Boolean = synchronized {
    sessions.get(sessionId).exists { state =>
      if state.cartItemsCount > 0 then
        state.checkoutStarted = true
        true
      else false
    }
  }

  def clearCheckout(sessionId: String): Unit = synchronized {
    sessions.get(sessionId).foreach { state =>
      state.checkoutStarted = false
    }
  }

  private def randomSessionId: Option[String] =
    Option.when(sessions.nonEmpty) {
      val sessionIds = sessions.keys.toVector
      sessionIds(Random.nextInt(sessionIds.size))
    }

  private def randomSessionIdWhere(predicate: ((String, SessionState)) => Boolean): Option[String] =
    val candidates = sessions.iterator.filter(predicate).map(_._1).toVector
    Option.when(candidates.nonEmpty) {
      candidates(Random.nextInt(candidates.size))
    }

object SessionInteractionStore:
  def empty: SessionInteractionStore =
    new SessionInteractionStore(mutable.Map.empty)

private final case class SessionState(
    customerId: String,
    var restaurantId: Option[String] = None,
    var cartItemsCount: Int = 0,
    var checkoutStarted: Boolean = false
)
