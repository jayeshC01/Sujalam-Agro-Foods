GRADLE := ./gradlew
JACOCO_HTML := build/reports/jacoco/test/html/index.html

.PHONY: build test format start stop logs

build:
	$(GRADLE) spotlessApply
	$(GRADLE) build
	@echo "JaCoCo coverage report: file://$(CURDIR)/$(JACOCO_HTML)"

test:
	$(GRADLE) test jacocoTestReport jacocoTestCoverageVerification
	@echo "JaCoCo coverage report: file://$(CURDIR)/$(JACOCO_HTML)"

format:
	$(GRADLE) spotlessApply

start:
	docker compose up -d --build

stop:
	docker compose down

logs:
	docker compose logs -f
