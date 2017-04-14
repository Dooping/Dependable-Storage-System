package actors

import akka.actor.Actor
import Datatypes._
import scala.collection.mutable.HashMap

class Replica extends Actor{
  
  val map = HashMap.empty[String,(Entry, Tag, String)]
  println(self.path + " created")
  
  def receive = {
    case ReadTag(nonce: Long, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sender ! ReadTagResult(tuple._2, tuple._3, nonce)
      else
        sender ! ReadTagResult(null, null, nonce)
    }
    
    case Write(new_tag: Tag, v: Any, sig: String, nonce: Long, key: String) => {
      //validate signature somehow...
      val tuple = map(key)
      if(tuple!=null){
        val tag = tuple._2
        if(new_tag.sn > tag.sn){
          map.put(key, (v,new_tag,sig))
          sender ! Ack(nonce)
        }
      }
      else{
        map.put(key, (v,new_tag,sig))
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