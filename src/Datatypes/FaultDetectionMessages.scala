package Datatypes

import akka.actor.ActorRef
import scala.collection.mutable.MutableList

case class RegisterReplica()
case class RegisterSentinent()
case class RegisterProxy()
case class Vote(replica: ActorRef)
case class NewReplicaList(replicas: List[ActorRef])
case class SetActiveReplica()
case class SyncRequest(replica: ActorRef)
case class Sync(timestamp: Long)
case class SyncResult(list: MutableList[(String,(Entry, Tag, String))])
case class NewSentinent()