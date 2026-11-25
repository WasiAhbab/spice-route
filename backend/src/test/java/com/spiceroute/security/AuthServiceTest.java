package com.spiceroute.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthServiceTest {

  @Test
  void hashesAreStableAndNotPlaintext() {
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      AuthService.hash("abc")
    );
    assertNotEquals(AuthService.hash("abc"), AuthService.hash("abd"));
  }
}
