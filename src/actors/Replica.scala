package actors

import akka.actor.Actor
import Datatypes._
import scala.collection.mutable.HashMap
import security.Encryption

class Replica extends Actor{
  
  val map = HashMap.empty[String,(Entry, Tag, String)].withDefaultValue(null)
  println(self.path + " created")
  val truststorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.trust-store")
  
  def receive = {
    case ReadTag(nonce: Long, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sender ! ReadTagResult(tuple._2, tuple._3, nonce)
      else
        sender ! ReadTagResult(null, null, nonce)
    }
    
    case Write(new_tag: Tag, v: Any, sig: String, nonce: Long, key: String) => {
      Encryption.verifySign(truststorePath, new_tag.toString().getBytes(),sig, false)
      val tuple = map(key)
      if(tuple!=null){
        val tag = tuple._2
        if(new_tag.sn > tag.sn){
          map+=(key -> (v,new_tag,sig))
          sender ! Ack(nonce)
        }
      }
      else{
        map+=(key -> (v,new_tag,sig))
        sender ! Ack(nonce)
      }
    }
    
    case Read(nonce: Long, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sender ! ReadResult(tuple._2, tuple._1, tuple._3, nonce, key)
      else
        sender ! ReadResult(null, null, null, nonce, key)
    }
  }
  
}