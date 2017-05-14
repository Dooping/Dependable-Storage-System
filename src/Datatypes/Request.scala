package Datatypes

import akka.actor.{ActorRef, ActorPath, Cancellable}
import scala.collection.mutable.Set

class Request (requester: ActorRef, reqType: String, clientId: String) {
  val sender = requester
  var rType = reqType
  var quorum = Set.empty[Any]
  var ackQuorum = Set.empty[Any]
  var max:Any = null
  var id = clientId
}