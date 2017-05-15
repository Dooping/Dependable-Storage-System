package actors

import akka.actor.{Actor, ActorRef}
import Datatypes._
import scala.collection.mutable.{HashMap, MutableList}
import security.Encryption
import scala.util.Random

class Replica(active: Boolean, faultServerAddress: String) extends Actor{
  var byzantine = false
  var sentinent = !active
  var chance = 0
  val r = Random
  var lastSync = 0L
  
  val log = MutableList.empty[(Long, String)]
  
  val map = HashMap.empty[String,(Entry, Tag, String)].withDefaultValue(null)
  println(self.path + " created")
  val truststorePath = context.system.settings.config.getString("akka.remote.netty.ssl.security.trust-store")
  val faultServer = context.actorSelection(faultServerAddress)
  
  if (sentinent) faultServer ! RegisterSentinent()
  else faultServer ! RegisterReplica()
  
  def receive = {
    case ReadTag(nonce: Long, key: String) => {
      val tuple = map(key)
      if(tuple!=null)
        sendMessage(sender, ReadTagResult(tuple._2, tuple._3, nonce))
      else
        sendMessage(sender, ReadTagResult(null, null, nonce))
    }
    
    case Write(new_tag: Tag, v: Any, sig: String, nonce: Long, key: String) => {
      if (Encryption.verifySign(truststorePath, new_tag.toString().getBytes(),sig, false)){
        val logEntry = (System.currentTimeMillis(), key)
        val tuple = map(key)
        if(tuple!=null){
          val tag = tuple._2
          if(new_tag.sn > tag.sn){
            map+=(key -> (v,new_tag,sig))
            log += logEntry
          }
        }
        else{
          map+=(key -> (v,new_tag,sig))
          log += logEntry
        }
        sendMessage(sender, Ack(nonce))
      }
    }
    
    case Write(new_tag: Tag, _, sig: String, nonce: Long, key: String) => {
      if(Encryption.verifySign(truststorePath, new_tag.toString().getBytes(),sig, false)){
        val logEntry = (System.currentTimeMillis(), key)
        val tuple = map(key)
        if(tuple!=null){
          val tag = tuple._2
          if(new_tag.sn > tag.sn){
            log += logEntry
            map+=(key -> null)
          }
          
        }
        else{
          log += logEntry
          map+=(key -> null)
        }
        sendMessage(sender, Ack(nonce))
      }
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
      println(self.path + " is now byzantine!")
      byzantine = true;
      this.chance = chance 
    }
    case SyncRequest(replica) => {
      //println(sender.path + " sync request to "+ self.path + " : " + replica.path)
      replica ! Sync(lastSync)
      lastSync = System.currentTimeMillis()
    }
    case Sync(timestamp) => {
      //println("sync "+timestamp)
      val operations = log.filter(p => p._1>=timestamp).toSet
      val syncList = MutableList.empty[(String,(Entry, Tag, String))]
      operations.foreach(o => {
        val element = (o._2,map(o._2))
        syncList += element
      })
      sender ! SyncResult(syncList)
    }
    case SyncResult(list) => {
      if (!list.isEmpty)
        println("tuplos atualizados: "+ list.map(_._2).toList)
      list.foreach(o => map += (o._1 -> o._2))
    }
    case SetActiveReplica() => {
      println(s"$self.path tornou-se replica")
      sentinent = false
    }
    case _ => println("replica recebeu mensagem diferente")
  }
  
  private def sendMessage(target:ActorRef, message: Any) = {
    if(byzantine){
        if(r.nextInt(100)>=chance)
          target ! message
        else
          println(s"$self.path : mensagem omitida")
    }
    else
      target ! message
  }
  
}