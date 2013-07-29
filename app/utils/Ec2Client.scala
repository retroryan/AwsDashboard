package utils

import scala.xml.Elem
import scala.concurrent.Future
import java.util.{TimeZone, Date}
import java.text.SimpleDateFormat
import java.net.URLEncoder
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Base64
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._
import java.util
import scala.collection.JavaConversions

/** Ec2Client from a gist by James Roper -  https://gist.github.com/jroper/5041486
  *
  */
class Ec2Client(accessKeyId: String, accessKey: String, region: String) {

  val version = "2012-12-01"
  val endpoint = "ec2." + region + ".amazonaws.com"
  val path = "/"
  val method = "POST"

  def provisionInstanceJava(imageId: String, size: String, number: Int, userData: String): Future[util.List[String]] = {
    val futureClient = provisionInstance(imageId, size, number, userData)

    futureClient onFailure  {
      case error  => println("An error has occured: " + error.getMessage)
    }

    futureClient map {
      JavaConversions.seqAsJavaList
    }
  }

  def provisionInstance(imageId: String, size: String, number: Int, userData: String): Future[Seq[String]] = {
    new Ec2Request("RunInstances", Map(
      "ImageId" -> imageId,
      "MinCount" -> number.toString,
      "MaxCount" -> number.toString,
      "UserData" -> new String(Base64.encodeBase64(userData.getBytes("UTF-8"))),
      "InstanceType" -> size,
      "InstanceInitiatedShutdownBehavior" -> "terminate"
    )).execute().map { xml =>
      (xml \\ "instanceId").toSeq.map(_.text)
    }
  }

  def terminateInstance(instanceId: String) = {
    new Ec2Request("TerminateInstances", Map(
      "InstanceId.1" -> instanceId
    )).execute()
  }

  private [utils] def signParams(method: String, endpoint: String, path: String, params: Map[String, String]) = {
    // Format query params
    val formattedQueryParams = params.map(param => param._1 + "=" + URLEncoder.encode(param._2, "UTF-8")).toList.sorted.mkString("&")

    // Format the canonical query string
    val canonical = s"${method}\n${endpoint}\n${path}\n${formattedQueryParams}"

    // Sign it
    val signingKey = new SecretKeySpec(accessKey.getBytes("UTF-8"), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)
    val rawHmac = mac.doFinal(canonical.getBytes("UTF-8"))
    new String(Base64.encodeBase64(rawHmac))
  }

  private class Ec2Request(action: String, params: Map[String, String], method: String = this.method, endpoint: String = this.endpoint, path: String = this.path) {
    def execute(): Future[Elem] = {
      val url = WS.url("https://" + endpoint + path).withQueryString(calculateQueryParams.toSeq:_*)
      println("calling url " + url)
      (method match {
        case "GET" => url.get()
        case "POST" => url.post("")
      }).map { resp =>
        println("resp " + resp)
        if (resp.status < 300) {
          resp.xml
        } else {
          println("Error calling endpoint " + resp.status + " " + resp.statusText + ": " + resp.body)
          <error>{resp.status}: {resp.statusText}</error>
        }
      } recover {
        case err => {
          println("err: " + err.getMessage)
          <error>{err.getMessage}</error>
        }
      }
    }

    def calculateQueryParams: Map[String, String] = {
      val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
      format.setTimeZone(TimeZone.getTimeZone("GMT"))
      val timestamp = format.format(new Date())

      // Build up query params
      val queryParams = params +
        ("Action" -> action) +
        ("Version" -> version) +
        ("AWSAccessKeyId" -> accessKeyId) +
        ("Timestamp" -> timestamp) +
        ("SignatureVersion" -> "2") +
        ("SignatureMethod" -> "HmacSHA256")

      queryParams + ("Signature" -> signParams(method, endpoint, path, queryParams))
    }

  }
}