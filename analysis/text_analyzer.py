import numpy as np
import json
from nltk.corpus import stopwords
from sklearn.feature_extraction.text import TfidfVectorizer

STOP_WORDS = stopwords.words("english")

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

# get map from userid -> text of all tweets
def get_user2tweets(filepath):
	user2tweets = {}
	with open(filepath, 'rb') as f: 
	    i = 0 
	    for line in f:
	        tweet = json.loads(line)
	        user_id = tweet['user']['id']
	        text = tweet['text'].lower().replace("\n", "")
	        
	        if user_id not in user2tweets:
	            user2tweets[user_id] = ""
	        user2tweets[user_id] = user2tweets[user_id] + " " + text
	f.close()
	return user2tweets

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
def get_similarity(cluster2users, user2text, npeople, stop=True):
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
		if user in user2text:
			docs.append(user2text[user])
		else:
			docs.append("")
	for user in userset2:
		if user in user2text:
			docs.append(user2text[user])
		else:
			docs.append("")

	vect = TfidfVectorizer(min_df=1,stop_words=STOP_WORDS if stop else None)
	tfidf = vect.fit_transform(docs)
	sim = (tfidf * tfidf.T).A

	within1 = np.array(sim[:npeople, :npeople])
	within2 = np.array(sim[npeople:, npeople:])
	between = np.array(sim[:npeople, npeople:])

	within1_avg = (np.sum(within1) - np.trace(within1)) / (npeople * npeople - npeople)
	within2_avg = (np.sum(within2) - np.trace(within1)) / (npeople * npeople - npeople) 
	between_avg = np.mean(between)

	return sim, within1_avg, within2_avg, between_avg

# perform analysis

# setup
DATA = "/media/rhenwood39/OS/6850_proj/efilter/resultsFirst1M.json"
SOURCE = "./1MRichieJson/"
cluster_filepaths = ["retweet_rm_clust.txt", "mentions_rm_clust.txt", "replies_rm_clust.txt", "retweet_rm_lelp_clust.txt", "mentions_rm_lelp_clust.txt", "replies_rm_lelp_clust.txt"]
output = ["retweet_labprop", "mentions_labprop", "replies_labprop", "retweet_modlabprop", "mentions_modlabprop", "replies_modlabprop"]

# get tweets
user2tweets = get_user2tweets(DATA)
# wi1s = []
# wi2s = []
# bws = []

# cluster sim
# for file in cluster_filepaths:
# 	cluster2users = get_cluster2users(SOURCE + file)
# 	sim,wi1,wi2,bw = get_similarity(cluster2users, user2docs, 2000)
# 	wi1s.append(wi1)
# 	wi2s.append(wi2)
# 	bws.append(bw)

# with open(SOURCE + "cosine_sim_analysis_hashtag.csv", "w") as f:
# 	f.write("source, within1, within2, between\n")
# 	for i in range(len(cluster_filepaths)):
# 		f.write(cluster_filepaths[i] + ", " + str(wi1s[i]) + ", " + str(wi2s[i]) + ", " + str(bws[i]) + "\n")
# f.close()

# get tweets for each cluster
for i in range(len(cluster_filepaths)):
	file = cluster_filepaths[i]
	out = output[i]
	
	# get clusters
	cluster2users = get_cluster2users(SOURCE + file)

	# get 2 biggest clusters
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

	cluster1 = np.random.choice(np.array(cluster2users[cluster_biggest_id]), size=20, replace=False)
	cluster2 = np.random.choice(np.array(cluster2users[cluster_2biggest_id]), size=20, replace=False)

	with open("./ClusterTweets/" + out + "1.txt", "w") as f:
		for i in range(len(cluster1)):
			user = cluster1[i]
			f.write(u"TWEET " + str(i) + u"\n")
			f.write((user2tweets[user] + u"\n").encode("utf8"))
			f.write(u"\n***********************************************************\n")
	f.close()

	with open("./ClusterTweets/" + out + "2.txt", "w") as f:
		for i in range(len(cluster2)):
			user = cluster2[i]
			f.write(u"TWEET " + str(i) + u"\n")
			f.write((user2tweets[user] + u"\n").encode("utf8"))
			f.write(u"\n***********************************************************\n")
	f.close()



