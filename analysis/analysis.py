import igraph as ig
import numpy as np
import matplotlib.pyplot as plt

SOURCE = "./1MRichieJson/"
TYPE = "mentions"
NUM = 75

# get largest connected comp
graph = ig.Graph.Read_Ncol(SOURCE + TYPE + ".txt").as_undirected().simplify()
lcc = graph.clusters().giant()

# get clusterings
vcs = []
for i in range(NUM):
	vcs.append(lcc.community_label_propagation())

# compute adj rand idx
rands = []
for i in range(NUM):
	for j in range(i+1,NUM):
		rands.append(ig.compare_communities(vcs[i], vcs[j], method="adjusted_rand"))

rands = np.array(rands)
with open(SOURCE + TYPE + "_analysis.txt", "w") as f:
	f.write("ADJ RAND INDEX\n")
	f.write("min: " + str(np.min(rands)) + "\n")
	#f.write("25 perc: " + str(np.percentile(rands, 25)) + "\n")
	f.write("median: " + str(np.median(rands)) + "\n")
	#f.write("75 perc: " + str(np.percentile(rands, 75)) + "\n")
	f.write("max: " + str(np.max(rands)) + "\n")
	f.write("mean: " + str(np.mean(rands)) + "\n")
	f.write("\n")

	f.write("5 TOP CLUSTER SIZES\n")
	for i in range(NUM):
		clusts = np.sort(np.array(vcs[i].sizes()))
		start = -min(5, len(clusts))
		topclusts = clusts[start:]
		f.write(str(topclusts))
		f.write("\n")
	f.write("\n")

	f.write("EDGE CROSSINGS\n")
	for i in range(NUM):
		crossing = np.array(vcs[i].crossing())
		f.write("cross: " + str(sum(crossing)) + " & within: " + str(sum(~crossing)) + "\n")
	f.write("\n")

	f.write("MODULARITIES\n")
	for i in range(NUM):
		f.write(str(vcs[i].modularity) + "\n")
f.close()

plt.hist(rands, bins="auto")
plt.title("Retweet Label Prop Adj Rand Idx")
plt.savefig(SOURCE + TYPE + "_randhist.png")
plt.clf()

# best_mod = -np.inf
# best_vc = None
# for vc in vcs:
# 	if vc.modularity > best_mod:
# 		best_vc = vc
# 		best_mod = vc.modularity
mods = []
for vc in vcs:
	mods.append(vc.modularity)
mods = np.array(mods)
idx = mods.argsort()[len(mods)/2]
vc = vcs[idx]

with open(SOURCE + TYPE + "_clust.txt", "w") as f:
	nnodes = len(vc.membership)
	for i in range(nnodes):
		f.write(lcc.vs[i]["name"] + " " + str(vc.membership[i]) + "\n")
f.close()	







