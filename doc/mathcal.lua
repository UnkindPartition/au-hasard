-- Pandoc renders \mathcal{U} as the character 𝒰, which the page's font lacks, and spaces the
-- rest differently from TeX. This replaces the formula 𝒰(a, b) with the TeX-rendered one from
-- the logo build, inline so that it takes the text's color.

local formula
do
  local f = assert(io.open("doc/formula.svg"))
  formula = f:read("a"):gsub("%s+$", "")
  f:close()
end

function Math(el)
  if el.text:gsub("%s", "") == "\\mathcal{U}(a,b)" then
    return pandoc.RawInline("html", formula)
  end
end
