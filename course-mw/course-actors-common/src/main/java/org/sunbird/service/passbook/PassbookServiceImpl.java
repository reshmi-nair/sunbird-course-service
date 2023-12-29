package org.sunbird.service.passbook;

import net.logstash.logback.encoder.org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dao.passbook.PassbookDaoImpl;
import org.sunbird.util.*;

import java.util.*;
public class PassbookServiceImpl implements PassbookService {

    private Logger logger = LoggerFactory.getLogger(PassbookServiceImpl.class);
    PassbookDaoImpl passbookDao = new PassbookDaoImpl();
    PassbookParserHandler parserHanlder = new PassbookParserHandler();
    @Override
    public Response getPassbook(RequestContext context, String requestedUserId, Map<String, Object> request,boolean isAdminApi) {
        return getPassbookDetails(context, requestedUserId, request, isAdminApi);
    }

    @Override
    public Response updatePassbook(RequestContext context, String requestedUserId, Map<String, Object> request) {
        Response dbResponse = null;
        String errMsg = validateUpdateRequest(request);
        if (StringUtils.isNotBlank(errMsg)) {
            ProjectCommonException exception =
            new ProjectCommonException(
                    ResponseCode.invalidRequestData.getErrorCode(),
                    ResponseCode.invalidRequestData.getErrorMessage(),
                    ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }
        try {
            String typeName = (String) request.get(JsonKey.TYPE_NAME);
            PassbookParser parser = getPassbookParser(typeName);

            List<Map<String, Object>> passbookDbInfoList = new ArrayList<Map<String, Object>>();
            errMsg = parser.validateUpdateReqeust(request, requestedUserId, passbookDbInfoList);
            if (errMsg.length() == 0) {
                dbResponse = passbookDao.batchInsert(context, passbookDbInfoList);
            }
        } catch (Exception e) {
            ProjectCommonException exception =
                    new ProjectCommonException(
                            ResponseCode.dbUpdateError.getErrorCode(),
                            ResponseCode.dbUpdateError.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }
        return dbResponse;
    }

    private Response getPassbookDetails(RequestContext context, String requestedUserId, Map<String, Object> requestBody, boolean isAdminApi) {
        Response response = new Response();;
                // Read request Data and validate it.
        String errMsg = validateReadRequest(requestBody, isAdminApi);
        if (StringUtils.isNotBlank(errMsg)) {
            ProjectCommonException exception =
                    new ProjectCommonException(
                            ResponseCode.invalidRequestData.getErrorCode(),
                            ResponseCode.invalidRequestData.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }

        try {
            String typeName = (String) requestBody.get(JsonKey.TYPE_NAME);
            Map<String, Object> propertyMap = new HashMap<>();
            List<String> userIdList = null;
            if (isAdminApi) {
                userIdList = (List<String>) requestBody.get(JsonKey.USER_IDs);
            } else {
                userIdList = Arrays.asList(requestedUserId);
            }

            propertyMap.put(JsonKey.USER_ID, userIdList);
            propertyMap.put(JsonKey.TYPE_NAME, typeName);
            List<Map<String, Object>> passbookList = passbookDao.getPassbook(context,propertyMap);
            PassbookParser parser = getPassbookParser(typeName);

            parser.parseDBInfo(passbookList, response);
        } catch (Exception e) {
            errMsg = String.format("Failed to read passbook details. Exception: ", e.getMessage());
            logger.error(errMsg, e);
        }
        if (StringUtils.isNotBlank(errMsg)) {
            ProjectCommonException exception =
                    new ProjectCommonException(
                            ResponseCode.SERVER_ERROR.getErrorCode(),
                            ResponseCode.SERVER_ERROR.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
            throw exception;
        }
        return response;
    }

    private String validateReadRequest(Map<String, Object> requestBody, boolean isAdminApi) {
        if (ObjectUtils.isEmpty(requestBody)) {
            return "Invalid Passbook Read request.";
        }

        StringBuilder errMsg = new StringBuilder();
        List<String> missingAttributes = new ArrayList<String>();
        List<String> errList = new ArrayList<String>();

        if (isAdminApi) {
            // Need to check at least one userId is available
            List<String> userIdList = (List<String>) requestBody.get(JsonKey.USER_IDs);
            if (CollectionUtils.isEmpty(userIdList)) {
                missingAttributes.add(JsonKey.USER_ID);
            } else {
                if (userIdList.stream().allMatch(x -> x == null || x.isEmpty())) {
                    errList.add(JsonKey.USER_ID + " contains null or empty. ");
                }
            }
        }

        String typeName = (String) requestBody.get(JsonKey.TYPE_NAME);
        if (StringUtils.isBlank(typeName)) {
            missingAttributes.add(JsonKey.TYPE_NAME);
        } else {
            if (!(ProjectUtil.getConfigValue("user.passbook.supported.typename")).contains(typeName)) {
                errList.add(String.format("Invalid TypeName value. Supported TypeNames are %s",
                        (ProjectUtil.getConfigValue("user.passbook.supported.typename"))));
            }
        }

        if (!missingAttributes.isEmpty()) {
            errMsg.append("Request doesn't have mandatory parameters - [").append(missingAttributes.toString())
                    .append("]. ");
        }

        if (!errList.isEmpty()) {
            errMsg.append(errList.toString());
        }

        return errMsg.toString();
    }

    private String validateUpdateRequest(Map<String, Object> request) {
        if (ObjectUtils.isEmpty(request)) {
            return "Invalid Passbook Read request.";
        }

        StringBuilder errMsg = new StringBuilder();
        List<String> missingAttributes = new ArrayList<String>();
        List<String> errList = new ArrayList<String>();

        String typeName = (String) request.get(JsonKey.TYPE_NAME);
        if (StringUtils.isBlank(typeName)) {
            missingAttributes.add(JsonKey.TYPE_NAME);
        } else {
            if (!(ProjectUtil.getConfigValue("user.passbook.supported.typename")).contains(typeName)) {
                errList.add(String.format("Invalid TypeName value. Supported TypeNames are %s",
                        (ProjectUtil.getConfigValue("user.passbook.supported.typename"))));
            }
        }

        if (!missingAttributes.isEmpty()) {
            errMsg.append("Request doesn't have mandatory parameters - [").append(missingAttributes.toString())
                    .append("]. ");
        }

        if (!errList.isEmpty()) {
            errMsg.append(errList.toString());
        }
        return errMsg.toString();
    }

    private PassbookParser getPassbookParser(String typeName) {
        return parserHanlder.getPassbookParser(typeName);
    }
}
