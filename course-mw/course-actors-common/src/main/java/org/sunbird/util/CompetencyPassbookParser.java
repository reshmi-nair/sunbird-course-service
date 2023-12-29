package org.sunbird.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.encoder.org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.models.passbook.CompetencyInfo;
import org.sunbird.models.passbook.CompetencyPassbookInfo;
import org.sunbird.common.models.response.Response;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

public class CompetencyPassbookParser implements PassbookParser {

    private Logger logger = LoggerFactory.getLogger(CompetencyPassbookParser.class);

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public void parseDBInfo(List<Map<String, Object>> passbookList, Response response) {
        if (CollectionUtils.isEmpty(passbookList)) {
            response.put(org.sunbird.common.Constants.RESPONSE, org.sunbird.common.Constants.SUCCESS);
            response.getResult().put(JsonKey.COUNT, 0);
            response.getResult().put(JsonKey.CONTENT, CollectionUtils.EMPTY_COLLECTION);
            return;
        }
        System.out.println("passbookList" + passbookList);
        Map<String, CompetencyPassbookInfo> competencyMap = new HashMap<String, CompetencyPassbookInfo>();
        // Parse the read values from DB and add it into response.result object
        for (Map<String, Object> competencyObj : passbookList) {
            String userId = (String) competencyObj.get(JsonKey.USER_ID);
            System.out.println("userid" + userId);
            CompetencyPassbookInfo competencyPassbookInfo = null;
            if (competencyMap.containsKey(userId)) {
                competencyPassbookInfo = competencyMap.get(userId);
            } else {
                competencyPassbookInfo = new CompetencyPassbookInfo(userId);
                competencyMap.put(userId, competencyPassbookInfo);
            }
            String competencyId = (String) competencyObj.get(JsonKey.TYPE_ID);
            System.out.println("competencyId 3" + competencyId);
            CompetencyInfo competencyInfo = competencyPassbookInfo.getCompetencies().get(competencyId);
            if (competencyInfo == null) {
                competencyInfo = new CompetencyInfo(competencyId);
            }

            if (ObjectUtils.isEmpty(competencyInfo.getAdditionalParams())) {
                competencyInfo.setAdditionalParams((Map<String, String>) competencyObj.get(JsonKey.ADDITIONAL_PARAM));
            }
            Map<String, Object> acquiredDetail = new HashMap<String, Object>();
            acquiredDetail.put(JsonKey.ACQUIRED_CHANNEL, (String) competencyObj.get(JsonKey.ACQUIRED_CHANNEL));
            acquiredDetail.put(JsonKey.COMPETENCY_LEVEL_ID, (String) competencyObj.get(JsonKey.CONTEXT_ID));
            acquiredDetail.put(JsonKey.EFFECTIVE_DATE, competencyObj.get(JsonKey.EFFECTIVE_DATE));
            acquiredDetail.put(JsonKey.ADDITIONAL_PARAM,
                    (Map<String, Object>) competencyObj.get(JsonKey.ACQUIRED_DETAILS));
            System.out.println("acquired detail 1" + acquiredDetail);
            Map<String, Object> acquiredDetailAdditionalParam = (Map<String, Object>) competencyObj
                    .get(JsonKey.ACQUIRED_DETAILS);
            System.out.println("acquiredDetailAdditionalParam 2" + acquiredDetailAdditionalParam);
            Iterator<Entry<String, Object>> iterator = acquiredDetailAdditionalParam.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, Object> entry = iterator.next();
                if (entry.getValue() instanceof String) {
                    acquiredDetail.put(entry.getKey(), (String) entry.getValue());
                } else {
                    // TODO - We need JSON schema config to determine the type of value.
                }
            }

            competencyInfo.getAcquiredDetails().add(acquiredDetail);
            competencyPassbookInfo.getCompetencies().put(competencyId, competencyInfo);
        }
        response.getResult().put(JsonKey.COUNT, competencyMap.size());
        response.getResult().put(JsonKey.CONTENT, competencyMap.values());
    }

    @Override
    public String validateUpdateReqeust(Map<String, Object> requestBody, String requestedUserId,
                                        List<Map<String, Object>> dbModel) {
        StringBuilder errMsg = new StringBuilder();
        List<String> missingAttributes = new ArrayList<String>();
        List<String> errList = new ArrayList<String>();
        Map<String, Map<String, Object>> competencyMap = new HashMap<String, Map<String, Object>>();

        String compUserId = (String) requestBody.get(JsonKey.USER_ID);
        if (StringUtils.isEmpty(compUserId)) {
            missingAttributes.add(JsonKey.USER_ID);
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

        List<Map<String, Object>> competencyList = (List<Map<String, Object>>) requestBody
                .get(JsonKey.COMPETENCY_DETAILS);
        if (CollectionUtils.isEmpty(competencyList)) {
            missingAttributes.add(JsonKey.COMPETENCY_DETAILS);
        } else if (errMsg.length() == 0) {
            for (Map<String, Object> competency : competencyList) {
                String err = validateCompetencyObject(requestedUserId, compUserId, competency, competencyMap);
                if (!StringUtils.isEmpty(err)) {
                    errMsg.append(err);
                    break;
                }
            }
        }

        if (errMsg.length() == 0) {
            dbModel.addAll(competencyMap.values());
        }
        return errMsg.toString();
    }

    private String validateCompetencyObject(String requestedUserId, String compUserId,
                                            Map<String, Object> competencyRequest, Map<String, Map<String, Object>> competencyMap) {
        if (ObjectUtils.isEmpty(competencyRequest)) {
            return "Invalid CompetencyDetail object";
        }
        StringBuilder errMsg = new StringBuilder();
        List<String> missingAttributes = new ArrayList<String>();
        List<String> errList = new ArrayList<String>();

        String competencyId = (String) competencyRequest.get(JsonKey.COMPETENCY_ID);
        if (StringUtils.isBlank(competencyId)) {
            missingAttributes.add(JsonKey.COMPETENCY_ID);
        }

        if (competencyMap.containsKey(competencyId)) {
            return String.format("Invalid Request. Competency %s is provided twice.", competencyId);
        }

        Map<String, Object> competency = new HashMap<String, Object>();
        competency.put(JsonKey.USER_ID, compUserId);
        competency.put(JsonKey.TYPE_ID, competencyId);
        competency.put(JsonKey.TYPE_NAME, JsonKey.COMPETENCY);
        System.out.println("competency inside check" + competency);

        Map<String, Object> acquiredDetailsMap = (Map<String, Object>) competencyRequest
                .get(JsonKey.ACQUIRED_DETAILS);
        if (ObjectUtils.isEmpty(acquiredDetailsMap)) {
            missingAttributes.add(JsonKey.ACQUIRED_DETAILS);
        } else {
            String acquiredChannel = (String) acquiredDetailsMap.get(JsonKey.ACQUIRED_CHANNEL);
            if (StringUtils.isBlank(acquiredChannel)) {
                missingAttributes.add(JsonKey.ACQUIRED_CHANNEL);
            } else {
                competency.put(JsonKey.ACQUIRED_CHANNEL, acquiredChannel);
                // Parse additionalParams from request
                Map<String, Object> acquiredDetailAdditionalParam = (Map<String, Object>) acquiredDetailsMap
                        .get(JsonKey.ADDITIONAL_PARAM);
                Map<String, String> acquiredDetails = new HashMap<String, String>();
                if (!ObjectUtils.isEmpty(acquiredDetailAdditionalParam)) {
                    Iterator<Entry<String, Object>> iterator = acquiredDetailAdditionalParam.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry<String, Object> entry = iterator.next();
                        if (entry.getValue() instanceof String) {
                            acquiredDetails.put(entry.getKey(), (String) entry.getValue());
                        } else {
                            try {
                                acquiredDetails.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
                            } catch (JsonProcessingException e) {
                                errList.add("Failed to parse acquiredDetails for competency : " + competencyId);
                                break;
                            }
                        }
                    }
                }
                System.out.println("competency inside check 2" + competency);

                acquiredDetails.put(JsonKey.CREATED_BY, requestedUserId);
                acquiredDetails.put(JsonKey.CREATED_DATE, DateTime.now().toString());
                competency.put(JsonKey.ACQUIRED_DETAILS, acquiredDetails);
            }

            String competencyLevelId = (String) acquiredDetailsMap.get(JsonKey.COMPETENCY_LEVEL_ID);
            if (StringUtils.isBlank(competencyLevelId)) {
                missingAttributes.add(JsonKey.COMPETENCY_LEVEL_ID);
            } else {
                competency.put(JsonKey.CONTEXT_ID, competencyLevelId);
            }

            String strEffectiveDate = (String) acquiredDetailsMap.get(JsonKey.EFFECTIVE_DATE);
            if (StringUtils.isBlank(strEffectiveDate)) {
                competency.put(JsonKey.EFFECTIVE_DATE, Timestamp.from(Instant.now()));
                // missingAttributes.add(JsonKey.EFFECTIVE_DATE);
            } else {
                try {
                    Timestamp effectiveDate = Timestamp.valueOf(strEffectiveDate);
                    competency.put(JsonKey.EFFECTIVE_DATE, effectiveDate);
                } catch (IllegalArgumentException e) {
                    logger.error(String.format("Failed to parse date: %s, Exception: ", strEffectiveDate), e);
                }
            }
            System.out.println("competency inside check 3" + competency);

        }

        Map<String, String> additionalParams = (Map<String, String>) competencyRequest.get(JsonKey.ADDITIONAL_PARAM);
        if (!ObjectUtils.isEmpty(additionalParams)) {
            competency.put(JsonKey.ADDITIONAL_PARAM, additionalParams);
        }

        if (!missingAttributes.isEmpty()) {
            errMsg.append("Request doesn't have mandatory parameters - [").append(missingAttributes.toString())
                    .append("]. ");
        }

        if (!errList.isEmpty()) {
            errMsg.append(errList.toString());
        }

        if (errMsg.length() == 0) {
            competencyMap.put(competencyId, competency);
        }

        return errMsg.toString();
    }
}