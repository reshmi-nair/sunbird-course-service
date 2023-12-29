package controllers.passbook.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PassbookRequestValidator extends BaseRequestValidator {
    public void validatePassbookUpdateRequest(Request requestDto) {
        validateParam(
                (String) requestDto.getRequest().get(JsonKey.USER_ID),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.USER_ID);
        commonValidations(requestDto);
    }
    public void validateGetPassbookRequest(Request requestDto) {
        commonValidations(requestDto);
    }
    public void validateGetPassbookRequestByAdmin(Request requestDto) {
        commonValidations(requestDto);
        // Need to check at least one userId is available
        List<String> userIdList = (List<String>) requestDto.getRequest().get(JsonKey.USER_IDs);
        if (userIdList.stream().allMatch(x -> x == null || x.isEmpty())) {
            throw new ProjectCommonException(
                    ResponseCode.invalidValue.getErrorCode(),
                    ProjectUtil.formatMessage(
                            ResponseCode.invalidValue.getErrorMessage(),
                            JsonKey.USER_IDs,
                            userIdList),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }
    private void commonValidations(Request requestDto) {

        validateParam(
                (String) requestDto.getRequest().get(JsonKey.TYPE_NAME),
                ResponseCode.mandatoryParamsMissing,
                JsonKey.TYPE_NAME);
        List<String> passbookTypeList = new ArrayList<>(
                Arrays.asList(
                        PropertiesCache.getInstance()
                                .getProperty(JsonKey.PASSBOOK_TYPES)
                                .split(",")));
        if (!passbookTypeList.contains(
                ((String) requestDto.getRequest().get(JsonKey.TYPE_NAME)).toLowerCase())) {
            throw new ProjectCommonException(
                    ResponseCode.invalidValue.getErrorCode(),
                    ProjectUtil.formatMessage(
                            ResponseCode.invalidValue.getErrorMessage(),
                            JsonKey.TYPE_NAME,
                            ((String) requestDto.getRequest().get(JsonKey.TYPE_NAME)),
                            String.join(StringFormatter.COMMA, passbookTypeList)),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }
}
