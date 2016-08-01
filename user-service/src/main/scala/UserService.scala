package vanner.user

import akka.actor._
import codecraft.codegen._
import octanner.platform.http._
import java.security.MessageDigest
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure, Either}
import vanner.users._

case class UserService(cloud: CloudInterface) extends IUserService {
	var users = Map.empty[String, User]
  var usersEmailIndex = Map.empty[String, String]

  private val hash = MessageDigest.getInstance("SHA1")

  private def uuid = java.util.UUID.randomUUID.toString

  private def sha1(s: String) = {
    hash.digest(s.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }

  def echo(cmd: Echo): EchoReply = {
    println(s"$cmd")
    EchoReply(cmd.message)
  }

  def delete(cmd: DeleteUser): DeleteUserReply = {
    users.synchronized {
      users get cmd.id map { user =>
        usersEmailIndex.synchronized {
          usersEmailIndex -= user.emailAddress
        }
      }
      users -= cmd.id
    }

    cloud event (IUserService.event.deleted, UserDeleted(cmd.id))

    DeleteUserReply(200, None)
  }

  def get(cmd: GetUser): GetUserReply = {
    users.synchronized {
      users get cmd.id
    } map { user =>
      GetUserReply(Some(user), 200, None)
    } getOrElse {
      GetUserReply(None, 404, Some("User not found"))
    }
  }

  def list(cmd: ListUsers): ListUsersReply = {
    users synchronized {
      users values
    } slice (cmd offset, cmd.limit + cmd.offset) toList match {
      case users =>
        ListUsersReply(Some(users), 200, None)
    }
  }

  def find(cmd: FindUser): FindUserReply = {
    usersEmailIndex synchronized {
      usersEmailIndex get cmd.emailAddress flatMap { id =>
        users synchronized {
          users get id
        } map { user =>
          Some(user)
        } getOrElse {
          usersEmailIndex -= cmd.emailAddress
          None
        }
      }
    } map { user =>
      FindUserReply(Some(user), 200, None)
    } getOrElse {
      FindUserReply(None, 404, Some(s"User not found"))
    }
  }

  def post(cmd: PostUser): PostUserReply = {
    usersEmailIndex synchronized {
      usersEmailIndex get cmd.emailAddress
    } map { id =>
      PostUserReply(None, 400, Some("Email already registered"))
    } getOrElse {
      val id = uuid
      val user = User(
        id,
        true,
        cmd.emailAddress,
        cmd.firstName,
        cmd.lastName
      )

      users synchronized {
        users += (id -> user)
      }
      usersEmailIndex synchronized {
        usersEmailIndex += (user.emailAddress -> id)
      }

      cloud event (IUserService.event.created, UserCreated(id))
      PostUserReply(Some(id), 201, None)
    }
  }

  def put(cmd: PostUser): PostUserReply = {
    usersEmailIndex synchronized {
      usersEmailIndex get cmd.emailAddress
    } flatMap { id =>
      users synchronized {
        users get id map { user =>
          // Update this user.
          val updated = user.copy(
            firstName = cmd.firstName,
            lastName = cmd.lastName
          )
          users += (id -> updated)

          cloud event (IUserService.event.created, UserCreated(id))

          Some(PostUserReply(Some(id), 200, None))
        } getOrElse {
          // A bad id? Delete it.
          None
        }
      }
    } getOrElse {
      // Create a new user.
      val id = uuid
      val user = User(
        id,
        true,
        cmd.emailAddress,
        cmd.firstName,
        cmd.lastName
      )

      users synchronized {
        users += (id -> user)
      }
      usersEmailIndex synchronized {
        usersEmailIndex += (cmd.emailAddress -> id)
      }

      cloud event (IUserService.event.updated, UserUpdated(id))

      PostUserReply(Some(id), 201, None)
    }
  }

  def onError(exn: Throwable) {
    println(s"$exn")
  }
}

object Main {
  def main(argv: Array[String]) {
    val system = ActorSystem("service")

    val port = util.Random.nextInt(10000) + 8080
    val cloud = HttpCloud(system, "amqp://guest:guest@129.168.99.100:5672/", port)
    Await.result(cloud.start, Duration.Inf)
    Await.result(cloud serviceOf (UserService(cloud)), Duration.Inf)
  }
}
