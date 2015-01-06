/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.http;

import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.test.WithApplication;
import javaguide.testhelpers.MockJavaAction;

//#imports
import play.mvc.*;
import play.mvc.Http.*;
//#imports

import static javaguide.testhelpers.MockJavaActionHelper.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JavaBodyParsers extends WithApplication {

    @Test
    public void accessRequestBody() {
        assertThat(contentAsString(call(new MockJavaAction() {
            //#request-body
            public Result index() {
                RequestBody body = request().body();
                return ok("Got body: " + body);
            }
            //#request-body
        }, fakeRequest().withTextBody("foo"))), containsString("foo"));
    }

    @Test
    public void particularBodyParser() {
        assertThat(contentAsString(call(new MockJavaAction() {
                    //#particular-body-parser
                    @BodyParser.Of(BodyParser.Json.class)
                    public Result index() {
                        RequestBody body = request().body();
                        return ok("Got json: " + body.asJson());
                    }
                    //#particular-body-parser
                }, fakeRequest().withJsonBody(Json.toJson("foo")))),
                containsString("\"foo\""));
    }

    @Test
    public void defaultParser() {
        assertThat(status(call(new MockJavaAction() {
                    //#default-parser
                    public Result save() {
                        RequestBody body = request().body();
                        String textBody = body.asText();

                        if(textBody != null) {
                            return ok("Got: " + textBody);
                        } else {
                            return badRequest("Expecting text/plain request body");
                        }
                    }
                    //#default-parser
                }, fakeRequest().withJsonBody(Json.toJson("foo")))),
                equalTo(400));
    }

    @Test
    public void maxLength() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            body.append("1234567890");
        }
        assertThat(status(callWithStringBody(new MockJavaAction() {
                    //#max-length
                    // Accept only 10KB of data.
                    @BodyParser.Of(value = BodyParser.Text.class, maxLength = 10 * 1024)
                    public Result index() {
                        return ok("Got body: " + request().body().asText());
                    }
                    //#max-length
                }, fakeRequest(), body.toString())),
                equalTo(413));
    }

}
