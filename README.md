# Sutra of reactive-snowflake

Writen in Akka Typed.

## Reference

- https://github.com/TanUkkii007/reactive-snowflake
- Thanks👍

## ID format

Same as Snowflake.

## Usage

```scala
val dcId1     = DatacenterId(1)
val workerId1 = WorkerId(1)

val probe    = testKit.createTestProbe[IdGenerated]()
val idWorker = testKit.spawn(IdWorker.behavior(dcId1, workerId1))
idWorker ! GenerateId(probe.ref)
```

## Cluster Support

To automatically manage workerId you can use Akka cluster shardhing.

```scala
val datacenterId = IdWorker.Datacenterid(0x01)

val clusterSharding = ClusterSharding(typedSystem)
ShardedIdGenerator.initClusterSharding(clusterSharding, IdRouter.behavior, receiveTimeout)

val idGenerator = system.spawn(ShardedIdGenerator.ofProxy(clusterSharding), ShardedIdGenerator.name)
val idClient    = system.spawn(IdClient.behavior(datacenterId, idGenerator), IdClient.name)
val probe       = TestProbe[IdWorker.IdGenerated]
idClient ! IdClient.GenerateId(probe.ref)
```

