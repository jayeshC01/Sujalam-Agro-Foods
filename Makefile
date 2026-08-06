GRADLE := ./gradlew
JACOCO_HTML := build/reports/jacoco/test/html/index.html

.PHONY: build test format

build:
	$(GRADLE) spotlessApply
	$(GRADLE) build
	@echo "JaCoCo coverage report: file://$(CURDIR)/$(JACOCO_HTML)"

test:
	$(GRADLE) test jacocoTestReport jacocoTestCoverageVerification
	@echo "JaCoCo coverage report: file://$(CURDIR)/$(JACOCO_HTML)"

format:
	$(GRADLE) spotlessApply
