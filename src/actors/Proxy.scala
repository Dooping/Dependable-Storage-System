package actors

import akka.actor.{Actor, ActorRef, Props, ActorPath}
import Datatypes._
import scala.collection.mutable.{Set, HashMap}
import scala.collection.immutable.List
import security.Encryption
import akka.routing.{Broadcast, FromConfig, RoundRobinGroup}
import scala.util.Random
import scala.collection.JavaConversions._
import scala.concurrent.duration._

class Proxy(replicasToCrash: Int, byzantineReplicas: Int, chance: Int, minQuorum: Int, faultServerAddress: String) extends Actor {
  
  class QuorumTimeout(quorum: Set[Any], replicas: List[ActorRef]) extends Runnable {
    def run = {
      val errors = replicas.filterNot(r => quorum.exists(p => p.asInstanceOf[(ActorRef, _)]._1 == r))
      errors.foreach(e => faultServer ! Vote(e))
      if(errors.size>0)
        println("Failure candidates: "+errors)
    }
  }
  
  class SumQuorumTimeout(quorum: Set[Any], replicas: List[ActorRef]) extends Runnable {
    def run = {
      val maxQuorum = quorum.groupBy(_.asInstanceOf[(ActorRef, SumAllResult)]._2.res).mapValues(_.size).maxBy(_._2)
      val errors = replicas.filterNot(r => quorum.exists(p => p.asInstanceOf[(ActorRef, SumAllResult)]._1 == r && p.asInstanceOf[(ActorRef, SumAllResult)]._2.res.compareTo(maxQuorum._1)==0))
      errors.foreach(e => faultServer ! Vote(e))
      if(errors.size>0)
        println("Failure candidates: "+errors)
    }
  }
  
  val r = Random
  var replicasCrashed = 0
  val requests = HashMap.empty[Long, Request]
  var replicaList = List.empty[ActorRef]
  val faultServer = context.actorSelection(faultServerAddress)
  var router1 = ActorRef.noSender
  val keystorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.key-store")
  var byzantineInit = false
  
  faultServer ! RegisterProxy()
  
  
  
