package actors

import akka.actor.{Actor, ActorRef, Cancellable}
import scala.collection.mutable.{HashMap , Set}
import Datatypes._
import scala.concurrent.duration._
import scala.util.Random

class FaultDetection(threshold: Int) extends Actor{
  
  class SyncReplica(replica: ActorRef) extends Runnable {
    def run = {
      if (!replicas.isEmpty){
        val rep = replicas.toVector(rnd.nextInt(replicas.size))
        //println(replica.path + " sync " + rep.path)
        replica ! SyncRequest(rep)
      }
    }
  }
  
  var replicas = Set.empty[ActorRef]
  var sentinent = Set.empty[ActorRef]
  var proxys = Set.empty[ActorRef]
  
  var votes = HashMap.empty[ActorRef,Int].withDefaultValue(0)
  var sentinentSync = HashMap.empty[ActorRef, Cancellable]
  val rnd=new Random
  
  def receive = {
    case RegisterReplica() => {
      println(sender.path + " registered as replica!")
      replicas += sender
      votes += (sender -> 0) //in case the replica comes back up after too many votes
      proxys.foreach(p => p ! NewReplicaList(replicas.toList))
    }
    case RegisterSentinent() => {
      println(sender.path + " registered as sentinent!")
      val cancellable = context.system.scheduler.schedule(1 seconds, 2 seconds, new SyncReplica(sender))(context.system.dispatcher)
      sentinentSync += (sender -> cancellable)
      sentinent += sender
    }
    case RegisterProxy() => {
      println(sender.path + " registered as proxy!")
      proxys += sender
      sender ! NewReplicaList(replicas.toList)
    }
    case Vote(replica) => {
      if(replicas.contains(replica)){
        println(sender.path + " voted for replica: " + replica.path)
        votes+=(replica -> (votes(replica)+1))
        if(votes(replica) == threshold){
          replicas -= replica
          if(!sentinent.isEmpty){
            val rep = sentinent.toVector(rnd.nextInt(sentinent.size))
            rep ! SetActiveReplica()
            sentinentSync(rep).cancel()
            val syncRep = replicas.toVector(rnd.nextInt(replicas.size))
            rep ! SyncRequest(syncRep)
            replicas += rep
            sentinent -= rep
            proxys.foreach(p => p ! NewReplicaList(replicas.toList))
          }
          //send signal to create another sentient?
          val newSentSpawner = context.actorSelection(replicas.toVector(rnd.nextInt(replicas.size)).path.parent)
          newSentSpawner ! NewSentinent()
        }
      }
    }
    case _ => println("other message")
  }
  
  
  
}