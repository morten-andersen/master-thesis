#!/usr/bin/Rscript

# R (http://www.r-project.org/) script for generating
# aggregated graphs over a number of tests and over a number of prototypes for  
# client data age in the test network.

args <- commandArgs(TRUE)
if (length(args) < 3)
{
	cat("Usage: aggregated-timings-combined.r <output-file> <runCount> <prototype>+\n")
	q(status = 1)
}
now <- Sys.time()
dst <- read.csv(sprintf("%s-aggregated.csv", args[3]), sep = ";")

pdf(args[1], title = sprintf("Aggregated Client Data Age for %s Runs - %s", args[2], format(now, "%Y-%m-%d %H:%M:%S")))
matplot(dst[2], type = 'l', lty = 1, col = 3, xlab = "", ylab = "age in ms.", ylim=c(0,6000))
matlines(dst[1], type = 'l', lty = 2, col = 3)

abline(h = 1000, col = "red", lty = 2)
abline(h = 5000, col = "red", lty = 2)

title(sprintf("Aggregated Client Data Age for %s Runs - %s", args[2], format(now, "%Y-%m-%d %H:%M:%S")), sub = "Max age and mean in ms. of data for every test run")
opar <- par(no.readonly = TRUE)
par(usr = c(0,1,0,1))

legend_txt <- c()
for (i in 3:length(args))
{
	legend_txt[i - 2] <- args[i]
}

legend(0.75,0.99, legend_txt, fill = 3:length(args), bg = "white", text.width = 0.157)

legend(0.75,0.865, c("max", "mean"), lty = c(1,2), bg = "white", text.width = 0.126)
par(opar)

if (length(args) > 3)
{
	for (i in 4:length(args))
	{
		dst <- read.csv(sprintf("%s-aggregated.csv", args[i]), sep = ";")
		matlines(dst[2], type = 'l', lty = 1, col = i)
		matlines(dst[1], type = 'l', lty = 2, col = i)
	}
}

q(status = 0)
