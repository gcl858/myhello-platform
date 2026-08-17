package org.acme.transfer.api;

import org.acme.transfer.api.dto.A16220Request;
import org.acme.transfer.api.dto.A16220Response;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/A16220")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class A16220Resource {

    @POST
    public Response execute(A16220Request request) {

        if (request == null) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new A16220Response("999", "Request不可為空", null))
                    .build();
        }

        if (request.AMT < 1000) {
            String message = "成功";
            if (request.EC)
                message = "成功(EC)";
            A16220Response resp = new A16220Response(
                    "100",
                    message,
                    request.Account_B);

            return Response.ok(resp).build();
        }

        A16220Response resp = new A16220Response(
                "999",
                "AMT必須小於1000",
                request.Account_B);

        return Response.status(Response.Status.FORBIDDEN)
                .entity(resp)
                .build();
    }
}