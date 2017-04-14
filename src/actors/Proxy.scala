package actors

import akka.actor.{Actor, ActorRef, Props, ActorPath}
import Datatypes._
import Datatypes.Request
import messages._
import akka.routing.FromConfig
import akka.actor.ActorSelection.toScala
import scala.collection.mutable.{Set, HashMap}

class Proxy extends Actor {
  
  val minQuorum = 5
  
  val requests = HashMap.empty[Long, Request]
  
  val router1: ActorRef = context.actorOf(FromConfig.props(Props[Replica]), "router1")
  
  val group = context.actorSelection("/user/proxy/router1/*")
  
  def receive = {
    case Read(nonce: Long, key: String) => {
      requests.put(nonce, new Request(sender, "ReadStep1"))
      group ! Read(nonce, key)
    }
    case ReadResult(tag: Tag, v: Entry, sig: String, nonce: Long, key: String) => {
        val request = requests(nonce)
        if(request.rType == "ReadStep1"){
          //validate tag
          val tuple = (sender.path, ReadResult(tag, v, sig, nonce, key))
          request.quorum += tuple
          if (request.quorum.size==minQuorum){
            request.rType = "ReadStep2" 
            val max = request.quorum.map(_.asInstanceOf[ReadResult]).toList.sortWith(_.tag > _.tag)(0)
            request.quorum = Set.empty[(ActorPath, Any)]
            group ! Write(max.tag, max.v, max.sig, nonce, key)
          }
        }
      }
    case ReadTag(nonce: Long, key: String) => {}
    case ReadTagResult(tag: Tag, sig: String, nonce: Long) => {}
    case Write(tag: Tag, v: Entry, sig: String, nonce: Long, key: String) => {}
    case WriteResult(nonce: Long) => {}
    case Ack(nonce: Long) => {
      val tuple = (sender.path, Ack(nonce))
      val request = requests(nonce)
      request.quorum += tuple
      if (request.quorum.size==minQuorum)
        request.sender ! Ack(nonce)
    }
  }
}