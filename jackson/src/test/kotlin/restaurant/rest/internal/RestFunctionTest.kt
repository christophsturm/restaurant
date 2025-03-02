package restaurant.rest.internal

import failgood.Test
import failgood.testCollection
import restaurant.MutableRequestContext
import restaurant.RequestContext
import restaurant.rest.RestService
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.message

@Test
class RestFunctionTest {
    val context =
        testCollection(RestFunction::class) {
            data class Body(val field: String)

            val requestContext = MutableRequestContext()
            describe("parameter type") {
                it("detects parameter type for methods with only the body parameter") {
                    class A : RestService {
                        @Suppress("UNUSED_PARAMETER") fun create(body: Body) = Unit
                    }
                    expectThat(RestFunction(A::create, A()))
                        .get { payloadType }
                        .isEqualTo(Body::class.java)
                }
                describe("detects parameter type for methods with body parameter and id") {
                    it("when body is first") {
                        @Suppress("UNUSED_PARAMETER")
                        class A : RestService {
                            fun update(body: Body, id: String) = Unit
                        }
                        expectThat(RestFunction(A::update, A()))
                            .get { payloadType }
                            .isEqualTo(Body::class.java)
                    }
                    it("when id is first") {
                        @Suppress("UNUSED_PARAMETER")
                        class A : RestService {
                            fun update(id: String, body: Body) = Unit
                        }
                        expectThat(RestFunction(A::update, A()))
                            .get { payloadType }
                            .isEqualTo(Body::class.java)
                    }
                }
            }
            describe("invoking the method") {
                it("can invoke a method with only an id parameter") {
                    class A : RestService {
                        fun get(id: String) = id
                    }

                    val subject = RestFunction(A::get, A())
                    expectThat(subject.callSuspend(null, "id", requestContext)).isEqualTo("id")
                }
                describe("id parameter type") {
                    it("can be Int") {
                        class A : RestService {
                            fun get(id: Int) = id
                        }

                        expectThat(
                                RestFunction(A::get, A()).callSuspend(null, "123", requestContext))
                            .isEqualTo(123)
                    }
                    it("can be Long") {
                        class A : RestService {
                            fun get(id: Long) = id
                        }

                        expectThat(
                                RestFunction(A::get, A()).callSuspend(null, "123", requestContext))
                            .isEqualTo(123L)
                    }
                }
                it("can invoke a method with only a body parameter") {
                    class A : RestService {
                        fun create(body: Body) = body
                    }

                    val subject = RestFunction(A::create, A())
                    expectThat(subject.callSuspend(Body("value"), null, requestContext))
                        .isEqualTo(Body("value"))
                }
                describe("invoking a method with body and a id parameter") {
                    it("supports declaring the body before the id") {
                        class A : RestService {
                            fun update(body: Body, id: Int) = Body(body.field + " with id " + id)
                        }

                        val subject = RestFunction(A::update, A())
                        expectThat(subject.callSuspend(Body("value"), "10", requestContext))
                            .isEqualTo(Body("value with id 10"))
                    }
                    it("supports declaring the id before the body") {
                        class A : RestService {
                            fun update(id: Int, body: Body) = Body(body.field + " with id " + id)
                        }

                        val subject = RestFunction(A::update, A())
                        expectThat(subject.callSuspend(Body("value"), "10", requestContext))
                            .isEqualTo(Body("value with id 10"))
                    }
                }
                describe("the request context") {
                    it("is also passed when declared as parameter") {
                        @Suppress("UNUSED_PARAMETER")
                        class A : RestService {
                            fun update(id: Int, body: Body, requestContext: RequestContext) =
                                Body(body.field + " with id " + id)
                        }

                        val subject = RestFunction(A::update, A())
                        expectThat(subject.callSuspend(Body("value"), "10", requestContext))
                            .isEqualTo(Body("value with id 10"))
                    }
                }
                describe("error handling") {
                    it("fails fast when body type cannot be determined") {
                        data class OtherPossibleBodyType(val s: String)

                        @Suppress("UNUSED_PARAMETER")
                        class A : RestService {
                            fun update(
                                id: Int,
                                body: Body,
                                otherPossibleBodyType: OtherPossibleBodyType,
                                requestContext: RequestContext
                            ) = Body(body.field + " with id " + id)
                        }
                        expectThrows<RuntimeException> { RestFunction(A::update, A()) }
                            .message
                            .isNotNull()
                            .and {
                                isEqualTo(
                                    "Rest method A#update(Int, Body, OtherPossibleBodyType, RequestContext)" +
                                        " has multiple possible body types: [Body, OtherPossibleBodyType]")
                            }
                    }
                }
            }
        }
}
