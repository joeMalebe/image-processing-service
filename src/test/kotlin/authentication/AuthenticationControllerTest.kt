package com.example.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// user must be able to register
// user must be able to log in
// user must receive a token when logging in
// unauthorized user cannot access protected routes


class AuthenticationControllerTest {

    private val controller = AuthenticationController(mutableMapOf())

    @Test
    fun `add user to db when sign up`() {
        controller.signUp(username = "test", password ="password")

        assertTrue(controller.database.containsKey("test"))
        assertEquals( "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", controller.database["test"])
    }

    @Test
    fun `when user is signed up then login succeeds`() {
        controller.signUp(username = "new", password ="user")

        val result: Boolean = controller.login(username = "new", password = "user")

        assertTrue { result }
    }

    @Test
    fun `when user is not signed up then login fails`() {
        controller.signUp(username = "new", password ="user")

        val result = controller.login("wrong", "user")

        assertFalse { result }
    }

    @Test
    fun `when username is correct but password is incorrect then login fails`() {
        controller.signUp(username = "hello", password ="user")

        val result = controller.login("hello", "wrong")

        assertFalse { result }
    }

    @Test
    fun `when username is blank then login fails`() {
        val result = controller.signUp(username = "", password ="user")

        assertFalse { result }
    }

    @Test
    fun `when password is blank then login fails`() {
        val result = controller.signUp(username = "jack", password ="")

        assertFalse { result }
    }

}