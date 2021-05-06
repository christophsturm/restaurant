package restaurant.internal

import failfast.FailFast
import failfast.describe
import restaurant.RestService
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}
object RestFunctionTest {
    val context = describe(RestFunction::class) {
        data class Body(val field: String)
        describe("parameter type") {
            it("detects parameter type for methods with only the body parameter") {
                class A : RestService {
                    fun create(body: Body) = Unit
                }
                expectThat(RestFunction(A::create, A())).get { parameterType }.isEqualTo(Body::class.java)
            }
            describe("detects parameter type for methods with body parameter and id") {
                it("when body is first") {
                    class A : RestService {
                        fun update(body: Body, id: String) = Unit
                    }
                    expectThat(RestFunction(A::update, A())).get { parameterType }.isEqualTo(Body::class.java)
                }
                it("when id is first") {
                    class A : RestService {
                        fun update(id: String, body: Body) = Unit
                    }
                    expectThat(RestFunction(A::update, A())).get { parameterType }.isEqualTo(Body::class.java)
                }
            }
        }
        describe("invoking the method") {
            it("can invoke a method with only an string id parameter") {
                class A : RestService {
                    fun get(id: String) = id
                }

                val subject = RestFunction(A::get, A())
                expectThat(subject.callSuspend(null, "id")).isEqualTo("id")
            }
            it("can invoke a method with only an int id parameter") {
                class A : RestService {
                    fun get(id: Int) = id
                }

                val subject = RestFunction(A::get, A())
                expectThat(subject.callSuspend(null, "123")).isEqualTo(123)
            }
            it("can invoke a method with only a body parameter") {
                class A : RestService {
                    fun create(body: Body) = body
                }

                val subject = RestFunction(A::create, A())
                expectThat(subject.callSuspend(Body("value"), null)).isEqualTo(Body("value"))
            }
            it("can invoke a method with a body and a id parameter") {
                class A : RestService {
                    fun update(body: Body, id: Int) = Body(body.field + " with id " + id)
                }

                val subject = RestFunction(A::update, A())
                expectThat(subject.callSuspend(Body("value"), "10")).isEqualTo(Body("value with id 10"))


            }
        }
    }
}
