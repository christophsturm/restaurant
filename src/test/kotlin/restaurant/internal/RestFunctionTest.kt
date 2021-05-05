package restaurant.internal

import failfast.describe
import restaurant.RestService
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object RestFunctionTest {
    val context = describe(RestFunction::class) {
        describe("parameter type") {
            it("detects parameter type for methods with a single parameter") {
                data class Body(val field: String)
                class A() : RestService {
                    fun create(body: Body) = Unit
                }
                expectThat(RestFunction(A::create, A())).get { parameterType }.isEqualTo(Body::class.java)
            }
        }
    }
}
