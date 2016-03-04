/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.services;

import io.netty.handler.codec.http.HttpRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.andes.kernel.Andes;
import org.wso2.andes.kernel.AndesContext;
import org.wso2.andes.kernel.DestinationType;
import org.wso2.carbon.andes.services.exceptions.BrokerManagerException;
import org.wso2.carbon.andes.services.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.services.exceptions.MessageManagerException;
import org.wso2.carbon.andes.services.exceptions.SubscriptionManagerException;
import org.wso2.carbon.andes.services.types.BrokerInformation;
import org.wso2.carbon.andes.services.types.ClusterInformation;
import org.wso2.carbon.andes.services.types.Destination;
import org.wso2.carbon.andes.services.types.DestinationRolePermission;
import org.wso2.carbon.andes.services.types.ErrorResponse;
import org.wso2.carbon.andes.services.types.Message;
import org.wso2.carbon.andes.services.types.StoreInformation;
import org.wso2.carbon.andes.services.types.Subscription;
import org.wso2.msf4j.Microservice;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Andes REST service is a microservice built on top of WSO2 msf4j. The REST service provides the capability of managing
 * resources of the WSO2 message broker. Resources being destinations, subscriptions and messages.
 */
@Component(
        name = "org.wso2.carbon.andes.AndesService",
        service = Microservice.class,
        immediate = true
)

@SwaggerDefinition(
        info = @Info(
                title = "WSO2 Message Broker REST Service", version = "v1.0.0",
                description = "WSO2 Message Broker REST Service for managing resources such as destinations, " +
                              "permissions, messages and subscriptions.",
                license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0"),
                contact = @Contact(
                        name = "WSO2",
                        url = "http://wso2.com"
                )),
        tags = {@Tag(name = "Destinations", description = "Operations on handling destination related resources."),
                @Tag(name = "Permissions", description = "Operations on handling permission related resources."),
                @Tag(name = "Messages", description = "Operations on handling message related resources."),
                @Tag(name = "Subscriptions", description = "Operations on handling subscription related resources."),
                @Tag(name = "Node Details", description = "Operations on getting node details.")},
        schemes = SwaggerDefinition.Scheme.HTTPS)
