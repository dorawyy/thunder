package com.sanctionco.thunder;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanctionco.thunder.dao.DatabaseHealthCheck;
import com.sanctionco.thunder.dao.UsersDao;
import com.sanctionco.thunder.dao.UsersDaoFactory;
import com.sanctionco.thunder.email.EmailHealthCheck;
import com.sanctionco.thunder.email.EmailService;
import com.sanctionco.thunder.email.EmailServiceFactory;
import com.sanctionco.thunder.openapi.OpenApiBundle;
import com.sanctionco.thunder.openapi.OpenApiConfiguration;
import com.sanctionco.thunder.resources.UserResource;
import com.sanctionco.thunder.resources.VerificationResource;
import com.sanctionco.thunder.secrets.SecretSourceProvider;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThunderApplicationTest {
  private static final ThunderConfiguration CONFIG = spy(ThunderConfiguration.class);

  private static final EmailServiceFactory EMAIL_FACTORY = mock(EmailServiceFactory.class);
  private static final EmailService EMAIL_SERVICE = mock(EmailService.class);
  private static final EmailHealthCheck EMAIL_HEALTH_CHECK = mock(EmailHealthCheck.class);

  private static final UsersDaoFactory DAO_FACTORY = mock(UsersDaoFactory.class);
  private static final DatabaseHealthCheck DATABASE_HEALTH_CHECK = mock(DatabaseHealthCheck.class);
  private static final UsersDao USERS_DAO = mock(UsersDao.class);

  private final ThunderApplication application = new ThunderApplication();

  @BeforeAll
  static void setup() {
    when(EMAIL_FACTORY.isEnabled()).thenReturn(true);
    when(EMAIL_FACTORY.createEmailService(any(MetricRegistry.class))).thenReturn(EMAIL_SERVICE);
    when(EMAIL_FACTORY.createHealthCheck()).thenReturn(EMAIL_HEALTH_CHECK);

    when(DAO_FACTORY.createHealthCheck()).thenReturn(DATABASE_HEALTH_CHECK);
    when(DAO_FACTORY.createUsersDao(any(ObjectMapper.class))).thenReturn(USERS_DAO);

    when(CONFIG.getUsersDaoFactory()).thenReturn(DAO_FACTORY);
    when(CONFIG.getEmailServiceFactory()).thenReturn(EMAIL_FACTORY);
  }

  @Test
  void testMain() throws Exception {
    var captor = ArgumentCaptor.forClass(String.class);

    try (MockedConstruction<ThunderApplication> ignored = mockConstruction(ThunderApplication.class,
        (appMock, context) -> doNothing().when(appMock).run(captor.capture()))) {
      ThunderApplication.main(new String[]{"Arg1", "Arg2"});

      assertEquals(2, captor.getAllValues().size());
      assertEquals("Arg1", captor.getAllValues().get(0));
      assertEquals("Arg2", captor.getAllValues().get(1));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testInitialize() {
    var captor = ArgumentCaptor.forClass(OpenApiBundle.class);
    var bootstrap = mock(Bootstrap.class);

    application.initialize(bootstrap);

    // Verify OpenApiBundle was added
    verify(bootstrap, times(1)).addBundle(captor.capture());

    // Verify getOpenApiConfiguration works
    OpenApiConfiguration openApiConfiguration = captor.getValue().getOpenApiConfiguration(CONFIG);
    assertTrue(openApiConfiguration.isEnabled());

    // Verify source provider was set
    var sourceCaptor = ArgumentCaptor.forClass(SecretSourceProvider.class);

    verify(bootstrap, times(1)).setConfigurationSourceProvider(sourceCaptor.capture());
    assertNotNull(sourceCaptor.getValue());
  }

  @Test
  void testRun() {
    var environment = mock(Environment.class);
    var jersey = mock(JerseyEnvironment.class);
    var healthChecks = mock(HealthCheckRegistry.class);
    var metrics = mock(MetricRegistry.class);

    when(environment.jersey()).thenReturn(jersey);
    when(environment.healthChecks()).thenReturn(healthChecks);
    when(environment.metrics()).thenReturn(metrics);

    var captor = ArgumentCaptor.forClass(Object.class);

    application.run(CONFIG, environment);

    // Verify register was called on jersey and healthChecks
    verify(jersey, atLeastOnce()).register(captor.capture());
    verify(healthChecks, times(1)).register(eq("Database"), any(DatabaseHealthCheck.class));
    verify(healthChecks, times(1)).register(eq("Email"), any(EmailHealthCheck.class));

    // Make sure each class that should have been registered on jersey was registered
    List<Object> values = captor.getAllValues();

    assertAll("Assert all objects were registered to Jersey",
        () -> assertEquals(1,
            values.stream().filter(v -> v instanceof AuthDynamicFeature).count()),
        () -> assertEquals(1,
            values.stream().filter(v -> v instanceof UserResource).count()),
        () -> assertEquals(1,
            values.stream().filter(v -> v instanceof VerificationResource).count()));
  }

  @Test
  void testRunWithoutVerification() {
    var environment = mock(Environment.class);
    var jersey = mock(JerseyEnvironment.class);
    var healthChecks = mock(HealthCheckRegistry.class);
    var metrics = mock(MetricRegistry.class);
    var config = spy(ThunderConfiguration.class);

    when(environment.jersey()).thenReturn(jersey);
    when(environment.healthChecks()).thenReturn(healthChecks);
    when(environment.metrics()).thenReturn(metrics);

    when(config.getUsersDaoFactory()).thenReturn(DAO_FACTORY);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

    application.run(config, environment);

    // Verify register was called on jersey and healthChecks
    verify(jersey, atLeastOnce()).register(captor.capture());
    verify(healthChecks, times(1)).register(eq("Database"), any(DatabaseHealthCheck.class));
    verify(healthChecks, times(0)).register(eq("Email"), any(EmailHealthCheck.class));

    // Make sure each class that should have been registered on jersey was registered
    List<Object> values = captor.getAllValues();

    assertAll("Assert all objects were registered to Jersey",
        () -> assertEquals(1,
            values.stream().filter(v -> v instanceof AuthDynamicFeature).count()),
        () -> assertEquals(1,
            values.stream().filter(v -> v instanceof UserResource).count()),
        () -> assertEquals(0,
            values.stream().filter(v -> v instanceof VerificationResource).count()));
  }
}
