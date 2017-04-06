package actors

import akka.actor.Actor
import Datatypes._

class Replica extends Actor{
  
  val map = scala.collection.mutable.HashMap.empty[String,(Entry, Tag, String)]

  
  def receive = {
    case ReadTag(nonce: Int, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sender ! ReadTagResult(tuple._2, tuple._3, nonce)
      else
        sender ! ReadTagResult(null, null, nonce)
    }
    
    case Write(tag: Tag, v: Any, sig: String, nonce: Int, key: String) => {
      
    }
    
    case Read(nonce: Int, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sender ! ReadResult(tuple._2, tuple._1, tuple._3, nonce)
      else
        sender ! ReadResult(null, null, null, nonce)
    }
  }
  
}