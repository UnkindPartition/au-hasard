# Plots the p-values written by Stats.java. Under a correct generator, each check's
# p-values are uniform on [0, 1]: a flat histogram and an ECDF along the diagonal.

library(ggplot2)

pvalues <- read.csv("build/stats/pvalues.csv")
ks <- read.csv("build/stats/ks.csv")

pooled_name <- "all checks pooled"
checks <- unique(pvalues$check)
data <- rbind(pvalues, transform(pvalues, check = pooled_name))
labels <- setNames(sprintf("%s\nKS p = %.3f", ks$check, ks$p), ks$check)
data$panel <- factor(labels[data$check], levels = labels[c(checks, pooled_name)])
seeds <- length(unique(pvalues$seed[pvalues$check == checks[1]]))

pdf("build/stats/pvalues.pdf", width = 12, height = 10)

print(
  ggplot(data, aes(p)) +
    geom_histogram(
      aes(y = after_stat(density)),
      breaks = seq(0, 1, 0.05), fill = "grey60", colour = "white"
    ) +
    geom_hline(yintercept = 1, colour = "red") +
    facet_wrap(~panel) +
    scale_x_continuous(breaks = c(0, 0.5, 1)) +
    labs(
      title = sprintf("Distribution of p-values, %d seeds per check", seeds),
      subtitle = "Red line: expected density under a correct generator",
      x = "p-value", y = "density"
    ) +
    theme_bw()
)

print(
  ggplot(data, aes(p)) +
    geom_abline(colour = "red") +
    stat_ecdf() +
    facet_wrap(~panel) +
    coord_equal() +
    scale_x_continuous(breaks = c(0, 0.5, 1)) +
    scale_y_continuous(breaks = c(0, 0.5, 1)) +
    labs(
      title = sprintf("Empirical CDF of p-values, %d seeds per check", seeds),
      subtitle = "Red line: expected CDF under a correct generator",
      x = "p-value", y = "fraction of seeds with a smaller p-value"
    ) +
    theme_bw()
)

invisible(dev.off())
