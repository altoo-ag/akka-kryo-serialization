package io.altoo.akka.serialization.kryo.testkit

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Inside}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class AbstractAkkaTest(config: Config = ConfigFactory.empty) extends TestKit(ActorSystem("testSystem", config)) with AnyFlatSpecLike with Matchers with Inside with BeforeAndAfterAll {

  override protected def afterAll(): Unit = shutdown(system)
}
