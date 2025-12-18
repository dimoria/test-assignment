
SRC_MAIN := src/main/java
SRC_TEST := src/test/java

OUT_DIR := out
OUT_MAIN := $(OUT_DIR)/main
OUT_TEST := $(OUT_DIR)/test

LIB_DIR := lib


JUNIT_PLATFORM := $(LIB_DIR)/junit-platform-console-standalone-6.0.1.jar
JUNIT4 := $(LIB_DIR)/junit-4.13.2.jar
HAMCREST := $(LIB_DIR)/hamcrest-core-1.3.jar

# Windows classpath separator is ;
CLASSPATH := $(OUT_MAIN);$(JUNIT_PLATFORM);$(JUNIT4);$(HAMCREST)

.PHONY: help clean deps compile compile-main compile-test test all


help:
	@echo ""
	@echo "Available targets:"
	@echo "  make help    - Show this help message"
	@echo "  make deps    - Download dependencies"
	@echo "  make compile - Compile sources"
	@echo "  make test    - Run tests"
	@echo "  make clean   - Clean build output"
	@echo ""


all: compile


deps: $(JUNIT_PLATFORM) $(JUNIT4) $(HAMCREST)

$(JUNIT_PLATFORM):
	mkdir -p $(LIB_DIR)
	wget -q https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.0.1/junit-platform-console-standalone-6.0.1.jar -O $(JUNIT_PLATFORM)
	@echo "Downloaded JUnit Platform Console"

$(JUNIT4):
	mkdir -p $(LIB_DIR)
	wget -q https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar -O $(JUNIT4)
	@echo "Downloaded JUnit 4.13.2"

$(HAMCREST):
	mkdir -p $(LIB_DIR)
	wget -q https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar -O $(HAMCREST)
	@echo "Downloaded Hamcrest 1.3"


compile-main: deps
	@echo "== Compiling main sources =="
	mkdir -p $(OUT_MAIN)
	javac -cp "$(CLASSPATH)" -d $(OUT_MAIN) $$(find $(SRC_MAIN) -name "*.java")

compile-test: compile-main
	@echo "== Compiling test sources =="
	mkdir -p $(OUT_TEST)
	javac -cp "$(CLASSPATH)" -d $(OUT_TEST) $$(find $(SRC_TEST) -name "*.java")

compile: compile-test


test: compile
	@echo "== Running JUnit tests =="
	java -jar $(JUNIT_PLATFORM) execute \
		--classpath "$(OUT_MAIN);$(OUT_TEST)" \
		--scan-classpath


clean:
	rm -rf $(OUT_DIR)

