from collections import Counter 
import math

if __name__ == "__main__":
	user_id_to_hashtag_counter = {}

	def cosine_sim(user_1, user_2):
		if user_1 not in user_id_to_hashtag_counter or user_2 not in user_id_to_hashtag_counter:
			return 0.0

		return counter_cosine_similarity(user_id_to_hashtag_counter[user_1], 
			user_id_to_hashtag_counter[user_2])

	def counter_cosine_similarity(c1, c2):
	    terms = set(c1).union(c2)
	    dotprod = sum(c1.get(k, 0) * c2.get(k, 0) for k in terms)
	    magA = math.sqrt(sum(c1.get(k, 0)**2 for k in terms))
	    magB = math.sqrt(sum(c2.get(k, 0)**2 for k in terms))
	    return dotprod / (magA * magB)

	with open('../1MRichieJson/hashtags.txt', 'rb') as f:
		for line in f:
			split_line = line.split()
			if len(split_line) < 2:
				continue 
			user_id = int(split_line[0])
			hashtag = split_line[1]
			if user_id not in user_id_to_hashtag_counter:
				user_id_to_hashtag_counter[user_id] = Counter() 
			user_id_to_hashtag_counter[user_id][hashtag] += 1 

	cluster_id_to_user_ids = {}
	with open('../1MRichieJson/mentions_clust.txt', 'rb') as f:
		for line in f:
			split_line = line.split()
			user_id = int(split_line[0])
			cluster_id = int(split_line[1])
			if cluster_id not in cluster_id_to_user_ids:
				cluster_id_to_user_ids[cluster_id] = []
			cluster_id_to_user_ids[cluster_id].append(user_id)

	print "DATA LOADED"

	#for k in cluster_id_to_user_ids:
	#	print k, len(cluster_id_to_user_ids[k])



	cluster_0 = cluster_id_to_user_ids[0]
	cluster_1 = cluster_id_to_user_ids[5]

	print "START "
	cos_score = 0.0
	l = 0.0

	for i in range(len(cluster_0)):
		for j in range(i+1, len(cluster_0)):
			l += 1
			cos_score += cosine_sim(cluster_0[i], cluster_0[j])
			if l % 1000000 == 0:
				print l 
	
	#for user_0 in cluster_0:
	#	for user_1 in cluster_1:
	#		l += 1 
	#		cos_score += cosine_sim(user_0, user_1)

	#		if l % 1000000 == 0:
	#			print l 
	print "DONE"
	print cos_score / l 
	avg = cos_score / l 
	with open("results.txt", "w") as f:
		f.write(str(avg))
