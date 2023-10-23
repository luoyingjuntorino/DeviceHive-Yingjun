package com.devicehive.resource;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.updates.IexperimentUpdate;
import com.devicehive.vo.IexperimentVO;
import com.devicehive.vo.IexperimentWithUsersAndDevicesVO;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

@Api(tags = {"Iexperiment"}, value = "Represents a iexperiment, an isolated area where devices reside.", consumes="application/json")
@Path("/iexperiment")
public interface IexperimentResource {

    /**
     * Produces following output:
     * <pre>
     * [
     *  {
     *    "description":"Iexperiment Description",
     *    "id":1,
     *    "name":"Iexperiment Name"
     *   },
     *   {
     *    "description":"Iexperiment Description",
     *    "id":2,
     *    "name":"Iexperiment Name"
     *   }
     * ]
     * </pre>
     *
     * @param name        exact iexperiment's name, ignored, when  namePattern is not null
     * @param namePattern name pattern
     * @param sortField   Sort Field, can be either "id", "key", "name" or "description"
     * @param sortOrderSt ASC - ascending, otherwise descending
     * @param take        limit, default 1000
     * @param skip        offset, default 0
     */
    @GET
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_IEXPERIMENT')")
    @ApiOperation(value = "List iexperiments", notes = "Gets list of iexperiments the client has access to.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns array of Iexperiment resources in the response body.", response = IexperimentVO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void list(
            @ApiParam(name = "name", value = "Filter by iexperiment name.")
            @QueryParam("name")
                    String name,
            @ApiParam(name = "namePattern", value = "Filter by iexperiment name pattern. In pattern wildcards '%' and '_' can be used.")
            @QueryParam("namePattern")
                    String namePattern,
            @ApiParam(name = "sortField", value = "Result list sort field.", allowableValues = "ID,Name")
            @QueryParam("sortField")
                    String sortField,
            @ApiParam(name = "sortOrder", value = "Result list sort order. The sortField should be specified.", allowableValues = "ASC,DESC")
            @QueryParam("sortOrder")
                    String sortOrderSt,
            @ApiParam(name = "take", value = "Number of records to take from the result list.", defaultValue = "20")
            @QueryParam("take")
            @Min(0) @Max(Integer.MAX_VALUE)
                    Integer take,
            @ApiParam(name = "skip", value = "Number of records to skip from the result list.", defaultValue = "0")
            @QueryParam("skip")
                    Integer skip,
            @Suspended final AsyncResponse asyncResponse
    );

    /**
     * Generates JSON similar to this:
     * <pre>
     *     {
     *         "count":1
     *     }
     * </pre>
     *
     * @param name        exact iexperiment's name, ignored, when namePattern is not null
     * @param namePattern name pattern
     */
    @GET
    @Path("/count")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_IEXPERIMENT')")
    @ApiOperation(value = "Count iexperiments", notes = "Gets count of iexperiments the client has access to.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns the count of Iexperiment resources in the response body.",
                    response = EntityCountResponse.class, responseContainer = "Count"),
            @ApiResponse(code = 400, message = "If request parameters invalid"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    void count(
            @ApiParam(name = "name", value = "Filter by iexperiment name.")
            @QueryParam("name")
                    String name,
            @ApiParam(name = "namePattern", value = "Filter by iexperiment name pattern. In pattern wildcards '%' and '_' can be used.")
            @QueryParam("namePattern")
                    String namePattern,
            @Suspended final AsyncResponse asyncResponse
    );

    /**
     * Generates  JSON similar to this:
     * <pre>
     *     {
     *      "description":"Iexperiment Description",
     *      "id":1,
     *      "name":"Iexperiment Name"
     *     }
     * </pre>
     *
     * @param id iexperiment id, can't be null
     */
    @GET
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(#id, 'GET_IEXPERIMENT')")
    @ApiOperation(value = "Get iexperiment", notes = "Gets information about iexperiment and its devices.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "If successful, this method returns a Iexperiment resource in the response body.", response = IexperimentWithUsersAndDevicesVO.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If iexperiment not found")
    })
    Response get(
            @ApiParam(name = "id", value = "Iexperiment identifier.")
            @PathParam("id")
                    long id);

    /**
     * Inserts new Iexperiment into database. Consumes next input:
     * <pre>
     *     {
     *       "name":"Iexperiment Name",
     *       "description":"Iexperiment Description"
     *     }
     * </pre>
     * Where is not required "name" is required <p/> In case of success will produce
     * following output:
     * <pre>
     *     {
     *      "description":"Iexperiment Description",
     *      "id":1,
     *      "name":"Iexperiment Name"
     *     }
     * </pre>
     * Where "description"will be provided, if they are specified in request. Fields "id" and "name" will be
     * provided anyway.
     */
    @POST
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_IEXPERIMENT')")
    @ApiOperation(value = "Create iexperiment", notes = "Creates new iexperiment.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 201, message = "If successful, this method returns a Iexperiment resource in the response body.", response = IexperimentVO.class),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response insert(
            @ApiParam(value = "Iexperiment body", defaultValue = "{}", required = true)
                    IexperimentUpdate iexperiment);

    /**
     * This method updates iexperiment with given Id. Consumes following input:
     * <pre>
     *     {
     *       "name":"Iexperiment Name",
     *       "description":"Iexperiment Description"
     *     }
     * </pre>
     * Where "description" is not required "name" is not required Fields, that are not specified
     * will stay unchanged Method will produce following output:
     * <pre>
     *     {
     *      "description":"Iexperiment Description",
     *      "id":1,
     *      "name":"Iexperiment Name"
     *     }
     * </pre>
     */
    @PUT
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(#id, 'MANAGE_IEXPERIMENT')")
    @ApiOperation(value = "Update iexperiment", notes = "Updates an existing iexperiment.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 400, message = "If request is malformed"),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions")
    })
    Response update(
            @ApiParam(value = "Iexperiment body", defaultValue = "{}", required = true)
                    IexperimentUpdate iexperimentToUpdate,
            @ApiParam(name = "id", value = "Iexperiment identifier.", required = true)
            @PathParam("id")
                    long id);

    /**
     * Deletes iexperiment by specified id. If success, outputs empty response
     *
     * @param id iexperiment's id
     */
    @DELETE
    @Path("/{id}")
    @PreAuthorize("isAuthenticated() and hasPermission(#id, 'MANAGE_IEXPERIMENT')")
    @ApiOperation(value = "Delete iexperiment", notes = "Deletes an existing iexperiment.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses({
            @ApiResponse(code = 204, message = "If successful, this method returns an empty response body."),
            @ApiResponse(code = 401, message = "If request is not authorized"),
            @ApiResponse(code = 403, message = "If principal doesn't have permissions"),
            @ApiResponse(code = 404, message = "If iexperiment not found")
    })
    Response delete(
            @ApiParam(name = "id", value = "Iexperiment identifier.", required = true)
            @PathParam("id")
                    long id,
            @ApiParam(name = "force", value = "Force deletion flag.", defaultValue = "false")
            @QueryParam("force")
                    boolean force);

}