@Api(value = "/mb/api/", description = "Endpoint to WSO2 message broker REST services.",
     produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
@Path("/mb/api/")
public class AndesService implements Microservice {
    private static final Logger log = LoggerFactory.getLogger(AndesService.class);
    /**
     * Bundle registration service for andes REST service.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Service class for managing destinations.
     */
    private DestinationManagerService destinationManagerService;

    /**
     * Service class for managing subscriptions.
     */
    private SubscriptionManagerService subscriptionManagerService;

    /**
     * Service class for managing message information.
     */
    private MessageManagerService messageManagerService;

    /**
     * Service class for managing broker details.
     */
    private BrokerManagerService brokerManagerService;

    /**
     * Initializes the service classes for resources.
     */
    public AndesService() {
        destinationManagerService = new DestinationManagerServiceImpl();
        subscriptionManagerService = new SubscriptionManagerServiceImpl();
        messageManagerService = new MessageManagerServiceImpl();
        brokerManagerService = new BrokerManagerServiceImpl();
    }

    /**
     * Gets the protocol types supported by the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/protocol-types
     * </pre>
     *
     * @return Return a collection of supported protocol. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of protocols as a
     *     response.</li>
     * </ul>
     */
    @GET
    @Path("/protocol-types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets supported protocols.",
            notes = "Gets supported protocols by the broker.",
            tags = "Protocol",
            response = String.class,
            responseContainer = "List")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List of protocols.")})
    public List<String> getProtocols() {
        return AndesContext.getInstance().getAndesContextStore().getProtocols().stream()
                .map(protocolType ->  protocolType.toString())
                .collect(Collectors.toList());
    }

    /**
     * Gets the destination types supported by the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/destination-types
     * </pre>
     *
     * @return Return a collection of supported destinations. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of destinations as a
     *     response.</li>
     * </ul>
     */
    @GET
    @Path("/destination-types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets supported destinations.",
            notes = "Gets supported destinations by the broker.",
            tags = "Destinations",
            response = String.class,
            responseContainer = "List")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List of destinations.")})
    public List<String> getDestinationTypes() {
        return Stream.of(DestinationType.values())
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.toList());
    }

    /**
     * Gets destinations that belongs to a specific protocol and destination type.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/queue
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/topic
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/durable_topic
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/queue?name=MyQueue&offset=5&limit=3
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType The destination type of the destination as {@link org.wso2.andes.kernel.DestinationType}.
     * @param destinationName The name of the destination. If "*", all destinations are returned, else destinations that
     *                        <strong>contains</strong> the value will be returned.
     * @param offset          The starting index of the return destination list for pagination. Default value is 0.
     * @param limit           The number of destinations to return for pagination. Default value is 20.
     * @return Return a collection of {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Destination} as a
     *     response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destinations from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets destinations.",
            notes = "Gets destinations that belongs to a specific protocol and destination type. Supports pagination.",
            tags = "Destinations",
            response = Destination.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful list of destinations.", response = Destination.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDestinations(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination. If \"*\", all destinations are returned, else " +
                              "destinations that contains the value will be returned.")
            @DefaultValue("*") @QueryParam("name") String destinationName,
            @ApiParam(value = "The starting index of the return destination list for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("0") @QueryParam("offset") int offset,
            @ApiParam(value = "The number of destinations to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("20") @QueryParam("limit") int limit) {
        try {
            List<Destination> destinations = destinationManagerService.getDestinations(protocol, destinationType,
                    destinationName, offset, limit);
            return Response.status(Response.Status.OK).entity(destinations).build();
        } catch (DestinationManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Deletes all destinations belonging to a specific protocol and destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/queue
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/durable_topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/mqtt/destination-type/topic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType The destination type of the destination as {@link org.wso2.andes.kernel.DestinationType}.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Destinations were successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     destinations from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}")
    @ApiOperation(
            value = "Deletes destinations.",
            notes = "Deletes destinations that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Destinations deleted.", response = Destination.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteDestinations(
            @ApiParam(value = "Protocol for the destination")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination")
            @PathParam("destination-type") String destinationType
    ) {
        try {
            destinationManagerService.deleteDestinations(protocol, destinationType);
            return Response.status(Response.Status.OK).build();
        } catch (DestinationManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Gets a specific destination belonging to a specific protocol and destination type. Topic
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/queue/name/MyQueue
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/topic/name/MyTopic
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/destination-type/durable_topic/name/MyDurable
     *  curl -v http://127.0.0.1:9443/mb/api/mqtt/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType The destination type of the destination as {@link org.wso2.andes.kernel.DestinationType}.
     * @param destinationName The name of the destination.
     * @return A JSON representation of {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destination from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets a destination.",
            notes = "Gets a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Destination returned.", response = Destination.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName) {
        try {
            Destination newDestination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != newDestination) {
                return Response.status(Response.Status.OK).entity(newDestination).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (DestinationManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Creates a new destination.  A topic will be created even if "durable_topic" is requested as the destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X POST http://127.0.0.1:9443/mb/api/amqp/destination-type/queue
     *  curl -v -X POST http://127.0.0.1:9443/mb/api/amqp/destination-type/topic
     *  curl -v -X POST http://127.0.0.1:9443/mb/api/amqp/destination-type/durable_topic
     *  curl -v -X POST http://127.0.0.1:9443/mb/api/mqtt/destination-type/topic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType The destination type of the destination as {@link org.wso2.andes.kernel.DestinationType}.
     *                        "durable_topic" is considered as a topic.
     * @param destination     The destination object. {@link Destination#destinationName} is required.
     * @return A JSON representation of the newly created {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error while creating new destination
     *     .</li>
     * </ul>
     */
    @POST
    @Path("/{protocol}/destination-type/{destination-type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a destination.",
            notes = "Creates a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations",
            response = Destination.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New destination successfully created.", response = Destination.class),
            @ApiResponse(code = 500, message = "Server error on creating destination", response = ErrorResponse.class)})
    public Response createDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination. \"durable_topic\" is considered as a topic.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "Destination object.")
            Destination destination,
            @Context HttpRequest request) {
        try {
            Destination newDestination = destinationManagerService.createDestination(protocol, destinationType,
                    destination.getDestinationName());

            return Response.status(Response.Status.OK)
                    .entity(newDestination)
                    .header("Location", request.getUri() + "/name/" + destination.getDestinationName())
                    .build();
        } catch (DestinationManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Deletes destination. A topic will be deleted even if "durable_topic" is requested as the destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/queue/name/MyQueue
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/topic/name/MyTopic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/durable_topic/name/MyDurable
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/mqtt/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType The destination type of the destination a {@link org.wso2.andes.kernel.DestinationType}.
     *                        "durable_topic" is considered as a topic.
     * @param destinationName The name of the destination to delete.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Destination was successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     destination from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes a destination.",
            notes = "Deletes a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Destination deleted.", response = Destination.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination. \"durable_topic\" is considered as a topic.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination")
            @PathParam("destination-name") String destinationName) {
        try {
            destinationManagerService.deleteDestination(protocol, destinationType, destinationName);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (DestinationManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Gets the permissions available for a destination.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/permissions/destination-type/queue/name/MyQueue
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/permissions/destination-type/topic/name/MyTopic
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/permissions/destination-type/durable_topic/name/MyDurable
     *  curl -v http://127.0.0.1:9443/mb/api/mqtt/permissions/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType The destination type of the destination as {@link org.wso2.andes.kernel.DestinationType}.
     *                        "durable_topic" is considered as a topic.
     * @param destinationName The name of the destination.
     * @return Return a collection of {@link DestinationRolePermission}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link DestinationRolePermission}
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     permissions from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/permissions/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets permission assigned to a destination.",
            notes = "Gets all the role based permissions assigned to a specific destination.",
            tags = {"Destinations", "Permissions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Gets list of permissions.", response = DestinationRolePermission.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDestinationPermissions(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName) {
        Set<DestinationRolePermission> permissions = destinationManagerService.getDestinationPermissions(protocol,
                destinationType, destinationName);
        return Response.status(Response.Status.OK).entity(permissions).build();
    }

    /**
     * Creates the permission assigned to a roles.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X POST \
     *          -H "Content-Type:application/json" \
     *          -d '{"role" : "abc", "consume" : true, "publish" : false}' \
     *          http://127.0.0.1:9443/mb/api/amqp/permissions/destination-type/queue/name/MyQueue
     *
     * </pre>
     *
     * @param protocol                      The protocol type of the destination as
     *                                      {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType               The destination type of the destination as
     *                                      {@link org.wso2.andes.kernel.DestinationType}. "durable_topic" is considered
     *                                      as a topic.
     * @param destinationName               The name of the destination.
     * @param newDestinationRolePermissions The new permission assigned to the role.
     * @return Return the newly created {@link DestinationRolePermission}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link DestinationRolePermission} as a JSON
     *     response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when creating
     *     the permission from the server.</li>
     * </ul>
     */
    @POST
    @Path("/{protocol}/permissions/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates new role permissions.",
            notes = "Creates new role permissions for a destination.",
            tags = {"Destinations", "Permissions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New permission created.", response = DestinationRolePermission.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response createDestinationPermission(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName,
            // Payload
            @ApiParam(value = "New role permission payload.")
            DestinationRolePermission newDestinationRolePermissions) {
        DestinationRolePermission newPermission = destinationManagerService.createDestinationPermission
                (protocol, destinationType, destinationName, newDestinationRolePermissions);
        return Response.status(Response.Status.OK).entity(newPermission).build();
    }

    /**
     * Updates the permission assigned to a roles.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X PUT \
     *          -H "Content-Type:application/json" \
     *          -d '{"role" : "abc", "consume" : true, "publish" : false}' \
     *          http://127.0.0.1:9443/mb/api/amqp/permissions/destination-type/queue/name/MyQueue
     *
     * </pre>
     *
     * @param protocol                          The protocol type of the destination as
     *                                          {@link org.wso2.andes.kernel.ProtocolType}.
     * @param destinationType                   The destination type of the destination as
     *                                          {@link org.wso2.andes.kernel.DestinationType}. "durable_topic" is
     *                                          considered as a topic.
     * @param destinationName                   The name of the destination.
     * @param updatedDestinationRolePermissions The updates permission assigned to the role.
     * @return Return the updated {@link DestinationRolePermission}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link DestinationRolePermission} as a JSON
     *     response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when updating
     *     the permission from the server.</li>
     * </ul>
     */
    @PUT
    @Path("/{protocol}/permissions/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates role permissions.",
            notes = "Updates role permissions for a destination.",
            tags = {"Destinations", "Permissions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Permission updated.", response = DestinationRolePermission.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response updateDestinationPermission(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName,
            // Payload
            @ApiParam(value = "New role permission payload.")
            DestinationRolePermission updatedDestinationRolePermissions) {
        DestinationRolePermission updatedPermission = destinationManagerService.updateDestinationPermission
                (protocol, destinationType, destinationName, updatedDestinationRolePermissions);
        return Response.status(Response.Status.OK).entity(updatedPermission).build();
    }

    /**
     * Gets subscriptions belonging to a specific protocol type and destination type. The subscriptions can be filtered
     * by subscription name, destination name and whether they are active or not.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/subscription-type/queue?destination=MyQueue&offset=2&limit=5
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/subscription-type/topic
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=true
     *  curl -v http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol         The protocol type matching for the subscription. Example : amqp, mqtt.
     * @param subscriptionType The destination type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param subscriptionName The name of the subscription. If "*", all subscriptions are included. Else subscriptions
     *                         that <strong>contains</strong> the value are included.
     * @param destinationName  The name of the destination name. If "*", all destinations are included. Else
     *                         destinations that <strong>equals</strong> the value are included.
     * @param active           Filtering the subscriptions that are active or inactive. Supported values = "*", "true"
     *                         and "false".
     * @param offset           The starting index to return.
     * @param limit            The number of subscriptions to return.
     * @return Return a collection of {@link Subscription}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Subscription}
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     subscriptions from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/subscription-type/{subscription-type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get subscriptions.",
            notes = "Get subscriptions that belongs to a specific protocol and subscription type. Supports pagination.",
            tags = "Subscriptions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully gets subscriptions.", response = Subscription.class),
            @ApiResponse(code = 400, message = "Invalid protocol or subscription type.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getSubscriptions(
            @ApiParam(value = "Protocol for the subscription.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "The type of subscription.")
            @PathParam("subscription-type") String subscriptionType,
            @ApiParam(value = "The name of the subscription. If \"*\", all subscriptions are included. Else " +
                              "subscriptions that CONTAINS the value are included.")
            @DefaultValue("*") @QueryParam("name") String subscriptionName,
            @ApiParam(value = "The name of the destination name. If \"*\", all destinations are included. Else" +
                              "destinations that EQUALS the value are included.")
            @DefaultValue("*") @QueryParam("destination") String destinationName,
            @ApiParam(value = "Filtering the subscriptions that are active or inactive.",
                      allowableValues = "[*, true, false]")
            @DefaultValue("*") @QueryParam("active") String active,
            @ApiParam(value = "The starting index of the return destination list for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("0") @QueryParam("offset") int offset,
            @ApiParam(value = "The number of destinations to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("20") @QueryParam("limit") int limit) {
        try {
            List<Subscription> subscriptions = subscriptionManagerService.getSubscriptions
                    (protocol, subscriptionType, subscriptionName, destinationName, active, offset, limit);
            return Response.status(Response.Status.OK).entity(subscriptions).build();
        } catch (SubscriptionManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Close/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/queue?destination=MyQueue
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=true
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol         The protocol type matching for the subscription. Example : amqp, mqtt.
     * @param subscriptionType The subscription type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param destinationName  The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                         Else destinations that <strong>contains</strong> the value are included.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Subscriptions were successfully closed/disconnected.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while
     *     closing/disconnecting a subscriptions from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/subscription-type/{subscription-type}")
    @ApiOperation(
            value = "Close subscriptions.",
            notes = "Closes subscriptions that belongs to a specific protocol and subscription type.",
            tags = "Subscriptions")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Subscriptions successfully close.", response = Subscription.class),
        @ApiResponse(code = 400, message = "Invalid protocol or subscription type.", response = ErrorResponse.class),
        @ApiResponse(code = 404, message = "Destinations not found.", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response closeSubscriptions(
            @ApiParam(value = "Protocol for the subscription.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "The type of subscription.")
            @PathParam("subscription-type") String subscriptionType,
            @ApiParam(value = "The name of the destination to close/unsubscribe. If \"*\", all destinations are " +
                              "included. Else destinations that CONTAINS the value are included.")
            @DefaultValue("*") @QueryParam("destination") String destinationName,
            @DefaultValue("false") @QueryParam("unsubscribe-only") boolean unsubscribeOnly) {
        try {
            subscriptionManagerService.closeSubscriptions(protocol, subscriptionType, destinationName, unsubscribeOnly);
            return Response.status(Response.Status.OK).build();
        } catch (SubscriptionManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Close/Unsubscribe subscription forcefully belonging to a specific protocol type, destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/queue/subscription-id/sub1
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic/subscription-id/sub1
     * </pre>
     *
     * @param protocol         The protocol type matching for the subscription. Example : amqp, mqtt.
     * @param subscriptionType The subscription type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param subscriptionID  The subscription ID to close/unsubscribe.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Subscription was successfully closed/disconnected.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Subscription was not found to close/disconnect.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while
     *     closing/disconnecting a subscriptions from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/subscription-type/{subscription-type}/subscription-id/{subscription-id}")
    @ApiOperation(
            value = "Close subscription.",
            notes = "Closes subscription that belongs to a specific protocol and subscription type.",
            tags = "Subscriptions")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Subscription successfully close.", response = Subscription.class),
        @ApiResponse(code = 400, message = "Invalid protocol or subscription type.", response = ErrorResponse.class),
        @ApiResponse(code = 404, message = "Subscription not found.", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response closeSubscription(
            @ApiParam(value = "Protocol for the subscription.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "The type of subscription.")
            @PathParam("subscription-type") String subscriptionType,
            @ApiParam(value = "The subscription ID.")
            @PathParam("subscription-id") String subscriptionID,
            @DefaultValue("false") @QueryParam("unsubscribe-only") boolean unsubscribeOnly) {
        try {
            subscriptionManagerService.closeSubscription(protocol, subscriptionType, subscriptionID,
                    unsubscribeOnly);
            return Response.status(Response.Status.OK).build();
        } catch (SubscriptionManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Browse message of a destination using message ID.
     * <p>
     * To browse messages without message ID, use {@link MessageManagerService#getMessagesOfDestinationByOffset(String,
     * String, String, boolean, int, int)}.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/queue?destination=MyQueue
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=true
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination.
     * @param content         Whether to return message content or not.
     * @param nextMessageID   The starting message ID to return from.
     * @param limit           The number of messages to return.
     * @return Return a collection of {@link Message}s. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Message}s
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     messages from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets message by message ID.",
            notes = "Gets message that belongs to a specific protocol,destination type and destination name. " +
                    "Supports pagination.",
            tags = "Messages",
            response = Message.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful list of messages.", response = Message.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessagesOfDestinationByMessageID(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName,
            @ApiParam(value = "Whether to return message content or not.", allowableValues = "[true, false]")
            @DefaultValue("false") @QueryParam("content") boolean content,
            @ApiParam(value = "The starting message ID to return from.")
            @DefaultValue("0") @QueryParam("next-message-id") long nextMessageID,
            @ApiParam(value = "The number of messages to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("100") @QueryParam("limit") int limit) {
        try {
            List<Message> messages = messageManagerService.getMessagesOfDestinationByMessageID(protocol,
                    destinationType, destinationName, content, nextMessageID, limit);
            return Response.status(Response.Status.OK).entity(messages).build();
        } catch (MessageManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Browse message of a destination. Please note this is time costly.
     * <p>
     * To browse messages with message ID, use {@link MessageManagerService#getMessagesOfDestinationByMessageID
     * (String, String, String, boolean, long, int)}.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/queue?destination=MyQueue
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=true
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param offset          Starting index of the messages to return.
     * @param limit           The number of messages to return.
     * @return Return a collection of {@link Message}s. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Message}s
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     messages from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets message by offset.",
            notes = "Gets message that belongs to a specific protocol,destination type and destination name. " +
                    "Supports pagination.",
            tags = "Messages",
            response = Message.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful list of messages.", response = Message.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessagesOfDestinationByOffset(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName,
            @ApiParam(value = "Whether to return message content or not.", allowableValues = "[true, false]")
            @DefaultValue("false") @QueryParam("content") boolean content,
            @ApiParam(value = "Starting index of the messages to return.")
            @DefaultValue("0") @QueryParam("offset") int offset,
            @ApiParam(value = "The number of messages to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("100") @QueryParam("limit") int limit) {
        try {
            List<Message> messages = messageManagerService.getMessagesOfDestinationByOffset(protocol,
                    destinationType, destinationName, content, offset, limit);
            return Response.status(Response.Status.OK).entity(messages).build();
        } catch (MessageManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Gets a message by message ID belonging to a particular protocol, destination type and destination name.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/queue?destination=MyQueue
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=true
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to which the message belongs to.
     * @param andesMessageID  The message ID. This message is the andes metadata message ID.
     * @param content         Whether to return content or not.
     * @return A JSON representation of {@link Message}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Message} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such message does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     message from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages/{message-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets a message.",
            notes = "Gets a message using message ID.",
            tags = "Messages",
            response = Message.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful received message.", response = Message.class),
            @ApiResponse(code = 400, message = "Invalid protocol,destination type or destination name.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Message not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessage(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName,
            @ApiParam(value = "The andes message ID.")
            @PathParam("message-id") String andesMessageID,
            @ApiParam(value = "Whether to return message content or not.", allowableValues = "[true, false]")
            @DefaultValue("false") @QueryParam("content") boolean content) {
        try {
            Message message = messageManagerService.getMessage(protocol, destinationType, destinationName,
                    andesMessageID, content);
            return Response.status(Response.Status.OK).entity(message).build();
        } catch (MessageManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Purge all messages belonging to a destination.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/destination-type/queue/name/MyQueue/messages
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=true
     *  curl -v -X DELETE http://127.0.0.1:9443/mb/api/amqp/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to purge messages.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Messages were successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     messages from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages")
    @ApiOperation(
            value = "Deletes/Purge message.",
            notes = "Deletes/Purge message belonging to a specific destination.",
            tags = "Messages")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Messages purged successfully.", response = Message.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteMessages(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName) {
        try {
            messageManagerService.deleteMessages(protocol, destinationType, destinationName);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (MessageManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Gets clustering related information of the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/information/cluster
     * </pre>
     *
     * @return Return a {@link ClusterInformation}. <p>
     * <ul>
     *      <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link ClusterInformation} as a response.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *      clustering details from the server.</li>
     * </ul>
     */
    @GET
    @Path("/information/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets cluster details.",
            notes = "Gets cluster details which includes node details as well..",
            tags = "Node Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully received cluster information.",
                         response = ClusterInformation.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getClusterInformation() {
        try {
            ClusterInformation clusterInformation = brokerManagerService.getClusterInformation();
            return Response.status(Response.Status.OK).entity(clusterInformation).build();
        } catch (BrokerManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Gets message store related information of the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/information/store
     * </pre>
     *
     * @return Return a {@link StoreInformation}. <p>
     * <ul>
     *      <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link StoreInformation} as a response.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *      message store details from the server.</li>
     * </ul>
     */
    @GET
    @Path("/information/store")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets store details.",
            notes = "Gets message store details.",
            tags = "Node Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully received store information.",
                         response = StoreInformation.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getStoreInformation() {
        try {
            StoreInformation storeInformation = brokerManagerService.getStoreInformation();
            return Response.status(Response.Status.OK).entity(storeInformation).build();
        } catch (BrokerManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Gets broker configuration related information.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9443/mb/api/information/broker
     * </pre>
     *
     * @return Return a {@link BrokerInformation}. <p>
     * <ul>
     *      <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link BrokerInformation} as a response.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *      broker details from the server.</li>
     * </ul>
     */
    @GET
    @Path("/information/broker")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets broker properties.",
            notes = "Gets current broker node details.",
            tags = "Node Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully received broker information.",
                         response = BrokerInformation.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getBrokerInformation() {
        try {
            BrokerInformation brokerInformation = brokerManagerService.getBrokerInformation();
            return Response.status(Response.Status.OK).entity(brokerInformation).build();
        } catch (BrokerManagerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    public void setDestinationManagerService(DestinationManagerService destinationManagerService) {
        this.destinationManagerService = destinationManagerService;
    }

    /**
     * Called when the bundle is activated.
     *
     * @param bundleContext Bundle of the component.
     */
    @Activate
    protected void start(BundleContext bundleContext) {
        serviceRegistration = bundleContext.registerService(AndesService.class.getName(), this, null);
        log.info("Andes REST Service has started successfully.");
    }

    /**
     * Called when the bundle is deactivated.
     *
     * @param bundleContext Bundle of the component.
     */
    @Deactivate
    protected void stop(BundleContext bundleContext) {
        serviceRegistration.unregister();
        log.info("Andes REST Service has been deactivated.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Andes-REST-Service-OSGi-Bundle";
    }

    /**
     * This bind method will be called when CarbonRuntime OSGi service is registered.
     *
     * @param andesInstance The CarbonRuntime instance registered by Carbon Kernel as an OSGi service
     */
    @Reference(
            name = "carbon.andes.service",
            service = Andes.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAndesRuntime"
    )
    protected void setAndesRuntime(Andes andesInstance) {
    }

    /**
     * This is the unbind method which gets called at the un-registration of CarbonRuntime OSGi service.
     *
     * @param andesInstance The CarbonRuntime instance registered by Carbon Kernel as an OSGi service
     */
    @SuppressWarnings("unused")
    protected void unsetAndesRuntime(Andes andesInstance) {

    }
}