  def receive = {
    case Read(nonce: Long, key: String) => {
      crashChance
      val request = new Request(sender, "ReadStep1",null)
      context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.quorum, replicaList.toList))(context.system.dispatcher)
      requests+=(nonce -> request)
      router1 ! Broadcast(Read(nonce, key))
    }
    case ReadResult(tag: Tag, v: Entry, sig: String, nonce: Long, key: String) => {
        val request = requests(nonce)
        if (Encryption.verifySign(keystorePath, tag.toString().getBytes(),sig, true)){
          val tuple = (sender, ReadResult(tag, v, sig, nonce, key))
          request.quorum += tuple
          if (request.quorum.size==minQuorum){
            request.rType = "ReadStep2" 
            val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
            request.max = max
            //request.quorum = Set.empty[Any]
            context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.ackQuorum, replicaList.toList))(context.system.dispatcher)
            router1 ! Broadcast(Write(max.tag, max.v, max.sig, nonce, key))
          }
        }
        else
          println(self.path + ": verificacao da assinatura falhou")
      }
    case ReadResult(_, _, _, nonce: Long, key: String) => {
        val request = requests(nonce)
        val tuple = (sender, ReadResult(Tag(0,""), null, null, nonce, key))
        request.quorum += tuple
        if (request.quorum.size==minQuorum){
          request.rType = "ReadStep2" 
          val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
          request.max = max
          //request.quorum = Set.empty[Any]
          if(max.tag.sn > 0){
            context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.ackQuorum, replicaList.toList))(context.system.dispatcher)
            router1 ! Broadcast(Write(max.tag, max.v, max.sig, nonce, key))
          }
          else
            request.sender ! max
        }
      }
    case ReadTagResult(tag: Tag, sig: String, nonce: Long) => {
        val request = requests(nonce)
        if (Encryption.verifySign(keystorePath, tag.toString().getBytes(),sig, true)){
          val tuple = (sender, ReadTagResult(tag, sig, nonce))
          request.quorum += tuple
          if (request.quorum.size==minQuorum){
            request.rType = "WriteStep2"
            val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadTagResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
            //request.quorum = Set.empty[Any]
            context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.ackQuorum, replicaList.toList))(context.system.dispatcher)
            var newTag = Tag(max.tag.sn+1,request.id)
            val write = request.max.asInstanceOf[APIWrite]
            router1 ! Broadcast(Write(newTag, write.v, Encryption.Sign(keystorePath, newTag.toString().getBytes), nonce, write.key))
          }
        }
        else
          println(self.path + ": verificacao da assinatura falhou")
    }
    case ReadTagResult(_, _, nonce: Long) => {
        val request = requests(nonce)
        val tuple = (sender, ReadTagResult(Tag(0,""), null, nonce))
        request.quorum += tuple
        if (request.quorum.size==minQuorum){
          request.rType = "WriteStep2"
          val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadTagResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
          //request.quorum = Set.empty[Any]
          context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.ackQuorum, replicaList.toList))(context.system.dispatcher)
          var newTag = Tag(max.tag.sn+1,request.id)
          val write = request.max.asInstanceOf[APIWrite]
          router1 ! Broadcast(Write(newTag, write.v, Encryption.Sign(keystorePath, newTag.toString().getBytes), nonce, write.key))
        }
    }
    case APIWrite(nonce: Long, key: String, id: String, v: Entry) => {
      crashChance
      val request = new Request(sender,"WriteStep1", id)
      request.max = APIWrite(nonce, key, id, v)
      context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.quorum, replicaList.toList))(context.system.dispatcher)
      requests+=(nonce -> request)
      router1 ! Broadcast(ReadTag(nonce, key))
    }
    case APIWrite(nonce: Long, key: String, id: String, _) => {
      crashChance
      val request = new Request(sender,"WriteStep1", id)
      request.max = APIWrite(nonce, key, id, null)
      context.system.scheduler.scheduleOnce(2 seconds, new QuorumTimeout(request.quorum, replicaList.toList))(context.system.dispatcher)
      requests+=(nonce -> request)
      router1 ! Broadcast(ReadTag(nonce, key))
    }
    case Ack(nonce: Long) => {
      val tuple = (sender, Ack(nonce))
      val request = requests(nonce)
      request.ackQuorum += tuple
      if (request.ackQuorum.size==minQuorum)
        if(request.rType == "ReadStep2")
          request.sender ! request.max
        else if(request.rType == "WriteStep2")
          request.sender ! nonce
    }
    case NewReplicaList(replicas) => {
      replicaList = replicas
      router1 = context.actorOf(RoundRobinGroup(replicas.map(_.path.toString())).props())
      if (!byzantineInit && byzantineReplicas < replicas.size){
        val byz = replicas.toList
        Random.shuffle(byz)
        var i = 0
        while (i < byzantineReplicas){
          byz(i) ! SetByzantine(chance)
          i+=1
        }
        byzantineInit = true;
      }
    }
    case SumAll(nonce, pos, encrypted, nsquare) => {
      crashChance
      val request = new Request(sender,"SumAll", sender.path.toString())
      request.max = SumAll(nonce, pos, encrypted, nsquare)
      context.system.scheduler.scheduleOnce(2 seconds, new SumQuorumTimeout(request.quorum, replicaList.toList))(context.system.dispatcher)
      requests+=(nonce -> request)
      router1 ! Broadcast(request.max)

    }
    case SumAllResult(nonce, res) => {
      //juntar falhas byzantinas com valores errados?
      val request = requests(nonce)
      val tuple = (sender, SumAllResult(nonce, res))
      request.quorum += tuple
      val maxQuorum = request.quorum.groupBy(_.asInstanceOf[(ActorRef, SumAllResult)]._2.res).mapValues(_.size).maxBy(_._2)
      if (maxQuorum._2.compareTo(minQuorum)==0)
        request.sender ! SumAllResult(nonce, maxQuorum._1)
    }
    case _ => println("recebeu mensagem diferente")
  }
  
  
  def crashChance = {
    if (replicasCrashed<replicasToCrash)
        if(r.nextInt(100)<chance){
          router1 ! CrashReplica
          replicasCrashed+=1
        }
  }
}