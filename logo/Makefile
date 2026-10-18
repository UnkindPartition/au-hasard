# Requires: TeX Live (latex), dvisvgm, python3, rsvg-convert (librsvg).

BUILD = build
OUT = out

all: $(OUT)/icon.svg $(OUT)/icon-512.png $(OUT)/preview.html ../assets/formula.svg

$(BUILD)/%.dvi: %.tex
	mkdir -p $(BUILD)
	latex -interaction=nonstopmode -halt-on-error -output-directory=$(BUILD) $<

$(BUILD)/%.svg: $(BUILD)/%.dvi
	dvisvgm --no-fonts=1 --exact-bbox -o $@ $<

$(BUILD)/digits.svg: $(BUILD)/digits.dvi
	dvisvgm --no-fonts=1 --bbox=papersize --precision=2 -o $@ $<

$(OUT)/icon.svg $(OUT)/preview.html ../res/drawable/ic_launcher_background.xml \
		../res/drawable/ic_launcher_foreground.xml ../res/drawable/ic_launcher_monochrome.xml \
		../res/drawable/formula.xml ../res/drawable/calligraphic_u.xml &: \
		$(BUILD)/formula.svg $(BUILD)/u.svg $(BUILD)/digits.svg build.py
	python3 build.py

# For the About page
../assets/formula.svg: $(BUILD)/formula.svg
	mkdir -p ../assets
	cp $< $@

# Google Play store listing icon
$(OUT)/icon-512.png: $(OUT)/icon.svg
	rsvg-convert -w 512 -h 512 -o $@ $<

clean:
	rm -rf $(BUILD)

.PHONY: all clean
