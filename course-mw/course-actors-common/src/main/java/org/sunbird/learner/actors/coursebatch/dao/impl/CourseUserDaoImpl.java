package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.CassandraPropertyReader;
import org.sunbird.common.request.Request;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.CourseUserDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.batch.user.BatchUser;
import org.sunbird.models.course.user.CourseUser;


import java.util.*;


public class CourseUserDaoImpl implements CourseUserDao {

    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private Util.DbInfo courseUserDb = Util.dbInfoMap.get(JsonKey.COURSE_USER_DB);

    private ObjectMapper mapper = new ObjectMapper();

    public LoggerUtil logger = new LoggerUtil(this.getClass());


    @Override
    public Response insert(RequestContext requestContext, Map<String, Object> courseUserDetails){
        return cassandraOperation.insertRecord(
                requestContext, courseUserDb.getKeySpace(), courseUserDb.getTableName(),courseUserDetails);
    }

    /**
     * Get course user information.
     *
     * @param courseId course user identifiers
     * @param requestContext
     * @return User courses information
     */
    @Override
    public CourseUser readById(RequestContext requestContext, String courseId,String userId) {
        logger.info(requestContext,"fetching data based on courseId "+courseId);
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.COURSE_ID_KEY, courseId);
        primaryKey.put(JsonKey.USER_ID_KEY, userId);
        Response response = cassandraOperation.getRecordByIdentifier(requestContext, courseUserDb.getKeySpace(), courseUserDb.getTableName(), primaryKey, null);
        List<Map<String, Object>> CourseUserList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isEmpty(CourseUserList)) {
            return null;
        }
        try {
            return mapper.convertValue((Map<String, Object>) CourseUserList.get(0), CourseUser.class);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Update courseUser.
     *
     * @param map Course user information to be updated
     * @return Response containing status of courseUser update
     */
    @Override
    public Response update(RequestContext requestContext, String courseId,String userId, Map<String, Object> map) {
        logger.info(requestContext,"updating data based on courseId and return the response"+courseId);
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.COURSE_ID_KEY, courseId);
        primaryKey.put(JsonKey.USER_ID, userId);
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.putAll(map);
        attributeMap.remove(JsonKey.COURSE_ID_KEY);
        attributeMap.remove(JsonKey.USER_ID_KEY);
        attributeMap = CassandraUtil.changeCassandraColumnMapping(attributeMap);
        logger.info(requestContext,"changing cassdra cloumnmapping and asign to attributeMap"+attributeMap);
        return cassandraOperation.updateRecord(
                requestContext, courseUserDb.getKeySpace(), courseUserDb.getTableName(), attributeMap, primaryKey);
    }
    /**
     * Get course user information.
     *
     * @param courseId and courseList
     *
     * @return User courses information
     */
    public List<Map<String, Object>> readCourseUsersList(Request request,String courseId) {
        Map<String, Object> search = (Map<String, Object>)request.getRequest().getOrDefault(JsonKey.FILTERS,"");
        Response response =
                cassandraOperation.getRecordByIndexedPropertyPagination(courseUserDb.getKeySpace(), courseUserDb.getTableName(),search,request);
        List<Map<String, Object>> courseUserList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isEmpty(courseUserList)) {
            return null;
        }
        return courseUserList;

    }

    public Response delete(RequestContext requestContext, String courseid,String userid) {
        List<String> list=new ArrayList<>();
        list.add(courseid);list.add(userid);
        return cassandraOperation.deleteRecordCourseId(
                courseUserDb.getKeySpace(), courseUserDb.getTableName(), list, requestContext);
    }



}
