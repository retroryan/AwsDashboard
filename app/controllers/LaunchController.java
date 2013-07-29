package controllers;


import models.Ec2Req;
import org.codehaus.jackson.JsonNode;
import play.Logger;
import play.Play;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.Future;
import utils.Ec2Client;
import views.html.startInstance;

import java.util.List;

public class LaunchController extends Controller {


    public static Result startInstance() {
        return ok(startInstance.render());
    }

    public static Result getDefaultEc2Req() {
        String region = Play.application().configuration().getString("ec2.region");
        String imageId = Play.application().configuration().getString("ec2.imageId");
        String size = Play.application().configuration().getString("ec2.size");
        Ec2Req ec2Req = new Ec2Req(region, imageId, size);
        return ok(Json.toJson(ec2Req));
    }

    public static Result startInstanceReq() throws Exception {
        JsonNode ec2ReqJson = request().body().asJson();
        Ec2Req ec2Req = Ec2Req.getEc2Req(ec2ReqJson);

        String accessKeyId = Play.application().configuration().getString("accessKeyId");
        String accessKey = Play.application().configuration().getString("accessKey");

        // be sure to use the right region so the endpoint is constructed properly
        // http://docs.aws.amazon.com/general/latest/gr/rande.html#ec2_region
        // i.e., ec2.us-west-2.amazonaws.com
        Ec2Client ec2Client = new Ec2Client(accessKeyId, accessKey, ec2Req.getRegion());
        Future<List<String>> seqFuture = ec2Client.provisionInstanceJava(ec2Req.getImageId(), ec2Req.getSize(), ec2Req.getNumInstances(), "");

        F.Promise<List<String>> promiseOfInt = new F.Promise<>(seqFuture);

        return async(
                promiseOfInt.map(
                        new F.Function<List<String>, Result>() {
                            public Result apply(List<String> stringList) {
                                for (String msg : stringList) {
                                    Logger.debug("new instance id = " + msg);
                                }
                                return ok(Json.toJson(stringList));
                            }
                        }
                )
        );
    }


}
