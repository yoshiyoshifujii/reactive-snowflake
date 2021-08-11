package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }

import scala.concurrent.duration.FiniteDuration

object ShardedIdGenerator {

  val TypeKey: EntityTypeKey[IdRouter.Command] = EntityTypeKey[IdRouter.Command]("IdRouter")

  private def entityBehavior(
      childBehavior: Behavior[IdRouter.Command],
      actorName: String,
      receiveTimeout: FiniteDuration
  ): EntityContext[IdRouter.Command] => Behavior[IdRouter.Command] = { entityContext =>
    Behaviors.setup { context =>
      val childRef = context.spawn(childBehavior, actorName)
      context.setReceiveTimeout(receiveTimeout, IdRouter.Idle)
      Behaviors.receiveMessage {
        case IdRouter.Idle =>
          entityContext.shard ! ClusterSharding.Passivate(context.self)
          Behaviors.same
        case IdRouter.Stop =>
          context.log.debug("> Changed state: Stop")
          Behaviors.stopped
        case msg =>
          childRef ! msg
          Behaviors.same
      }
    }
  }

  def initClusterSharding(
      clusterSharding: ClusterSharding,
      childBehavior: Behavior[IdRouter.Command],
      receiveTimeout: FiniteDuration
  ): ActorRef[ShardingEnvelope[IdRouter.Command]] = {
    val entity = Entity(TypeKey)(
      createBehavior = entityBehavior(
        childBehavior,
        IdRouter.name,
        receiveTimeout
      )
    )

    clusterSharding.init(entity.withStopMessage(IdRouter.Stop))
  }

  def ofProxy(clusterSharding: ClusterSharding): Behavior[IdRouter.Command] =
    Behaviors.receiveMessage { msg =>
      val entityId = msg match {
        case IdRouter.GenerateId(datacenterId, workerId, _) =>
          s"${datacenterId.value % 32}-${workerId.value % 32}"
        case _ =>
          throw new UnsupportedOperationException(s"$msg")
      }
      val entityRef = clusterSharding.entityRefFor[IdRouter.Command](TypeKey, entityId)
      entityRef ! msg
      Behaviors.same
    }

  val name: String = "IdGenerator"
}
