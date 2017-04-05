package srv

import akka.actor.{ActorSystem, ActorRef, Props}
import com.typesafe.config.ConfigFactory
import actors._
import messages._
import akka.routing.FromConfig

class Helper {
  val spawner1 = ActorSystem("Spawner1", ConfigFactory.load.getConfig("Spawner1"));
  val spawner2 = ActorSystem("Spawner2", ConfigFactory.load.getConfig("Spawner2"));
  
  val system = ActorSystem("RemoteCreation", ConfigFactory.load.getConfig("RemoteCreation"));
  val parent = system.actorOf(Props[Parent], "parent")
}