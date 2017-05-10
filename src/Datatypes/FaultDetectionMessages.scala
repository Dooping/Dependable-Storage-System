package Datatypes

import akka.actor.ActorRef

case class RegisterReplica()
case class RegisterSentinent()
case class RegisterProxy()
case class Vote(replica: ActorRef)
case class NewReplicaList(replicas: List[ActorRef])