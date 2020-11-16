package io.altoo.akka.serialization.kryo

import java.io.File

import akka.actor._
import akka.persistence._
import akka.serialization.SerializationExtension
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps


case object TakeSnapshot
case object GetState
case object Boom
case object SnapshotSaveSuccess
case object SnapshotSaveFail

case class Person(fName: String, lName: String)
case class ExampleState(received: List[Person] = Nil) {
  def updated(s: Person): ExampleState = copy(s :: received)
  override def toString: String = received.reverse.toString
}

object SnapshotRecoveryLocalStoreSpec {
  class SnapshotTestPersistentActor(name: String, probe: ActorRef) extends PersistentActor {
    def persistenceId: String = name

    private var state = ExampleState()

    def receiveCommand: Receive = {
      case TakeSnapshot => saveSnapshot(state)
      case SaveSnapshotSuccess(_) => probe ! SnapshotSaveSuccess
      case SaveSnapshotFailure(_, _) => probe ! SnapshotSaveFail
      case s: Person => persist(s) { evt => state = state.updated(evt) }
      case GetState => sender() ! state.received.reverse
      case Boom => throw new Exception("Intentionally throwing exception to test persistence by restarting the actor")
    }

    def receiveRecover: Receive = {
      case SnapshotOffer(_, s: ExampleState) => state = s
      case evt: Person => state = state.updated(evt)
    }
  }
}

class SnapshotRecoveryTest extends PersistenceSpec with ImplicitSender {
  import io.altoo.akka.serialization.kryo.SnapshotRecoveryLocalStoreSpec._

  private val persistentActor = system.actorOf(Props(classOf[SnapshotTestPersistentActor], "PersistentActor", testActor))

  "A persistent actor which is persisted" should {

    "get right serializer" in {
      val serialization = SerializationExtension(system)
      val sample = List(Person("John", "Doe"), Person("Bruce", "Wayne"), Person("Tony", "Stark"))
      val sampleHead: Person = sample.head
      assert(serialization.findSerializerFor(sample).getClass == classOf[KryoSerializer])
      assert(serialization.findSerializerFor(sampleHead).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(sample)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[List[Person]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == sample)
    }

    "recover state only from its own correct snapshot file after restart" in {
      persistentActor ! Person("John", "Doe")
      expectNoMessage()
      persistentActor ! Person("Bruce", "Wayne")
      expectNoMessage()
      persistentActor ! TakeSnapshot
      expectMsg(SnapshotSaveSuccess)
      persistentActor ! Person("Tony", "Stark")
      expectNoMessage()
      persistentActor ! Boom
      persistentActor ! GetState
      expectMsg(List(Person("John", "Doe"), Person("Bruce", "Wayne"), Person("Tony", "Stark")))
    }

    "recover correct state after explicitly killing the actor and starting it again" in {
      persistentActor ! Kill //default supervision stops the actor on ActorKilledException

      val newPersistentActor = system.actorOf(Props(classOf[SnapshotTestPersistentActor], "PersistentActor", testActor))
      within(3 seconds) {
        newPersistentActor ! GetState
        expectMsg(List(Person("John", "Doe"), Person("Bruce", "Wayne"), Person("Tony", "Stark")))
      }
    }
  }
}


abstract class PersistenceSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(TestConfig.config))) with AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  private val storageLocations = List("akka.persistence.snapshot-store.local.dir").map(s => new File(system.settings.config.getString(s)))

  override def beforeAll(): Unit = {
    storageLocations.foreach(FileUtils.deleteDirectory)
    super.beforeAll()
  }

  //  override def afterAll() {
  //    storageLocations.foreach(FileUtils.deleteDirectory)
  //    super.afterAll()
  //  }
}

object TestConfig {
  val config =
    """
  akka {
    actor {
      serializers {
        kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      }

      serialization-bindings {
          "scala.collection.immutable.$colon$colon" = kryo
          "scala.collection.immutable.List" = kryo
          "io.altoo.akka.serialization.kryo.Person" = kryo
          "akka.persistence.serialization.Snapshot" = kryo
          "akka.persistence.SnapshotMetadata" = kryo
      }
    }

    persistence {
      journal.plugin = "akka.persistence.journal.inmem"
      snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      snapshot-store.local.dir = "target/test-snapshots"
    }
  }
  akka-kryo-serialization {
    type = "nograph"
    id-strategy = "incremental"
    kryo-reference-map = false
    buffer-size = 65536
    post-serialization-transformations = "lz4,aes"
    encryption {
      aes {
        key-provider = "io.altoo.akka.serialization.kryo.DefaultKeyProvider"
        mode = "AES/GCM/PKCS5Padding"
        iv-length = 12
        password = "j68KkRjq21ykRGAQ"
        salt = "pepper"
      }
    }
    implicit-registration-logging = true
    mappings {
      "scala.collection.immutable.$colon$colon" = 48
      "scala.collection.immutable.List" = 49
      "io.altoo.akka.serialization.kryo.Person" = 56
      "akka.persistence.serialization.Snapshot" = 108
      "akka.persistence.SnapshotMetadata" = 113
    }
  }
  """
}
