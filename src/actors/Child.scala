package actors

import akka.actor.Actor
import messages._

class Child extends Actor{
  
  def receive = {
    case Message(msg) => {
      println(self + ": " + msg)
      sender ! Message("pong")
    }
  }
  
}