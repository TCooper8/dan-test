package controllers

import codecraft.codegen._
import javax.inject._
import octanner.platform.http._
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import vanner.users._

@Singleton
class UserController @Inject() (cloud: CloudInterface) extends Controller {
  import UserFormat._

  def get(id: String) = Action.async { req =>
    val cmd = GetUser(id)
    cloud.cmd[GetUser, GetUserReply](IUserService.get, cmd) map {
      case GetUserReply(Some(user), _, _) =>
        Ok(Json toJson user)
      case GetUserReply(None, code, error) =>
        Status(code)(error getOrElse "")
    }
  }

  def post = Action.async(BodyParsers.parse.json) { req =>
    req.body.validate[PostUser].fold(
      errors => Future {
        BadRequest(JsError toJson errors)
      },
      cmd => {
        (cloud.cmd[PostUser, PostUserReply](IUserService.post, cmd)) map {
          case PostUserReply(Some(id), _, _) =>
            Ok(id)

          case PostUserReply(None, code, error) =>
            Status(code)(error getOrElse "")
        }
      }
    )
  }
}

