import igraph as ig
import numpy as np
import matplotlib.pyplot as plt

SOURCE = "./1MRichieJson/"
TYPE = "mentions"
NUM = 75

# get graph
graph = ig.Graph.Read_Ncol(SOURCE + TYPE + ".txt").as_undirected().simplify()

# delete users w/ not tweets in our dataset
user_ids = None
to_remove = []
with open(SOURCE + "users_w_tweets.txt") as f:
	user_ids = set(map(lambda x: str(long(x)), f.readlines()))
f.close()
for v in graph.vs:
	if v['name'] not in user_ids:
		to_remove.append(v['name'])
graph.delete_vertices(to_remove)

# get largest connected component
lcc = graph.clusters().giant()
"""
# get clusterings
vcs = []
for i in range(NUM):
	#vcs.append(lcc.community_label_propagation())
	tmp = lcc.community_leading_eigenvector(clusters=2)
	vcs.append(lcc.community_label_propagation(initial=tmp.membership))

# compute adj rand idx
rands = []
for i in range(NUM):
	for j in range(i+1,NUM):
		rands.append(ig.compare_communities(vcs[i], vcs[j], method="adjusted_rand"))

rands = np.array(rands)
with open(SOURCE + TYPE + "_rm_lelp_analysis.txt", "w") as f:
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
plt.savefig(SOURCE + TYPE + "_rm_lelp_randhist.png")
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

with open(SOURCE + TYPE + "_rm_lelp_clust.txt", "w") as f:
	nnodes = len(vc.membership)
	for i in range(nnodes):
		f.write(lcc.vs[i]["name"] + " " + str(vc.membership[i]) + "\n")
f.close()	
"""






