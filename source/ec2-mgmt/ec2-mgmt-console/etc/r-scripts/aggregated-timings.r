#!/usr/bin/Rscript

# R (http://www.r-project.org/) script for generating
# aggregated graphs over a number of tests for  
# client data age in the test network.

args <- commandArgs(TRUE)
if (length(args) != 3)
{
	cat("Usage: aggregated-timings.r <dataset.csv> <output-file> <runCount>\n")
	q(status = 1)
}
now <- Sys.time()
dst <- read.csv(args[1], sep = ";")

pdf(args[2], title = sprintf("Aggregated Client Data Age for %s Runs - %s", args[3], format(now, "%Y-%m-%d %H:%M:%S")))
matplot(dst[2], type = 'l', lty = 1, col = "blue", xlab = "", ylab = "age in ms.", ylim=c(0,6000))
matlines(dst[1], type = 'l', lty = 2, col = "blue")

abline(h = 1000, col = "red", lty = 2)
abline(h = 5000, col = "red", lty = 2)

title(sprintf("Aggregated Client Data Age for %s Runs - %s", args[3], format(now, "%Y-%m-%d %H:%M:%S")), sub = "Max age and mean in ms. of data for every test run")
opar <- par(no.readonly = TRUE)
par(usr = c(0,1,0,1))

legend(0.75,0.99, c("max", "mean"), lty = c(1,2), col = c("blue", "blue"), bg = "white", text.width = 0.126)
par(opar)

q(status = 0)
