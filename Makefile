# Requires: JDK, Android SDK (platform and build-tools), zip, pandoc.
# The aab target also requires bundletool (https://github.com/google/bundletool).

BUILD = build
ANDROID_HOME ?= /opt/android-sdk
PLATFORM = $(ANDROID_HOME)/platforms/android-36/android.jar
BUILD_TOOLS = $(ANDROID_HOME)/build-tools/36.0.0
DEBUG_KEYSTORE = $(HOME)/.android/debug.keystore
BUNDLETOOL ?= /opt/bundletool/bundletool.jar
RELEASE_ALIAS ?= au-hasard
UPLOAD_ALIAS ?= upload
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
	javac -source 8 -target 8 -Xlint:-options -encoding UTF-8 -bootclasspath $(PLATFORM):$(BUILD_TOOLS)/core-lambda-stubs.jar \
		-d $(BUILD)/classes $$(find src $(BUILD)/gen -name '*.java' | sort)
	$(BUILD_TOOLS)/d8 --release --min-api 29 --lib $(PLATFORM) --output $(BUILD) \
		$$(find $(BUILD)/classes -name '*.class' | sort)

# The unsigned APK is reproducible: classes.dex goes in with a fixed timestamp, since zip would
# otherwise record the time of the build.
$(BUILD)/aligned.apk: $(BUILD)/resources.apk $(BUILD)/classes.dex
	cp $(BUILD)/resources.apk $(BUILD)/unaligned.apk
	rm -rf $(BUILD)/dex
	mkdir $(BUILD)/dex
	cp $(BUILD)/classes.dex $(BUILD)/dex/
	TZ=UTC touch -d '2000-01-01 00:00:00' $(BUILD)/dex/classes.dex
	cd $(BUILD)/dex && TZ=UTC zip -q -X ../unaligned.apk classes.dex
	$(BUILD_TOOLS)/zipalign -f -p 4 $(BUILD)/unaligned.apk $@

$(BUILD)/au-hasard-debug.apk: $(BUILD)/aligned.apk $(DEBUG_KEYSTORE)
	$(BUILD_TOOLS)/apksigner sign --ks $(DEBUG_KEYSTORE) --ks-pass pass:android \
		--out $@ $<

# Release builds need RELEASE_KEYSTORE / UPLOAD_KEYSTORE and prompt for the
# keystore password. See SIGNING.md.
release-apk: $(BUILD)/au-hasard.apk

# Signed with the app signing key, for F-Droid and direct downloads.
$(BUILD)/au-hasard.apk: $(BUILD)/aligned.apk
	$(if $(RELEASE_KEYSTORE),,$(error RELEASE_KEYSTORE is not set))
	$(BUILD_TOOLS)/apksigner sign --ks $(RELEASE_KEYSTORE) --ks-key-alias $(RELEASE_ALIAS) \
		--out $@ $<

aab: $(BUILD)/au-hasard.aab

# The base module is the proto-format resource APK rearranged into the layout
# bundletool expects.
$(BUILD)/base.zip: $(BUILD)/res.zip AndroidManifest.xml assets/about.html $(shell find assets -type f) $(BUILD)/classes.dex
	rm -rf $(BUILD)/base $@
	$(BUILD_TOOLS)/aapt2 link --proto-format -o $(BUILD)/proto.apk -I $(PLATFORM) \
		--manifest AndroidManifest.xml -A assets $(BUILD)/res.zip
	unzip -q $(BUILD)/proto.apk -d $(BUILD)/base
	mkdir $(BUILD)/base/manifest $(BUILD)/base/dex
	mv $(BUILD)/base/AndroidManifest.xml $(BUILD)/base/manifest/
	cp $(BUILD)/classes.dex $(BUILD)/base/dex/
	cd $(BUILD)/base && zip -q -X -r ../base.zip .

# Signed with the Play upload key.
$(BUILD)/au-hasard.aab: $(BUILD)/base.zip
	$(if $(UPLOAD_KEYSTORE),,$(error UPLOAD_KEYSTORE is not set))
	rm -f $@
	java -jar $(BUNDLETOOL) build-bundle --modules=$< --output=$@
	jarsigner -keystore $(UPLOAD_KEYSTORE) -storetype PKCS12 -digestalg SHA-256 \
		-sigalg SHA256withRSA $@ $(UPLOAD_ALIAS)

# The standard Android debug key, shared with other Android tooling.
$(DEBUG_KEYSTORE):
	mkdir -p $(dir $@)
	keytool -genkeypair -keystore $@ -storepass android -alias androiddebugkey \
		-keyalg RSA -validity 10000 -dname "CN=Android Debug,O=Android,C=US"

# The About page, also usable as a standalone web page.
about: assets/about.html

assets/about.html: doc/about.md doc/style.html doc/mathcal.lua doc/formula.svg
	mkdir -p assets
	pandoc $< --standalone --lua-filter=doc/mathcal.lua --include-in-header=doc/style.html -o $@

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

.PHONY: apk install release-apk aab about test stats compile-tests clean
