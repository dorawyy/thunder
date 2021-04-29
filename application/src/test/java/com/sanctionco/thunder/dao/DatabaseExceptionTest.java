package com.sanctionco.thunder.dao;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseExceptionTest {

  @Test
  void testDatabaseExceptionCreation() {
    DatabaseException exception = new DatabaseException(DatabaseError.USER_NOT_FOUND);
    assertEquals(DatabaseError.USER_NOT_FOUND, exception.getErrorKind());
    assertEquals("An error occurred in the database interaction.", exception.getMessage());

    exception = new DatabaseException("Error", DatabaseError.CONFLICT);
    assertEquals(DatabaseError.CONFLICT, exception.getErrorKind());
    assertEquals("Error", exception.getMessage());

    exception = new DatabaseException("Error", new RuntimeException(), DatabaseError.DATABASE_DOWN);
    assertEquals(DatabaseError.DATABASE_DOWN, exception.getErrorKind());
    assertEquals("Error", exception.getMessage());
    assertEquals(RuntimeException.class, exception.getCause().getClass());
  }

  @Test
  void testResponse() {
    Response response = new DatabaseException("Error", DatabaseError.USER_NOT_FOUND).response();
    assertEquals(404, response.getStatus());
    assertEquals("User unknown not found in the database.", response.getEntity());

    response = new DatabaseException("Error", DatabaseError.USER_NOT_FOUND).response("test");
    assertEquals(404, response.getStatus());
    assertEquals("User test not found in the database.", response.getEntity());
  }
}
