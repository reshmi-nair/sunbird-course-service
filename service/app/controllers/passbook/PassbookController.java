package controllers.passbook;

import akka.actor.ActorRef;
import controllers.BaseController;
import controllers.passbook.validator.PassbookRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletionStage;

public class PassbookController extends BaseController {
    @Inject
    @Named("passbook-actor")
    private ActorRef passbookActor;

    private PassbookRequestValidator validator = new PassbookRequestValidator();

    public CompletionStage<Result> updatePassbook(Http.Request httpRequest) {
        return handleRequest(
                passbookActor,
                ActorOperations.UPDATE_PASSBOOK.getValue(),
                httpRequest.body().asJson(),
                (request) -> {
                    Request req = (Request) request;
                    validator.validatePassbookUpdateRequest(req);
                    return null;
                },
                getAllRequestHeaders(httpRequest),
                httpRequest);
    }
    public CompletionStage<Result> getUserPassbook(Http.Request httpRequest) {
        return handleRequest(
                passbookActor,
                ActorOperations.GET_PASSBOOK.getValue(),
                httpRequest.body().asJson(),
                (request) -> {
                    Request req = (Request) request;
                    validator.validateGetPassbookRequest(req);
                    return null;
                },
                getAllRequestHeaders(httpRequest),
                httpRequest);
    }
    public CompletionStage<Result> getPassbookByAdmin(Http.Request httpRequest) {
        return handleRequest(
                passbookActor,
                ActorOperations.GET_PASSBOOK_BY_ADMIN.getValue(),
                httpRequest.body().asJson(),
                (request) -> {
                    Request req = (Request) request;
                    validator.validateGetPassbookRequestByAdmin(req);
                    return null;
                },
                getAllRequestHeaders(httpRequest),
                httpRequest);
    }

}
