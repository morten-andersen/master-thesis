#!/usr/bin/Rscript

# R (http://www.r-project.org/) script for generating graphs of 
# client data age in the test network.

args <- commandArgs(TRUE)
if (length(args) < 2)
{
	cat("Usage: client-timings.r <serverCount> <client-ip>+\n")
	cat("\tA file with name '<client-ip>.csv' must exist\n")
	q(status = 1)
}
now <- Sys.time()
dst <- read.csv(sprintf("%s.csv", args[2]), sep = ";")
pdf(sprintf("client-timings-%s.pdf", format(now, "%Y-%m-%d_%H-%M-%S")), title = sprintf("Client Data Age - %s", format(now, "%Y-%m-%d %H:%M:%S")))
matplot(dst[3], type = 'l', lty = 1, col = 2, xlab = "", ylab = "age in ms.", ylim=c(0,6000))
matlines(dst[2], type = 'l', lty = 3, col = 2)

abline(h = 1000, col = "red", lty = 2)
abline(h = 5000, col = "red", lty = 2)

title(sprintf("Client Data Age - %s", format(now, "%Y-%m-%d %H:%M:%S")), sub = "Age in ms. of data on one client - sampling every second")
opar <- par(no.readonly = TRUE)
par(usr = c(0,1,0,1))

legend_txt <- c()
for (i in 2:length(args))
{
	legend_txt[i - 1] <- args[i]
}

legend(0.69,0.99, legend_txt, fill = 2:length(args), bg = "white", text.width = 0.217)

legend(0.69,0.75, c("max", "mean"), lty = c(1,3), bg = "white", text.width = 0.186)
par(opar)

if (length(args) > 2)
{
	for (i in 3:length(args))
	{
		dst <- read.csv(sprintf("%s.csv", args[i]), sep = ";")
		matlines(dst[3], type = 'l', lty = 1, col = i)
		matlines(dst[2], type = 'l', lty = 3, col = i)
	}
}

# draw server uptime lines at the bottom
for (i in 1:args[1])
{
	srvUp <- read.csv(sprintf("server-%s.csv", (i - 1)), sep = ";")
	for (j in 1:nrow(srvUp))
	{
		segments(srvUp[j, 1], -50 * i, srvUp[j, 2], -50 * i, col= 'blue', lwd = 2)
	}
}

q(status = 0)
