package Datatypes

import akka.actor.{ActorRef, ActorPath}
import scala.collection.mutable.Set

class Request (requester: ActorRef, reqType: String) {
  val sender = requester
  var rType = reqType
  var quorum = Set.empty[(ActorPath, Any)]
}