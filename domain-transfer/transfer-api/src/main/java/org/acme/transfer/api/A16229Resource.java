package org.acme.transfer.api;

import org.acme.transfer.api.dto.A16229Request;
import org.acme.transfer.api.dto.A16229Response;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/A16229")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class A16229Resource {

    @POST
    public Response execute(A16229Request request) {

        if (request == null) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new A16229Response("999", "Request不可為空", null))
                    .build();
        }

        if (request.AMT > 50) {
            String message = "成功";
            if (request.EC)
                message = "成功(EC)";
            A16229Response resp = new A16229Response(
                    "100",
                    message,
                    request.Account_A);

            return Response.ok(resp).build();
        }

        A16229Response resp = new A16229Response(
                "999",
                "AMT必須大於50",
                request.Account_A);

        return Response.status(Response.Status.FORBIDDEN)
                .entity(resp)
                .build();
    }
}