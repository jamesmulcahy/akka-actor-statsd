package deploymentzone.actor.integration

import deploymentzone.actor._
import org.scalatest.{WordSpecLike, Matchers}
import java.net.InetSocketAddress
import akka.testkit.{TestProbe, ImplicitSender}
import akka.io.Udp
import scala.concurrent.duration._
import akka.actor.Terminated

class StatsActorSpec
  extends TestKit("stats-actor-integration-spec")
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "StatsActor" when {
    "initialized with an empty namespace" should {
      "send the expected message" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, ""), "stats")
        val msg = Increment("dog")
        stats ! msg
        expectMsg(msg.toString)

        shutdown()
      }
    }
    "initialized with a namespace" should {
      "send the expected message" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, "name.space"), "stats-ns")
        val msg = Gauge("gauge")(340L)
        stats ! msg
        expectMsg(s"name.space.$msg")

        shutdown()
      }
      "sending the same message over and over again does not alter the message" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, "name.space"), "stats-ns-repeat")
        val msg = Increment("kittens")
        stats ! msg
        expectMsg(s"name.space.$msg")
        stats ! msg
        expectMsg(s"name.space.$msg")
      }
    }
    "initialized with a null address" should {
      "throw an exception" in new Environment {
        val inetSocketAddress: InetSocketAddress = null
        val probe = TestProbe()
        probe watch system.actorOf(StatsActor.props(inetSocketAddress), "stats-failure")
        probe expectMsgClass classOf[Terminated]

        shutdown()
      }
    }
    "initialized with a null namespace" should {
      "be permitted" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, null), "stats-null-ns")
        val msg = Timing("xyz")(40.seconds)
        stats ! msg
        expectMsg(msg.toString)

        shutdown()
      }
    }
    "sending multiple messages quickly in sequence" should {
      "transmit all the messages" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, null), "stats-mmsg")
        val msgs = Seq(Timing("xyz")(40.seconds),
                     Increment("ninjas"),
                     Decrement("pirates"),
                     Gauge("ratchet")(0xDEADBEEF))
        msgs.foreach(stats ! _)
        expectMsg(msgs.mkString("\n").stripLineEnd)

        shutdown()
      }
    }
    "multiple instances" should {
      "all deliver their messages" in new Environment {
        val stats1 = system.actorOf(StatsActor.props(address, null), "mi-stats1")
        val stats2 = system.actorOf(StatsActor.props(address, null), "mi-stats2")
        val msg1 = Increment("count")
        val msg2 = Decrement("count")
        stats1 ! msg1
        stats2 ! msg2
        expectMsgAllOf(msg1.toString, msg2.toString)
      }
    }
  }

  private class Environment {
    val listener = system.actorOf(UdpListenerActor.props(testActor))
    val address = expectMsgClass(classOf[InetSocketAddress])

    def shutdown() {
      listener ! Udp.Unbind
    }
  }

}
