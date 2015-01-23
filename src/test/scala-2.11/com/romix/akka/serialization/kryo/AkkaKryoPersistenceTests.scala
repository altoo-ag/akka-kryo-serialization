package com.romix.akka.serialization.kryo

import java.io.File

import akka.actor._
import akka.persistence._
import akka.serialization.SerializationExtension
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest._

import scala.collection.immutable.HashMap
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by blingannagari on 22/01/15.
 */

case object TakeSnapshot
case object GetState
case object Boom
case class KeyValue(key: String, value: Any)
case object RecoveryCompleted
case object SnapshotSaved

object SnapshotRecoveryLocalStoreSpec {
  final case class ExampleState(received: List[String] = Nil) {
    def updated(s: String): ExampleState = copy(s :: received)
    override def toString = received.reverse.toString
  }
  class SnapshotTestPersistentActor(name: String, probe: ActorRef) extends PersistentActor {
    def persistenceId: String = name

    var state = ExampleState()

    def receiveCommand: Receive = {
      case TakeSnapshot                          => saveSnapshot(state)
      case SaveSnapshotSuccess(metadata)         => probe ! "save success"
      case SaveSnapshotFailure(metadata, reason) => {
        println("Saving snapshot failed: " + reason.getMessage)
        probe ! "save failure"
      }
      case s: String =>
        persist(s) { evt => state = state.updated(evt) }
      case GetState => sender() ! state.received.reverse
      case Boom => throw new Exception("Intentionally throwing exception to test persistence by restarting the actor")
    }

    def receiveRecover: Receive = {
      case SnapshotOffer(_, s: ExampleState) =>
        println("offered state = " + s)
        state = s
        sender() ! RecoveryCompleted
      case evt: String =>
        state = state.updated(evt)
    }
  }
}

class SnapshotRecoveryTest extends PersistenceSpec with ImplicitSender {
  
  import com.romix.akka.serialization.kryo.SnapshotRecoveryLocalStoreSpec._
  "A persistent actor which is persisted" should {
    val persistentActor = system.actorOf(Props(classOf[SnapshotTestPersistentActor], "PersistentActor", testActor))
    
    "should get right serializer" in {
      val serialization = SerializationExtension(system) 
      val sample = List("a", "b", "c")
      assert(serialization.findSerializerFor(sample).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(sample)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[List[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == sample)
    }
    
    "recover state only from its own correct snapshot file after restart" in {
      persistentActor ! "a"
      expectNoMsg()
      persistentActor ! "b"
      expectNoMsg()
      persistentActor ! TakeSnapshot
      expectMsg("save success")
      persistentActor ! "c"
      expectNoMsg()
      persistentActor ! Boom
      persistentActor ! GetState
      expectMsg(List("a", "b", "c"))
    }

    "recover correct state after explicitly killing the actor and starting it again" in {
      persistentActor ! Kill   //default supervision stops the actor on ActorKilledException
      
      val newPersistentActor = system.actorOf(Props(classOf[SnapshotTestPersistentActor], "PersistentActor", testActor))
      within(3 seconds) {
        newPersistentActor ! GetState
        expectMsg(List("a", "b", "c"))
      }
    }
  }
}



abstract class PersistenceSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(TestConfig.config)))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {val storageLocations = List("akka.persistence.snapshot-store.local.dir").map(s => new File(system.settings.config.getString(s)))

  override def beforeAll() {
    storageLocations.foreach(FileUtils.deleteDirectory)
    super.beforeAll()
  }

//  override def afterAll() {
//    storageLocations.foreach(FileUtils.deleteDirectory)
//    super.afterAll()
//  }
}

object TestConfig {
  val config = """
    akka {
    extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
    actor {
      kryo {
        type = "nograph"
        idstrategy = "incremental"
        kryo-reference-map = false
        buffer-size = 65536
        pre-serialization-transformations = "lz4,aes"
        encryption {
          aes {
            mode = "AES/CBC/PKCS5Padding"
            key = j68KkRjq21ykRGAQ
          }
        }
        implicit-registration-logging = true
        mappings {
          "scala.collection.immutable.$colon$colon" = 48
          "scala.collection.immutable.List" = 49
        }
      }

      serializers {
        kryo = "com.romix.akka.serialization.kryo.KryoSerializer"
      }

      serialization-bindings {
        "scala.collection.immutable.List" = kryo
      }
    }

    persistence {
      journal.plugin = "in-memory-journal"
      snapshot-store.local.dir = "target/test-snapshots"
    }
  }
  """
}