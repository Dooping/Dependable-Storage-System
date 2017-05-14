package actors

import akka.actor.{Actor, Props}
import Datatypes._

class Spawner(replicas: Int, sentinent: Int, faultServerAddress: String) extends Actor {
  var replicaNumber = 1
  for( i <- 1 to replicas){
    context.actorOf(Props(new Replica(true, faultServerAddress)), "r"+replicaNumber)
    replicaNumber += 1
  }
  for( i <- 1 to sentinent){
    context.actorOf(Props(new Replica(false, faultServerAddress)), "r"+replicaNumber)
    replicaNumber += 1
  }
  
  def receive = {
    case NewSentinent() => {
      context.actorOf(Props(new Replica(false, faultServerAddress)), "r"+replicaNumber)
      replicaNumber += 1
    }
  }
  
}