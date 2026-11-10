PROJECT_DIR := user-config-service
GRADLE      := cd $(PROJECT_DIR) && ./gradlew

.DEFAULT_GOAL := help
.PHONY: help build build-notest test run jar clean deps

help: ## List targets
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  %-14s %s\n", $$1, $$2}'

build: ## Compile, test and package
	$(GRADLE) build

build-notest: ## Compile and package, skipping tests
	$(GRADLE) build -x test

test: ## Run tests; narrow with TEST=Class or TEST=Class.method
	$(GRADLE) test $(if $(TEST),--tests '$(TEST)')

run: ## Run the app (bootRun)
	$(GRADLE) bootRun

jar: ## Build the executable Spring Boot jar only
	$(GRADLE) bootJar

clean: ## Delete build outputs
	$(GRADLE) clean

deps: ## Show runtime dependency tree
	$(GRADLE) dependencies --configuration runtimeClasspath
