# Sutra of reactive-snowflake

[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

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

