package actors

import akka.actor.{Actor, ActorRef}
import Datatypes._
import scala.collection.mutable.HashMap
import security.Encryption
import scala.util.Random

class Replica extends Actor{
  var byzantine = false
  var chance = 0
  val r = Random
  
  val map = HashMap.empty[String,(Entry, Tag, String)].withDefaultValue(null)
  println(self.path + " created")
  val truststorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.trust-store")
  
  def receive = {
    case ReadTag(nonce: Long, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sendMessage(sender, ReadTagResult(tuple._2, tuple._3, nonce))
      else
        sendMessage(sender, ReadTagResult(null, null, nonce))
    }
    
    case Write(new_tag: Tag, v: Any, sig: String, nonce: Long, key: String) => {
      Encryption.verifySign(truststorePath, new_tag.toString().getBytes(),sig, false)
      val tuple = map(key)
      if(tuple!=null){
        val tag = tuple._2
        if(new_tag.sn > tag.sn)
          map+=(key -> (v,new_tag,sig))
        
      }
      else
        map+=(key -> (v,new_tag,sig))
      sendMessage(sender, Ack(nonce))
    }
    
    case Read(nonce: Long, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sendMessage(sender, ReadResult(tuple._2, tuple._1, tuple._3, nonce, key))
      else
        sendMessage(sender, ReadResult(null, null, null, nonce, key))
    }
    case CrashReplica => {
      println(self.path + " crashed!")
      context.stop(self)
    }
    case SetByzantine(chance: Int) => {
      byzantine = true;
      this.chance = chance 
    }
  }
  
  private def sendMessage(target:ActorRef, message: Any) = {
    if(byzantine){
        if(r.nextInt(100)>=chance)
          target ! message
    }
    else
      target ! message
  }
  
}