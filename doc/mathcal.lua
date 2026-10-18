-- Pandoc renders \mathcal{U} as the character 𝒰, which the page's font lacks. This replaces it
-- with the TeX-rendered glyph from the logo build, inline so that it takes the text's color.

local glyph
do
  local f = assert(io.open("doc/calligraphic-u.svg"))
  glyph = f:read("a")
  f:close()
end

function Math(el)
  if not el.text:find("\\mathcal{U}", 1, true) then
    return nil
  end
  local result = {}
  local rest = el.text
  while true do
    local i, j = rest:find("\\mathcal{U}", 1, true)
    local piece = i and rest:sub(1, i - 1) or rest
    if piece:match("%S") then
      table.insert(result, pandoc.Math(el.mathtype, piece))
    end
    if not i then
      break
    end
    table.insert(result, pandoc.RawInline("html", glyph))
    rest = rest:sub(j + 1)
  end
  return result
end
