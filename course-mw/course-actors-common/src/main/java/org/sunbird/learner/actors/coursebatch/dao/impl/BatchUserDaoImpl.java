package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.BatchUserDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.batch.user.BatchUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BatchUserDaoImpl implements BatchUserDao {


    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private Util.DbInfo batchUserDb = Util.dbInfoMap.get(JsonKey.BATCH_USER_DB);

    private ObjectMapper mapper = new ObjectMapper();

    public LoggerUtil logger = new LoggerUtil(this.getClass());

    @Override
    public BatchUser readById(RequestContext requestContext, String batchId,String userId) {

        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.BATCH_ID_KEY, batchId);
        primaryKey.put(JsonKey.USER_ID_KEY, userId);
        Response response = cassandraOperation.getRecordByIdentifier(requestContext, batchUserDb.getKeySpace(), batchUserDb.getTableName(), primaryKey, null);
        List<Map<String, Object>> batchUserList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isEmpty(batchUserList)) {
            return null;
        }
        try {
            return mapper.convertValue((Map<String, Object>) batchUserList.get(0), BatchUser.class);
        } catch (Exception e) {

            return null;
        }
    }

    public List<Map<String, Object>> readBatchUsersList(Request request, String batchId) {
        Map<String, Object> search = (Map<String, Object>)request.getRequest().getOrDefault(JsonKey.FILTERS,"");
        logger.info(null, "search : " + search);
        search.put(JsonKey.BATCH_ID,batchId);
        Response batchUserResponse =
                cassandraOperation.getRecordByIndexedPropertyPagination(batchUserDb.getKeySpace(), batchUserDb.getTableName(),search,request);
        List<Map<String, Object>> batchUserList =
                (List<Map<String, Object>>) batchUserResponse.get(JsonKey.RESPONSE);

        if (CollectionUtils.isEmpty(batchUserList)) {
            return null;
        }
        logger.debug(null, "batchUserList : " + batchUserList);
        return batchUserList;

    }

        @Override
        public Response insert (RequestContext requestContext, Map < String, Object > batchUserDetails){
            return cassandraOperation.insertRecord(requestContext, batchUserDb.getKeySpace(), batchUserDb.getTableName(), batchUserDetails);
        }

    /**
     * Update batch user.
     *
     * @param map batch user information to be updated
     * @return Response containing status of batch user update
     */
    @Override
    public Response update(RequestContext requestContext, String batchId, String userId,Map<String, Object> map) {
       logger.info(requestContext,"updating data based on batchId and return the response"+batchId);
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKey.USER_ID_KEY, userId);
        primaryKey.put(JsonKey.BATCH_ID_KEY, batchId);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.putAll(map);
        attributeMap.remove(JsonKey.USER_ID_KEY);
        attributeMap.remove(JsonKey.BATCH_ID_KEY);
        attributeMap = CassandraUtil.changeCassandraColumnMapping(attributeMap);
        logger.info(requestContext,"changing cassdra cloumnmapping and asign to attributeMap"+attributeMap);
        return cassandraOperation.updateRecord(
                requestContext, batchUserDb.getKeySpace(), batchUserDb.getTableName(), attributeMap, primaryKey);
    }

    public Response delete(RequestContext requestContext, String batchid,String userid) {
        List<String> list=new ArrayList<>();
        list.add(batchid); list.add(userid);
        return cassandraOperation.deleteRecordBatchId(
                batchUserDb.getKeySpace(), batchUserDb.getTableName(), list, requestContext);
    }
}
