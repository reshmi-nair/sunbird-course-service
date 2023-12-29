package org.sunbird.service.passbook;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;

import java.util.Map;

public interface PassbookService
{
    public Response getPassbook(RequestContext context, String requestedUserId, Map<String, Object> request, boolean isAdmin);
    public Response updatePassbook(RequestContext context, String requestedUserId, Map<String, Object> request);
}
