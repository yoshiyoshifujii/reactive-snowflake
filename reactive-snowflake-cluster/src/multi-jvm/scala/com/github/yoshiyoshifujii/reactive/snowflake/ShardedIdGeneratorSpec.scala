package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join, Subscribe, Unsubscribe}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.TestDuration
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object ShardedIdGeneratorConfig extends MultiNodeConfig {
  val worker1_1: RoleName = role("worker-1-1")
  val worker1_2: RoleName = role("worker-1-2")
  val worker2_1: RoleName = role("worker-2-1")
  val worker2_2: RoleName = role("worker-2-2")

  commonConfig(
    ConfigFactory
      .parseString(s"""
         |akka.actor.provider = cluster
         |akka {
         |  actor {
         |    serializers {
         |      jackson-cbor = "akka.serialization.jackson.JacksonCborSerializer"
         |    }
         |    serialization-bindings {
         |      "${classOf[IdWorker.Command].getName}" = jackson-cbor
         |    }
         |  }
         |}
         |akka.remote.artery.canonical.port = 0
         |akka.cluster.roles = [compute]
         |""".stripMargin).withFallback(ConfigFactory.load())
  )
}

class ShardedIdGeneratorSpecMultiJvmWorker1_1 extends ShardedIdGeneratorSpec
class ShardedIdGeneratorSpecMultiJvmWorker1_2 extends ShardedIdGeneratorSpec
class ShardedIdGeneratorSpecMultiJvmWorker2_1 extends ShardedIdGeneratorSpec
class ShardedIdGeneratorSpecMultiJvmWorker2_2 extends ShardedIdGeneratorSpec

class ShardedIdGeneratorSpec extends MultiNodeSpec(ShardedIdGeneratorConfig) with MultiNodeSpecHelper {
  import ShardedIdGeneratorConfig._

  override protected def beforeAll(): Unit       = multiNodeSpecBeforeAll()
  override protected def afterAll(): Unit        = multiNodeSpecAfterAll()
  override def initialParticipants: Int          = roles.size
  implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
  private val dilated: FiniteDuration            = 15.seconds.dilated

  "id cluster" must {

    "join cluster" in within(dilated) {
      val cluster           = Cluster(typedSystem)
      val worker1_1_Address = node(worker1_1).address
      val worker1_2_Address = node(worker1_2).address
      val worker2_1_Address = node(worker2_1).address
      val worker2_2_Address = node(worker2_2).address

      val probe = TestProbe[ClusterEvent.MemberEvent]
      cluster.subscriptions ! Subscribe(probe.ref, classOf[ClusterEvent.MemberUp])

      cluster.manager ! Join(worker1_1_Address)

      assert {
        probe
          .receiveMessages(4)
          .collect { case ClusterEvent.MemberUp(m) =>
            m.address
          }.toSet === Set(worker1_1_Address, worker1_2_Address, worker2_1_Address, worker2_2_Address)
      }

      cluster.subscriptions ! Unsubscribe(probe.ref)

      enterBarrier("all-up")
    }

    "init Cluster Sharding" in within(dilated) {
      val clusterSharding = ClusterSharding(typedSystem)
      ShardedIdGenerator.initClusterSharding(clusterSharding, IdRouter.behavior, dilated)

      enterBarrier("all-sharding-up")
    }

    "generate id" in within(dilated) {
      runOn(worker1_1) {
        val clusterSharding = ClusterSharding(typedSystem)
        val datacenterMask = 0x00000000003E0000L
        val datacenterId = IdWorker.DatacenterId(0x1F)
        val idGenerator = system.spawn(ShardedIdGenerator.ofProxy(clusterSharding), ShardedIdGenerator.name)
        val idClient = system.spawn(IdClient.behavior(datacenterId, idGenerator), IdClient.name)
        val probe = TestProbe[IdWorker.IdGenerated]
        (0 until 100).foreach { _ =>
          idClient ! IdClient.GenerateId(probe.ref)
        }
        probe.receiveMessages(100).foreach {
          case IdWorker.IdGenerated(id) =>
            assert(((id & datacenterMask) >> 17) === datacenterId.value)
        }
      }
    }
  }
}
