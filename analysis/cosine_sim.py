from collections import Counter
import math
import cPickle as pickle

def generate_data():
	user_id_to_hashtag_counter = {}
	with open('hashtags.txt', 'rb') as f:
	    for line in f:
	        split_line = line.split()
	        if len(split_line) < 2:
	            continue 
	        user_id = int(split_line[0])
	        hashtag = split_line[1]
	        if user_id not in user_id_to_hashtag_counter:
	            user_id_to_hashtag_counter[user_id] = Counter() 
	        user_id_to_hashtag_counter[user_id][hashtag] += 1 
	return user_id_to_hashtag_counter

def load_data():
	with open('user_id_to_hashtag_counter.pickle', 'rb') as f:
		return pickle.load(f)

def cosine_sim(user_1, user_2):
    return counter_cosine_similarity(user_id_to_hashtag_counter[user_1], 
                                     user_id_to_hashtag_counter[user_2])

def counter_cosine_similarity(c1, c2):
    terms = set(c1).union(c2)
    dotprod = sum(c1.get(k, 0) * c2.get(k, 0) for k in terms)
    magA = math.sqrt(sum(c1.get(k, 0)**2 for k in terms))
    magB = math.sqrt(sum(c2.get(k, 0)**2 for k in terms))
    return dotprod / (magA * magB)

user_id_to_hashtag_counter = {}
try: 
	user_id_to_hashtag_counter = load_data() 
except Error:
	user_id_to_hashtag_counter = generate_data() 
print "LOADED"
while True:
	user_id_1 = raw_input("Enter id of first user: ")
	if not user_id_1.isdigit() or int(user_id_1) not in user_id_to_hashtag_counter:
		print "NOT A VALID ID"
	else: 
		user_id_2 = raw_input("Enter id of second user: ")
		if not user_id_2.isdigit() or int(user_id_2) not in user_id_to_hashtag_counter:
			print "NOT A VALID ID"
		else:
			print cosine_sim(int(user_id_1), int(user_id_2))