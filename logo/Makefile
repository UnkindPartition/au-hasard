# Requires: TeX Live (latex, amsfonts), dvisvgm, python3, rsvg-convert (librsvg).

BUILD = build
OUT = out

all: $(OUT)/icon.svg $(OUT)/icon-512.png $(OUT)/preview.html

$(BUILD)/formula.dvi: formula.tex
	mkdir -p $(BUILD)
	latex -interaction=nonstopmode -halt-on-error -output-directory=$(BUILD) $<

$(BUILD)/formula.svg: $(BUILD)/formula.dvi
	dvisvgm --no-fonts=1 --exact-bbox -o $@ $<

$(OUT)/icon.svg $(OUT)/ic_launcher_foreground.xml $(OUT)/ic_launcher_background.xml $(OUT)/preview.html &: $(BUILD)/formula.svg build.py
	python3 build.py

# Google Play store listing icon
$(OUT)/icon-512.png: $(OUT)/icon.svg
	rsvg-convert -w 512 -h 512 -o $@ $<

clean:
	rm -rf $(BUILD)

.PHONY: all clean
