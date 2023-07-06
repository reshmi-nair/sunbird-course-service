package org.sunbird.learner.actors.coursebatch.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.batch.user.BatchUser;

import java.util.List;
import java.util.Map;

public interface BatchUserDao {



    BatchUser readById(RequestContext requestContext, String batchId,String userId);

    /**
     * Create an entry for  user batch information
     *
     * @param requestContext
     * @param batchUserDetails  user batch information
     */
    Response insert(RequestContext requestContext, Map<String, Object> batchUserDetails);

    /**
     * Get batch user information.
     *
     *
     * @param request batchId
     * @return User batch information
     */
    List<Map<String, Object>> readBatchUsersList(Request request, String batchId);
    /**
     * Update batch user.
     *
     * @param batchUserMap batch user information to be updated
     * @return Response containing status of batch user update
     */
    Response update(RequestContext requestContext,String batchId, String userId,Map<String, Object> batchUserMap);

    /**
     * Deleted batch user.
     *
     * @param  batchId user information to be updated
     * @return Response containing status of batch user delete
     */
    Response delete(RequestContext requestContext, String batchId,String userId);

}
