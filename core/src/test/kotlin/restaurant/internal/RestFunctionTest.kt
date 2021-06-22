package restaurant.internal

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import restaurant.RestService
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class RestFunctionTest {
    val context = describe(RestFunction::class) {
        data class Body(val field: String)
        describe("parameter type") {
            it("detects parameter type for methods with only the body parameter") {
                class A : RestService {
                    @Suppress("UNUSED_PARAMETER")
                    fun create(body: Body) = Unit
                }
                expectThat(RestFunction(A::create, A())).get { payloadType }.isEqualTo(Body::class.java)
            }
            describe("detects parameter type for methods with body parameter and id") {
                it("when body is first") {
                    @Suppress("UNUSED_PARAMETER")
                    class A : RestService {
                        fun update(body: Body, id: String) = Unit
                    }
                    expectThat(RestFunction(A::update, A())).get { payloadType }.isEqualTo(Body::class.java)
                }
                it("when id is first") {
                    @Suppress("UNUSED_PARAMETER")
                    class A : RestService {
                        fun update(id: String, body: Body) = Unit
                    }
                    expectThat(RestFunction(A::update, A())).get { payloadType }.isEqualTo(Body::class.java)
                }
            }
        }
        describe("invoking the method") {
            it("can invoke a method with only an id parameter") {
                class A : RestService {
                    fun get(id: String) = id
                }

                val subject = RestFunction(A::get, A())
                expectThat(subject.callSuspend(null, "id")).isEqualTo("id")
            }
            describe("id parameter type") {
                it("can be Int") {
                    class A : RestService {
                        fun get(id: Int) = id
                    }

                    expectThat(RestFunction(A::get, A()).callSuspend(null, "123")).isEqualTo(123)
                }
                it("can be Long") {
                    class A : RestService {
                        fun get(id: Long) = id
                    }

                    expectThat(RestFunction(A::get, A()).callSuspend(null, "123")).isEqualTo(123L)
                }
            }
            it("can invoke a method with only a body parameter") {
                class A : RestService {
                    fun create(body: Body) = body
                }

                val subject = RestFunction(A::create, A())
                expectThat(subject.callSuspend(Body("value"), null)).isEqualTo(Body("value"))
            }
            describe("invoking a method with body and a id parameter") {
                it("supports declaring the body before the id") {
                    class A : RestService {
                        fun update(body: Body, id: Int) = Body(body.field + " with id " + id)
                    }

                    val subject = RestFunction(A::update, A())
                    expectThat(subject.callSuspend(Body("value"), "10")).isEqualTo(Body("value with id 10"))
                }
                it("supports declaring the id before the body") {
                    class A : RestService {
                        fun update(id: Int, body: Body) = Body(body.field + " with id " + id)
                    }

                    val subject = RestFunction(A::update, A())
                    expectThat(subject.callSuspend(Body("value"), "10")).isEqualTo(Body("value with id 10"))
                }
            }
        }
    }
}
