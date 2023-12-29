package org.sunbird.actor.passbook;

import org.sunbird.actor.base.BaseActor;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.service.passbook.PassbookService;
import org.sunbird.service.passbook.PassbookServiceImpl;
public class PassbookActor extends BaseActor {
    @Override
    public void onReceive(Request request) throws Throwable {
        if (request.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_PASSBOOK.getValue())) {
            updatePassbook(request);
        } else if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_PASSBOOK.getValue())) {
            getPassbook(request);
        } else if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_PASSBOOK_BY_ADMIN.getValue())) {
            getPassbookByAdmin(request);
        } else {
            onReceiveUnsupportedOperation(request.getOperation());
        }
    }
    private void updatePassbook(Request request) {
        Response res;
        try{
            String requestedBy = (String) request.getContext().get(JsonKey.REQUESTED_BY);
            PassbookService service = new PassbookServiceImpl();
            res = service.updatePassbook(request.getRequestContext(), requestedBy, request.getRequest());
        } catch (Exception e) {
            logger.error(request.getRequestContext(), "PassbookActor:updatePassbook: Error occurred = " + e.getMessage(), e);
            ProjectCommonException exception =
                    new ProjectCommonException(
                            ResponseCode.SERVER_ERROR.getErrorCode(),
                            ResponseCode.SERVER_ERROR.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }
        sender().tell(res, self());
    }
    private void getPassbook(Request request) {
        Response res;
        try{
            String requestedBy = (String) request.getContext().get(JsonKey.REQUESTED_BY);
            PassbookService service = new PassbookServiceImpl();
            res = service.getPassbook(request.getRequestContext(), requestedBy, request.getRequest(),false);
        } catch (Exception e) {
            logger.error(request.getRequestContext(), "PassbookActor:getPassbook: Error occurred = " + e.getMessage(), e);
            ProjectCommonException exception =
                    new ProjectCommonException(
                            ResponseCode.SERVER_ERROR.getErrorCode(),
                            ResponseCode.SERVER_ERROR.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }
        sender().tell(res, self());
    }
    private void getPassbookByAdmin(Request request) {
        Response res;
        try{
            String requestedBy = (String) request.getContext().get(JsonKey.REQUESTED_BY);

            PassbookService service = new PassbookServiceImpl();
            res = service.getPassbook(request.getRequestContext(), requestedBy, request.getRequest(),true);
        } catch (Exception e) {
            logger.error(request.getRequestContext(), "PassbookActor:getPassbookByAdmin: Error occurred = " + e.getMessage(), e);
            ProjectCommonException exception =
                    new ProjectCommonException(
                            ResponseCode.SERVER_ERROR.getErrorCode(),
                            ResponseCode.SERVER_ERROR.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }
        sender().tell(res, self());
    }
}
