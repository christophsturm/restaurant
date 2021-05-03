package restaurant.internal

import failfast.describe

object RestFunctionTest {
    val context = describe(RestFunction::class) {
        describe("parameter type") {
            pending("detects parameter type for methods with only one parameter") {
                class A() {

                }
            }
        }
    }
}
