/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms;

import com.google.common.collect.ImmutableMap;
import javaguide.forms.csrf.Global;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.CSRFFilter;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.Crypto;
import play.mvc.Result;
import play.test.WithApplication;
import play.test.FakeApplication;

import static play.test.Helpers.*;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import javaguide.forms.html.form;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCsrf extends WithApplication {
    @Override
    public FakeApplication provideFakeApplication() {
        return fakeApplication(ImmutableMap.of("application.secret", "foobar"));
    }

    public Crypto crypto() {
      return app.getWrappedApplication().injector().instanceOf(Crypto.class);
    }

    @Test
    public void global() {
        assertThat(new Global().filters()[0], equalTo((Class) CSRFFilter.class));
    }

    @Test
    public void templates() {
        String token = crypto().generateSignedToken();
        String body = contentAsString(MockJavaActionHelper.call(new MockJavaAction() {
            public Result index() {
                return ok(javaguide.forms.html.csrf.render());
            }
        }, fakeRequest("GET", "/").withSession("csrfToken", token)));

        Matcher matcher = Pattern.compile("action=\"/items\\?csrfToken=[a-f0-9]+-\\d+-([a-f0-9]+)\"")
                .matcher(body);
        assertTrue(matcher.find());
        assertThat(matcher.group(1), equalTo(crypto().extractSignedToken(token)));

        matcher = Pattern.compile("value=\"[a-f0-9]+-\\d+-([a-f0-9]+)\"")
                .matcher(body);
        assertTrue(matcher.find());
        assertThat(matcher.group(1), equalTo(crypto().extractSignedToken(token)));
    }

    @Test
    public void csrfCheck() {
        assertThat(status(MockJavaActionHelper.call(new Controller1(), fakeRequest("POST", "/")
                .withHeader(CONTENT_TYPE, "application/x-www-form-urlencoded"))), equalTo(FORBIDDEN));
    }

    public static class Controller1 extends MockJavaAction {
        //#csrf-check
        @RequireCSRFCheck
        public Result save() {
            // Handle body
            return ok();
        }
        //#csrf-check
    }

    @Test
    public void csrfAddToken() {
        assertThat(crypto().extractSignedToken(contentAsString(
                MockJavaActionHelper.call(new Controller2(), fakeRequest("GET", "/"))
        )), notNullValue());
    }

    public static class Controller2 extends MockJavaAction {
        //#csrf-add-token
        @AddCSRFToken
        public Result get() {
            return ok(form.render());
        }
        //#csrf-add-token
    }

}
