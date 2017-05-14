import json
import cPickle as pickle
user_id_to_tweets = {}
with open('../richie.json', 'rb') as f: 
    i = 0 
    for line in f:
        tweet = json.loads(line)
        user_id = tweet['user']['id']
        text = tweet['text']
        
        if user_id not in user_id_to_tweets:
            user_id_to_tweets[user_id] = []
        user_id_to_tweets[user_id].append(text)

with open('user_id_to_tweets.pickle', 'wb') as f:
    pickle.dump(user_id_to_tweets, f)
    
print "DONE"

# below is the code to load
#with open('user_id_to_tweets.pickle', 'rb') as f:
#	user_id_to_tweets =  pickle.load(f)