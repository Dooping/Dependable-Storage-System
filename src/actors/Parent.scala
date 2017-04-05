package actors

import akka.actor.{ActorRef, Props, Actor}
import actors._
import messages._
import akka.routing.FromConfig
import akka.actor.ActorSelection.toScala


class Parent extends Actor{
  
  val router1: ActorRef = context.actorOf(FromConfig.props(Props[Child]), "router1")
  
   val group = context.actorSelection("/user/parent/router1/*")
  
   group ! Message("ping")

  def receive = {
    case Message(str: String) =>
      println(sender.path + " " + str)
  }
}