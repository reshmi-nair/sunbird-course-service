package org.sunbird.learner.actors.certificate.service;

import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.base.BaseActor;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.kafka.client.InstructionEventGenerator;
import org.sunbird.learner.actors.coursebatch.dao.impl.UserCoursesDaoImpl;
import org.sunbird.learner.constants.CourseJsonKey;
import org.sunbird.learner.constants.InstructionEvent;
import org.sunbird.learner.util.CourseBatchUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.courses.UserCourses;

public class CertificateActor extends BaseActor {
  

  private static enum ResponseMessage {
    SUBMITTED("Certificates issue action for Course Batch Id {0} submitted Successfully!"),
    FAILED("Certificates issue action for Course Batch Id {0} Failed!"),
    PIAA_SUBMITTED("Certificates will be issued for Course Batch Id {0} after evaluation ");
    private String value;

    private ResponseMessage(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  };

  private Util.DbInfo userRoleDb = Util.dbInfoMap.get(JsonKey.USER_ROLES_DB);
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER, this.getClass().getName());

    String requestedOperation = request.getOperation();
    switch (requestedOperation) {
      case "issueCertificate":
        issueCertificate(request);
        break;
      case "issueCertificateForPIAA":
        issueCertificateForPIAA(request);
        break;
      default:
        onReceiveUnsupportedOperation(request.getOperation());
        break;
    }
  }

  private void issueCertificate(Request request) {
    logger.info(request.getRequestContext(), "issueCertificate request=" + request.getRequest());
    final String batchId = (String) request.getRequest().get(JsonKey.BATCH_ID);
    final String courseId = (String) request.getRequest().get(JsonKey.COURSE_ID);
    List<String> userIds = (List<String>) request.getRequest().get(JsonKey.USER_IDs);
    final boolean reIssue = isReissue(request.getContext().get(CourseJsonKey.REISSUE));
    Map<String, Object> courseBatchResponse =
        CourseBatchUtil.validateCourseBatch(request.getRequestContext(), courseId, batchId);
    if (null == courseBatchResponse.get("cert_templates")) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.CLIENT_ERROR, "No certificate templates associated with " + batchId);
    }
    Map<String, Object> courseDetailsResponse =
            CourseBatchUtil.getCourseDetails(request.getRequestContext(), courseId);
    Response response = new Response();
    Map<String, Object> resultData = new HashMap<>();
    if(courseDetailsResponse.get(JsonKey.PRIMARYCATEGORY).equals("PIAA Assessment")){
      //Do Nothing
    }else{
      resultData.put(JsonKey.STATUS, MessageFormat.format(ResponseMessage.SUBMITTED.getValue(), batchId));
      resultData.put(JsonKey.BATCH_ID, batchId);
      resultData.put(JsonKey.COURSE_ID, courseId);
      resultData.put(JsonKey.COLLECTION_ID, courseId);
      response.put(JsonKey.RESULT, resultData);
      try {
        pushInstructionEvent(batchId, courseId, userIds, reIssue);
      } catch (Exception e) {
        logger.error(request.getRequestContext(), "issueCertificate pushInstructionEvent error for courseId="
                + courseId + ", batchId=" + batchId, e);
        resultData.put(
                JsonKey.STATUS, MessageFormat.format(ResponseMessage.FAILED.getValue(), batchId));
      }
    }
    sender().tell(response, self());
  }

  private void issueCertificateForPIAA(Request request) {
    logger.info(request.getRequestContext(), "issueCertificateForPIAA request=" + request.getRequest());
    final String batchId = (String) request.getRequest().get(JsonKey.BATCH_ID);
    final String courseId = (String) request.getRequest().get(JsonKey.COURSE_ID);
    List<String> userIds = (List<String>) request.getRequest().get(JsonKey.USER_IDs);
    final boolean reIssue = isReissue(request.getContext().get(CourseJsonKey.REISSUE));
    Integer statusCode = (Integer) request.getOrDefault(JsonKey.STATUS, 0);
    for (int x = 0; x < userIds.size(); x++) {
      UserCoursesDaoImpl userCoursesDao=new UserCoursesDaoImpl();
      UserCourses enrolmentData = userCoursesDao.read(request.getRequestContext(), userIds.get(x), courseId, batchId);
      logger.info(request.getRequestContext(),"checking enrolment Data"+enrolmentData);
      if (enrolmentData.getComment() != null) {
        enrolmentData.getComment().put(getUserRole(request.getContext().getOrDefault(JsonKey.REQUESTED_BY, "").toString()), "");
        logger.info(request.getRequestContext(),"checking comment from enrolment"+enrolmentData.getComment());
        // creating request map
        HashMap<String, Object> map =(HashMap<String, Object>) createCourseEvalRequestMap(enrolmentData.getComment(), statusCode);
        logger.info(null,"checking map"+map);
        // creating cassandra column map
        HashMap<String, Object> data = (HashMap<String, Object>) CassandraUtil.changeCassandraColumnMapping(map);
        userCoursesDao.updateV2(request.getRequestContext(), userIds.get(x), courseId, batchId, data);
      }
    }
    Map<String, Object> courseBatchResponse =
            CourseBatchUtil.validateCourseBatch(request.getRequestContext(), courseId, batchId);
    if (null == courseBatchResponse.get("cert_templates")) {
      ProjectCommonException.throwClientErrorException(
              ResponseCode.CLIENT_ERROR, "No certificate templates associated with " + batchId);
    }
    Map<String, Object> courseDetailsResponse =
            CourseBatchUtil.getCourseDetails(request.getRequestContext(), courseId);
    Response response = new Response();
    Map<String, Object> resultData = new HashMap<>();
    if(courseDetailsResponse.get(JsonKey.PRIMARYCATEGORY).equals("PIAA Assessment")){
      resultData.put(JsonKey.STATUS, MessageFormat.format(ResponseMessage.PIAA_SUBMITTED.getValue(), batchId));
      resultData.put(JsonKey.BATCH_ID, batchId);
      resultData.put(JsonKey.COURSE_ID, courseId);
      resultData.put(JsonKey.COLLECTION_ID, courseId);
      response.put(JsonKey.RESULT, resultData);
      try {
        pushInstructionEvent(batchId, courseId, userIds, reIssue);
      } catch (Exception e) {
        logger.error(request.getRequestContext(), "issueCertificate pushInstructionEvent error for courseId="
                + courseId + ", batchId=" + batchId, e);
        resultData.put(
                JsonKey.STATUS, MessageFormat.format(ResponseMessage.FAILED.getValue(), batchId));
      }
    }
    sender().tell(response, self());
  }
  private boolean isReissue(Object queryString) {
    if (queryString != null) {
      if (queryString instanceof String[]) {
        String query = Arrays.stream((String[]) queryString).findFirst().orElse(null);
        return Boolean.parseBoolean(query);
      } else if (queryString instanceof String) {
        return Boolean.parseBoolean((String) queryString);
      }
    }
    return false;
  }

  /**
   * Construct the instruction event data and push the event data as BEInstructionEvent.
   *
   * @param batchId
   * @param courseId
   * @throws Exception
   */
  private void pushInstructionEvent(
      String batchId, String courseId, List<String> userIds, boolean reIssue) throws Exception {
    Map<String, Object> data = new HashMap<>();

    data.put(
        CourseJsonKey.ACTOR,
        new HashMap<String, Object>() {
          {
            put(JsonKey.ID, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getActorId());
            put(JsonKey.TYPE, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getActorType());
          }
        });

    String id = OneWayHashing.encryptVal(batchId + CourseJsonKey.UNDERSCORE + courseId);
    data.put(
        CourseJsonKey.OBJECT,
        new HashMap<String, Object>() {
          {
            put(JsonKey.ID, id);
            put(JsonKey.TYPE, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getType());
          }
        });

    data.put(CourseJsonKey.ACTION, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getAction());

    data.put(
        CourseJsonKey.E_DATA,
        new HashMap<String, Object>() {
          {
            if (CollectionUtils.isNotEmpty(userIds)) {
              put(JsonKey.USER_IDs, userIds);
            }
            put(JsonKey.BATCH_ID, batchId);
            put(JsonKey.COURSE_ID, courseId);
            put(CourseJsonKey.ACTION, InstructionEvent.ISSUE_COURSE_CERTIFICATE.getAction());
            put(CourseJsonKey.ITERATION, 1);
            if (reIssue) {
              put(CourseJsonKey.REISSUE, true);
            }
          }
        });
    String topic = ProjectUtil.getConfigValue("kafka_topics_certificate_instruction");
    InstructionEventGenerator.pushInstructionEvent(batchId, topic, data);
  }

  private Map<String, Object> createCourseEvalRequestMap(Map<String, String> comment, Integer statusCode) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.STATUS, statusCode);
    map.put(JsonKey.COMMENT, comment);
    return map;
  }

  private String getUserRole(String userid) {
    Response response = cassandraOperation.getRecordByUserId(null, userRoleDb.getKeySpace(), userRoleDb.getTableName(), userid, null);
    List<Map<String, String>> responseList = (List<Map<String, String>>) response.get("response");

    List<String> filteredRoles = new ArrayList<>();
    for (Map<String, String> roleMap : responseList) {
      String role = roleMap.get(JsonKey.ROLE);
      if (role.equals(JsonKey.NODAL_OFFICER) || role.equals(JsonKey.ORG_ADMIN)) {
        filteredRoles.add(role);
      }
    }

    if (!filteredRoles.isEmpty()) {
      return filteredRoles.get(0);
    } else {
      throw new NoSuchElementException("No matching role found for user ID: " + userid);
    }
  }
}
