package org.sunbird.dao.passbook;

import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

import java.util.List;
import java.util.Map;

public class PassbookDaoImpl {
    private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final String KEYSPACE_NAME = Util.dbInfoMap.get(JsonKey.PASSBOOK_DB).getKeySpace();
    private static final String TABLE_NAME = Util.dbInfoMap.get(JsonKey.PASSBOOK_DB).getTableName();

    public Response batchInsert(RequestContext requestContext, List<Map<String, Object>> passbookDetails) {
        return cassandraOperation.batchInsert(requestContext, KEYSPACE_NAME, TABLE_NAME, passbookDetails);
    }

    public Response insert(RequestContext requestContext, Map<String, Object> passbookDetails) {
        return cassandraOperation.insertRecord(requestContext, KEYSPACE_NAME, TABLE_NAME, passbookDetails);
    }

    public List<Map<String, Object>> getPassbook(RequestContext requestContext,Map<String, Object> primaryKey) {
        Response response = cassandraOperation.getRecordByIdentifier(requestContext, KEYSPACE_NAME, TABLE_NAME, primaryKey, null);
        List<Map<String, Object>> passbookList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isEmpty(passbookList)) {
            return null;
        } else {
            return passbookList;
        }
    }
}
