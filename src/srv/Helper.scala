package srv

import akka.actor.{ActorSystem, ActorRef, Props}
import com.typesafe.config.ConfigFactory
import actors._
import messages._
import akka.routing.FromConfig
import Datatypes._

class Helper {
  val spawner1 = ActorSystem("Spawner1", ConfigFactory.load.getConfig("Spawner1"));
  val spawner2 = ActorSystem("Spawner2", ConfigFactory.load.getConfig("Spawner2"));
  
  val system = ActorSystem("RemoteCreation", ConfigFactory.load.getConfig("RemoteCreation"));
  val parent = system.actorOf(Props[Parent], "parent")
  
  println(Tag(2,"1234")<Tag(3,"1234"))
  println(Tag(2,"2222")<Tag(2,"1111"))
  println(Tag(2,"1234")==Tag(2,"1234"))
}