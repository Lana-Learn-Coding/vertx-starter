package com.example.vet.validation;

import com.example.vet.model.User;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.RoutingContext;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;


public class UserValidationHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (body == null) {
            context.next();
            return;
        }

        User user = body.mapTo(User.class);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        if (violations.isEmpty()) {
            // remove un-mapped fields, then forward
            context.setBody(Buffer.buffer(JsonObject.mapFrom(user).toString()));
            context.next();
            return;
        }

        JsonObject errors = new JsonObject();
        violations.forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        context.response()
            .setStatusCode(400)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errors.encode());
    }
}
