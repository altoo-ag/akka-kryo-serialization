package io.altoo.testing

import akka.actor.ActorRef

case class SampleMessage(actorRef: ActorRef) extends Serializable
