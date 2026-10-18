# Requires: JDK, Android SDK (platform and build-tools), zip, pandoc.

BUILD = build
ANDROID_HOME ?= /opt/android-sdk
PLATFORM = $(ANDROID_HOME)/platforms/android-36/android.jar
BUILD_TOOLS = $(ANDROID_HOME)/build-tools/36.0.0
DEBUG_KEYSTORE = $(HOME)/.android/debug.keystore
JUNIT = /usr/share/java/junit.jar:/usr/share/java/hamcrest/hamcrest.jar

apk: $(BUILD)/au-hasard-debug.apk

install: $(BUILD)/au-hasard-debug.apk
	adb install -r $<

$(BUILD)/res.zip: $(shell find res -type f)
	mkdir -p $(BUILD)
	$(BUILD_TOOLS)/aapt2 compile --dir res -o $@

# Also generates R.java.
$(BUILD)/resources.apk: $(BUILD)/res.zip AndroidManifest.xml assets/about.html $(shell find assets -type f)
	rm -rf $(BUILD)/gen
	$(BUILD_TOOLS)/aapt2 link -o $@ -I $(PLATFORM) --manifest AndroidManifest.xml \
		-A assets --java $(BUILD)/gen $(BUILD)/res.zip

# Compiling against android.jar as the boot class path (which requires -source 8)
# rejects JDK APIs that Android doesn't have. The lambda stubs let javac compile lambdas,
# which d8 then desugars.
$(BUILD)/classes.dex: $(BUILD)/resources.apk $(shell find src -name '*.java')
	rm -rf $(BUILD)/classes
	mkdir -p $(BUILD)/classes
	javac -source 8 -target 8 -Xlint:-options -bootclasspath $(PLATFORM):$(BUILD_TOOLS)/core-lambda-stubs.jar \
		-d $(BUILD)/classes $$(find src $(BUILD)/gen -name '*.java')
	$(BUILD_TOOLS)/d8 --release --min-api 29 --lib $(PLATFORM) --output $(BUILD) \
		$$(find $(BUILD)/classes -name '*.class')

$(BUILD)/au-hasard-debug.apk: $(BUILD)/resources.apk $(BUILD)/classes.dex $(DEBUG_KEYSTORE)
	cp $(BUILD)/resources.apk $(BUILD)/unaligned.apk
	cd $(BUILD) && zip -q unaligned.apk classes.dex
	$(BUILD_TOOLS)/zipalign -f -p 4 $(BUILD)/unaligned.apk $(BUILD)/aligned.apk
	$(BUILD_TOOLS)/apksigner sign --ks $(DEBUG_KEYSTORE) --ks-pass pass:android \
		--out $@ $(BUILD)/aligned.apk

# The standard Android debug key, shared with other Android tooling.
$(DEBUG_KEYSTORE):
	mkdir -p $(dir $@)
	keytool -genkeypair -keystore $@ -storepass android -alias androiddebugkey \
		-keyalg RSA -validity 10000 -dname "CN=Android Debug,O=Android,C=US"

# The About page, also usable as a standalone web page.
about: assets/about.html

assets/about.html: doc/about.md doc/style.html
	mkdir -p assets
	pandoc $< --standalone --include-in-header=doc/style.html -o $@

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

clean:
	rm -rf $(BUILD)

.PHONY: apk install about test stats compile-tests clean
