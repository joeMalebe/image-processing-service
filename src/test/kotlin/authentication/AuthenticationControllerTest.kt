package com.example.authentication

import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// user must be able to register
// user must be able to log in
// user must receive a token when logging in
// unauthorized user cannot access protected routes


class AuthenticationControllerTest {

    val mockDb = mock<AuthenticationDatabase>()
    private val controller = AuthenticationController(mockDb)



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