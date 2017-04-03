package actors

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import messages._

class Test extends Actor {
  println("hello world!")
  val child = context.actorOf(Props[Child], "child1")
  
  child ! Message("ping")
  
  def receive = {
    case Message(msg) => println(self + " " + msg)
  }
  
  def say(msg: String) = println(msg)
}