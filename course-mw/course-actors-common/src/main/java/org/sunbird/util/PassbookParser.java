package org.sunbird.util;

import org.sunbird.common.models.response.Response;

import java.util.List;
import java.util.Map;

public interface PassbookParser {
    public void parseDBInfo(List<Map<String, Object>> passbookList, Response response);

    public String validateUpdateReqeust(Map<String, Object> request, String requestedUserId,
                                        List<Map<String, Object>> dbModel);
}