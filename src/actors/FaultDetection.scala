package actors

import akka.actor.{Actor, ActorRef}
import scala.collection.mutable.{HashMap , Set}
import Datatypes._

class FaultDetection extends Actor{
  
  val threshold = 10
  
  var replicas = Set.empty[ActorRef]
  var sentinent = Set.empty[ActorRef]
  var proxys = Set.empty[ActorRef]
  
  var votes = HashMap.empty[ActorRef,Int].withDefaultValue(0)
  
  def receive = {
    case RegisterReplica() => replicas += sender
    case RegisterSentinent() => sentinent += sender
    case RegisterProxy() => proxys += sender
    case Vote(replica) => {
      votes+=(replica -> (votes(replica)+1))
      if(votes(replica) >= threshold){
        replicas -= replica
        if(!sentinent.isEmpty)
          replicas += sentinent.toList(0)
        proxys.foreach(p => p ! NewReplicaList(replicas.toList))
        //send signal to create another sentient?
      }
        
    }
    case _ => println("other message")
  }
  
}