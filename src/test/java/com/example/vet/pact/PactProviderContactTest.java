package com.example.vet.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.example.vet.VetMain;
import io.vertx.core.Vertx;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(PactRunner.class)
@Provider("My Provider")
@PactFolder("build/pacts")
public class PactProviderContactTest {
    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Vertx.vertx().deployVerticle(new VetMain(), result -> {
            if (result.failed()) {
                throw new RuntimeException("Failed to start vertx: " + result.cause());
            } else {
                done.countDown();
            }
        });
        done.await();
    }

    @TestTarget
    public final Target target = new HttpTarget("http", "localhost", 8800);

    @State("test GET")
    public void testGetState() {
    }

    @State("test POST")
    public void testPostState() {
    }

    @State("test POST invalid")
    public void testPostInvalid() {

    }
}
