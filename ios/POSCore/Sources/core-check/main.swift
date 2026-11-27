import CoreChecks
import Foundation

// Runs the shared checks without Xcode:  swift run core-check
let result = CoreChecks.runAll()
for f in result.failures {
    print("FAIL  \(f)")
}
print("\(result.passed) checks passed, \(result.failures.count) failed")
exit(result.failures.isEmpty ? 0 : 1)
