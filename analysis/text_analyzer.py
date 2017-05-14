import numpy as np
from nltk.corpus import stopwords
from sklearn.feature_extraction.text import TfidfVectorizer

STOP_WORDS = map(lambda w: w.encode("ascii"), stopwords.words("english"))

# get map from userid -> clusterid
def get_user2cluster(filepath):
	with open(filepath, "r") as f:
		content = f.readlines()
	f.close()

	user2cluster = {}
	for line in content:
		tmp = line.split(" ")
		userid = long(tmp[0])
		clusterid = int(tmp[1])
		user2cluster[userid] = clusterid
	return user2cluster

# get map from clusterid -> array of member id's
def get_cluster2users(filepath):
	with open(filepath, "r") as f:
		content = f.readlines()
	f.close()

	cluster2users = {}
	for line in content:
		tmp = line.split(" ")
		userid = long(tmp[0])
		clusterid = int(tmp[1])

		if clusterid in cluster2users:
			cluster2users[clusterid].append(userid)
		else:
			cluster2users[clusterid] = [userid]
	return cluster2users

# get map from userid -> string of hashtags used (string separated)
def get_user2hashtags(filepath):
	with open(filepath, "r") as f:
		content = f.readlines()
	f.close()

	user2hashtags = {}
	for line in content:
		tmp = line.split(" ")
		userid = long(tmp[0])
		hashtag = tmp[1]

		if userid in user2hashtags:
			user2hashtags[userid] = user2hashtags[userid] + " " + hashtag
		else:
			user2hashtags[userid] = hashtag
	return user2hashtags

# randomly sample from given cluster
def select_from_cluster(cluster2users, npeople, clusterid):
	cluster_members = np.array(cluster2users[clusterid])
	return np.random.choice(cluster_members, size=npeople, replace=False)

# sample npeople from 2 largest clusters
# computer similarity of their associated documents
def get_similarity_hashtags(cluster2users, user2hashtags, npeople):
	cluster_biggest = -1
	cluster_2biggest = -1
	cluster_biggest_id = None
	cluster_2biggest_id = None

	for clusterid in cluster2users.keys():
		users = cluster2users[clusterid]
		if len(users) >= cluster_biggest:
			cluster_2biggest = cluster_biggest
			cluster_biggest = len(users) 
			cluster_2biggest_id = cluster_biggest_id
			cluster_biggest_id = clusterid
		elif len(users) > cluster_2biggest:
			cluster_2biggest = len(users)
			cluster_2biggest_id = clusterid

	userset1 = select_from_cluster(cluster2users, npeople, cluster_biggest_id)
	userset2 = select_from_cluster(cluster2users, npeople, cluster_2biggest_id)

	docs = []
	for user in userset1:
		if user in user2hashtags:
			docs.append(user2hashtags[user])
		else:
			docs.append("")
	for user in userset2:
		if user in user2hashtags:
			docs.append(user2hashtags[user])
		else:
			docs.append("")

	vect = TfidfVectorizer(min_df=1)
	tfidf = vect.fit_transform(docs)
	sim = (tfidf * tfidf.T).A

	within1 = np.array(sim[:npeople, :npeople])
	within2 = np.array(sim[npeople:, npeople:])
	between = np.array(sim[:npeople, npeople:])

	within1_avg = (np.sum(within1) - np.trace(within1)) / (npeople * npeople - npeople)
	within2_avg = (np.sum(within2) - np.trace(within1)) / (npeople * npeople - npeople) 
	between_avg = np.mean(between)

	return sim, within1_avg, within2_avg, between_avg



