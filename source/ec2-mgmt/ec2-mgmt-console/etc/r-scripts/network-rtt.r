#!/usr/bin/Rscript

# R (http://www.r-project.org/) script for generating
# a "distance matrix" of the RTT between all nodes
# in a test network.
#
# Uses the receipt described here:
# http://stackoverflow.com/questions/3081066/what-techniques-exists-in-r-to-visualize-a-distance-matrix


args <- commandArgs(TRUE)
if (length(args) != 2)
{
	cat("Usage: network-rtt.r <dataset.csv> <output-file>\n")
	q(status = 1)  
}
now <- Sys.time()
fdst <- read.csv(args[1])
dim <- ncol(fdst)
dst <- data.matrix(fdst[2:dim])
dim <- ncol(dst)
pdf(args[2], title = sprintf("Network Round Trip Time - %s", format(now, "%Y-%m-%d %H:%M:%S")))
image(1:dim, 1:dim, dst, axes = FALSE, main = sprintf("Network Round Trip Time - %s", format(now, "%Y-%m-%d %H:%M:%S")),
		sub = "Average network round trip time in ms. between nodes", xlab = "", ylab = "")
axis(1, 1:dim, fdst[1:dim,1], cex.axis = 0.5, las = 2)
axis(2, 1:dim, fdst[1:dim,1], cex.axis = 0.5, las = 1)

for (i in 1:dim)
{
	for (j in 1:dim)
	{
		txt <- sprintf("%0.3f", dst[i,j])
		text(i, j, txt, cex=0.5)
	}
}

q(status = 0)
