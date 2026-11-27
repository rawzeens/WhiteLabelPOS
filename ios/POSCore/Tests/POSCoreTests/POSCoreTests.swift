import CoreChecks
import XCTest

final class POSCoreTests: XCTestCase {
    func testSharedRules() {
        let result = CoreChecks.runAll()
        XCTAssertEqual(result.failures, [])
        XCTAssertGreaterThan(result.passed, 100)
    }
}
