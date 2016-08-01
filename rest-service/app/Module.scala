import akka.actor.ActorSystem
import codecraft.codegen._
import com.google.inject.AbstractModule
import com.typesafe.config.ConfigFactory
import java.time.Clock
import octanner.platform.http._
import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

class Module extends AbstractModule {
  val config = ConfigFactory.load()
  val system = ActorSystem("cloud")
  val port = util.Random.nextInt(10000) + 8080
  val cloud = HttpCloud(system, "amqp://guest:guest@129.168.99.100:5672/", port)

  Await result (
    cloud start,
    Duration.Inf
  )

  println("Cloud created")

  override def configure() = {
    println(s"Binding services...")
    bind(classOf[CloudInterface]).toInstance(cloud)
    println(s"Bound services")
  }
}
