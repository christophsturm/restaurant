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
        describe("parameter type") {
            data class Body(val field: String)
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
    }
}
