BUILD = build
JUNIT = /usr/share/java/junit.jar:/usr/share/java/hamcrest/hamcrest.jar

test: compile-tests
	java -cp $(BUILD)/test:$(JUNIT) info.ro_che.auhasard.RunTests

# Requires R with ggplot2. Plots even if a check fails, then reports the failure.
stats: compile-tests
	java -cp $(BUILD)/test:$(JUNIT) info.ro_che.auhasard.Stats; \
	status=$$?; Rscript test/plot-stats.R && exit $$status

compile-tests:
	rm -rf $(BUILD)/test
	mkdir -p $(BUILD)/test
	javac -d $(BUILD)/test -cp $(JUNIT) -sourcepath src $$(find test -name '*.java')

.PHONY: test stats compile-tests
