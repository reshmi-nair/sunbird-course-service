package org.sunbird.learner.actors.coursebatch.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.batch.user.BatchUser;
import org.sunbird.models.course.user.CourseUser;

import java.util.List;
import java.util.Map;

public interface CourseUserDao {


    /**
     * Create course user.
     *
     * @param requestContext
     * @param courseUserDetails Course batch information to be created
     * @return Response containing identifier of created course batch
     */
    Response insert(RequestContext requestContext, Map<String, Object> courseUserDetails);

    /**
     * Get course user information.
     *
     * @param courseId course user identifiers
     * @param requestContext
     * @return User courses information
     */
    CourseUser readById(RequestContext requestContext, String courseId,String userId);


    /**
     * Update courseUser.
     *
     * @param courseUserMap Course user information to be updated
     * @return Response containing status of courseUser update
     */
    Response update(RequestContext requestContext, String courseId,String userId, Map<String, Object> courseUserMap);

    /**
     * Get course user information.
     *
     *
     * @param request
     * @return User course information
     */
    List<Map<String, Object>> readCourseUsersList(Request request, String courseId);

    /**
     * Deleted batch user.
     *
     * @param  courseId user information to be updated
     * @return Response containing status of batch user delete
     */
    Response delete(RequestContext requestContext, String courseId,String userId);

}
