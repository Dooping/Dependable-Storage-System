package actors

import akka.actor.{Actor, ActorRef, Props, ActorPath}
import Datatypes._
import Datatypes.Request
import messages._
import akka.routing.FromConfig
import akka.actor.ActorSelection.toScala
import scala.collection.mutable.{Set, HashMap}
import security.Encryption

class Proxy extends Actor {
  
  val minQuorum = 5
  
  val requests = HashMap.empty[Long, Request]
  
  val router1: ActorRef = context.actorOf(FromConfig.props(Props[Replica]), "router1")
  
  val group = context.actorSelection("/user/proxy/router1/*")
  
  def receive = {
    case Read(nonce: Long, key: String) => {
      requests+=(nonce -> new Request(sender, "ReadStep1",null))
      group ! Read(nonce, key)
    }
    case ReadResult(tag: Tag, v: Entry, sig: String, nonce: Long, key: String) => {
        val request = requests(nonce)
        if(request.rType == "ReadStep1"){
          val keystorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.key-store")
          if (Encryption.verifySign(keystorePath, tag.toString().getBytes(),sig, true)){
            val tuple = (sender.path, ReadResult(tag, v, sig, nonce, key))
            request.quorum += tuple
            if (request.quorum.size==minQuorum){
              request.rType = "ReadStep2" 
              val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
              request.max = max
              request.quorum = Set.empty[Any]
              group ! Write(max.tag, max.v, max.sig, nonce, key)
            }
          }
          else
            println(self.path + ": verificação da assinatura falhou")
        }
      }
    case ReadTagResult(tag: Tag, sig: String, nonce: Long) => {
      println(self.path + ": recebeu ReadTagResult(tag:"+tag+",sig:"+sig+",nonce:"+nonce+")")
        val request = requests(nonce)
        if(request.rType == "WriteStep1"){
          val keystorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.key-store")
          if (Encryption.verifySign(keystorePath, tag.toString().getBytes(),sig, true)){
            val tuple = (sender.path, ReadTagResult(tag, sig, nonce))
            request.quorum += tuple
            if (request.quorum.size==minQuorum){
              request.rType = "WriteStep2"
              val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadTagResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
              request.quorum = Set.empty[Any]
              var newTag = Tag(max.tag.sn+1,request.id)
              val write = request.max.asInstanceOf[APIWrite]
              group ! Write(newTag, write.v, Encryption.Sign(keystorePath, newTag.toString().getBytes), nonce, write.key)
            }
          }
          else
            println(self.path + ": verificacao da assinatura falhou")
        }
    }
    case ReadTagResult(_, _, nonce: Long) => {
      println(self.path + ": recebeu ReadTagResult(tag:null,sig:null,nonce:"+nonce+")")
        val request = requests(nonce)
        if(request.rType == "WriteStep1"){
          val tuple = (sender.path, ReadTagResult(Tag(0,""), null, nonce))
          request.quorum += tuple
          if (request.quorum.size==minQuorum){
            println("quorum")
            request.rType = "WriteStep2"
            val max = request.quorum.map(_.asInstanceOf[(ActorRef,ReadTagResult)]).toList.sortWith(_._2.tag > _._2.tag)(0)._2
            request.quorum = Set.empty[Any]
            var newTag = Tag(max.tag.sn+1,request.id)
            val write = request.max.asInstanceOf[APIWrite]
            val keystorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.key-store")
            group ! Write(newTag, write.v, Encryption.Sign(keystorePath, newTag.toString().getBytes), nonce, write.key)
          }
        }
    }
    case APIWrite(nonce: Long, key: String, id: String, v: Entry) => {
      println(self.path + ": recebeu APIWrite nonce:"+nonce)
      val message = new Request(sender,"WriteStep1", id)
      message.max = APIWrite(nonce, key, id, v)
      requests+=(nonce -> message)
      group ! ReadTag(nonce, key)
    }
    case Ack(nonce: Long) => {
      val tuple = (sender.path, Ack(nonce))
      val request = requests(nonce)
      request.quorum += tuple
      if (request.quorum.size==minQuorum)
        if(request.rType == "ReadStep2")
          request.sender ! request.max
        else if(request.rType == "WriteStep2")
          request.sender ! nonce
    }
    case _ => println("recebeu mensagem diferente")
  }
}